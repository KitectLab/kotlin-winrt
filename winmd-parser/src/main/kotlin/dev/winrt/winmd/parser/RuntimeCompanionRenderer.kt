package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ParameterSpec
import dev.winrt.winmd.plugin.WinMdActivationKind
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType

internal class RuntimeCompanionRenderer(
    private val typeRegistry: TypeRegistry,
    private val typeNameMapper: TypeNameMapper,
    private val delegateLambdaPlanResolver: DelegateLambdaPlanResolver,
    private val eventSlotDelegatePlanResolver: EventSlotDelegatePlanResolver,
    private val winRtSignatureMapper: WinRtSignatureMapper,
    private val asyncMethodRuleRegistry: AsyncMethodRuleRegistry,
    private val winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    private val kotlinCollectionProjectionMapper: KotlinCollectionProjectionMapper,
) {
    fun render(type: WinMdType): TypeSpec {
        val typeClass = ClassName(type.namespace.lowercase(), type.name)
        val builder = TypeSpec.companionObjectBuilder()
            .addSuperinterface(PoetSymbols.winRtRuntimeClassMetadataClass)
            .addProperty(overrideStringProperty("qualifiedName", "${type.namespace}.${type.name}"))
            .addProperty(
                PropertySpec.builder("classId", PoetSymbols.runtimeClassIdClass)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%T(%S, %S)", PoetSymbols.runtimeClassIdClass, type.namespace, type.name)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("defaultInterfaceName", String::class.asTypeName().copy(nullable = true))
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer(type.defaultInterface?.let { CodeBlock.of("%S", it) } ?: CodeBlock.of("null"))
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("activationKind", PoetSymbols.winRtActivationKindClass)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%T.%L", PoetSymbols.winRtActivationKindClass, activationKindLiteral(type.activationKind))
                    .build(),
            )
            .addFunction(
                FunSpec.builder(type.activationFunctionName)
                    .returns(typeClass)
                    .addStatement("return %T.activate(this, ::%L)", PoetSymbols.winRtRuntimeClass, type.name)
                    .build(),
            )
        renderStatics(type).forEach { member ->
            when (member) {
                is PropertySpec -> builder.addProperty(member)
                is FunSpec -> builder.addFunction(member)
            }
        }
        return builder.build()
    }

    private fun renderStatics(type: WinMdType): List<Any> {
        val staticsType = typeRegistry.findRuntimeClassStaticsType(type.name, type.namespace) ?: return emptyList()
        val staticsClass = ClassName(staticsType.namespace.lowercase(), staticsType.name)
        val members = mutableListOf<Any>()

        members += PropertySpec.builder("statics", staticsClass)
            .addModifiers(KModifier.PRIVATE)
            .delegate(CodeBlock.of("lazy { %T.projectActivationFactory(this, %T, ::%T) }", PoetSymbols.winRtRuntimeClass, staticsClass, staticsClass))
            .build()

        val declaredPropertyNames = staticsType.properties.map { it.name }.toSet()
        staticsType.properties.forEach { property ->
            members += renderForwardingProperty(property, type.namespace)
        }
        staticsType.methods
            .filterNot { method -> isGetterLike(method) || isSetterLike(method) }
            .forEach { method ->
                members += renderForwardingMethod(method, type.namespace)
                renderForwardingAsyncAwaitMethod(method, type.namespace)?.let(members::add)
                renderForwardingLambdaOverload(method, type.namespace)?.let(members::add)
            }
        renderEventSlotMembers(type, staticsType).let { eventMembers ->
            members.addAll(eventMembers.properties)
        }
        synthesizeGetterProperties(staticsType.methods, declaredPropertyNames, type.namespace).forEach(members::add)
        return members
    }

    fun renderStaticEventSlotTypes(type: WinMdType): List<TypeSpec> {
        val staticsType = typeRegistry.findRuntimeClassStaticsType(type.name, type.namespace) ?: return emptyList()
        return renderEventSlotMembers(type, staticsType).types
    }

    private fun renderEventSlotMembers(type: WinMdType, staticsType: WinMdType): RuntimeCompanionEventMembers {
        val methodsByName = staticsType.methods.associateBy { it.name }
        val plans = staticsType.methods.mapNotNull { addMethod ->
            if (!addMethod.name.startsWith("add_") || addMethod.parameters.size != 1 || addMethod.returnType != "EventRegistrationToken") {
                return@mapNotNull null
            }
            val eventName = addMethod.name.removePrefix("add_")
            val removeMethod = methodsByName["remove_$eventName"] ?: return@mapNotNull null
            if (removeMethod.parameters.size != 1 || removeMethod.parameters.single().type != "EventRegistrationToken" || removeMethod.returnType != "Unit") {
                return@mapNotNull null
            }
            val delegateTypeName = addMethod.parameters.single().type
            val delegatePlan = eventSlotDelegatePlanResolver.resolve(delegateTypeName, type.namespace)
                ?: return@mapNotNull null
            RuntimeCompanionEventSlotPlan(
                propertyName = eventName.replaceFirstChar(Char::lowercase),
                typeName = "${eventName}StaticEvent",
                nestedType = ClassName(type.namespace.lowercase(), type.name, "${eventName}StaticEvent"),
                delegateType = delegatePlan.delegateType,
                lambdaType = delegatePlan.lambdaType,
                delegateGuid = delegatePlan.delegateGuid,
                lambdaArgumentKindsLiteral = delegatePlan.argumentKindsLiteral(),
                lambdaCallbackInvocation = delegatePlan.callbackInvocation("handler"),
                staticsClass = ClassName(staticsType.namespace.lowercase(), staticsType.name),
                addVtableIndex = addMethod.vtableIndex ?: return@mapNotNull null,
                removeVtableIndex = removeMethod.vtableIndex ?: return@mapNotNull null,
            )
        }
        return RuntimeCompanionEventMembers(
            properties = plans.flatMap { plan ->
                listOf(
                    PropertySpec.builder("${plan.propertyName}EventSlot", plan.nestedType)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("%L { statics }", plan.typeName)
                        .build(),
                    PropertySpec.builder("${plan.propertyName}Event", plan.nestedType)
                        .getter(
                            FunSpec.getterBuilder()
                                .addStatement("return %N", "${plan.propertyName}EventSlot")
                                .build(),
                        )
                        .build(),
                )
            },
            types = plans.map { plan ->
                TypeSpec.classBuilder(plan.typeName)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("staticsProvider", LambdaTypeName.get(returnType = plan.staticsClass))
                            .build(),
                    )
                    .addProperty(
                        PropertySpec.builder("staticsProvider", LambdaTypeName.get(returnType = plan.staticsClass))
                            .addModifiers(KModifier.PRIVATE)
                            .initializer("staticsProvider")
                            .build(),
                    )
                    .addProperty(
                        PropertySpec.builder(
                            "delegateHandles",
                            PoetSymbols.mutableMapClass.parameterizedBy(
                                PoetSymbols.eventRegistrationTokenClass,
                                PoetSymbols.winRtDelegateHandleClass,
                            ),
                        )
                            .addModifiers(KModifier.PRIVATE)
                            .initializer("mutableMapOf()")
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("subscribe")
                            .addParameter("handler", plan.delegateType)
                            .returns(PoetSymbols.eventRegistrationTokenClass)
                            .addStatement(
                                "return %T(%T.invokeInt64MethodWithObjectArg(staticsProvider().pointer, %L, handler.pointer).getOrThrow())",
                                PoetSymbols.eventRegistrationTokenClass,
                                PoetSymbols.platformComInteropClass,
                                plan.addVtableIndex,
                            )
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("subscribe")
                            .addParameter("handler", plan.lambdaType)
                            .returns(PoetSymbols.eventRegistrationTokenClass)
                            .addStatement(
                                "val delegateHandle = %T.createUnitDelegate(%M(%S), %L) { args -> %L }",
                                PoetSymbols.winRtDelegateBridgeClass,
                                PoetSymbols.guidOfMember,
                                plan.delegateGuid,
                                plan.lambdaArgumentKindsLiteral,
                                plan.lambdaCallbackInvocation,
                            )
                            .addStatement("val token = subscribe(%T(delegateHandle.pointer))", plan.delegateType)
                            .addStatement("delegateHandles[token] = delegateHandle")
                            .addStatement("return token")
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("plusAssign")
                            .addModifiers(KModifier.OPERATOR)
                            .addParameter("handler", plan.delegateType)
                            .addStatement("subscribe(handler)")
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("unsubscribe")
                            .addParameter("token", PoetSymbols.eventRegistrationTokenClass)
                            .addStatement("delegateHandles.remove(token)?.close()")
                            .addStatement("%T.invokeUnitMethodWithInt64Arg(staticsProvider().pointer, %L, token.value).getOrThrow()", PoetSymbols.platformComInteropClass, plan.removeVtableIndex)
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("minusAssign")
                            .addModifiers(KModifier.OPERATOR)
                            .addParameter("token", PoetSymbols.eventRegistrationTokenClass)
                            .addStatement("unsubscribe(token)")
                            .build(),
                    )
                    .build()
            },
        )
    }

    private fun renderForwardingProperty(property: WinMdProperty, currentNamespace: String): PropertySpec {
        val typeName = exposedTypeName(property.type, currentNamespace)
        val propertyName = property.name.replaceFirstChar { it.lowercase() }
        return PropertySpec.builder(propertyName, typeName)
            .mutable(property.mutable)
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("return statics.%L", propertyName)
                    .build(),
            )
            .apply {
                if (property.mutable) {
                    setter(
                        FunSpec.setterBuilder()
                            .addParameter("value", typeName)
                            .addStatement("statics.%L = value", propertyName)
                            .build(),
                    )
                }
            }
            .build()
    }

    private fun renderForwardingMethod(method: WinMdMethod, currentNamespace: String): FunSpec {
        val builder = FunSpec.builder(method.name.replaceFirstChar { it.lowercase() })
        method.parameters.forEach { parameter ->
            builder.addParameter(parameter.name, exposedTypeName(parameter.type, currentNamespace))
        }
        if (method.returnType != "Unit") {
            builder.returns(exposedTypeName(method.returnType, currentNamespace))
            builder.addStatement(
                "return statics.%L(%L)",
                companionForwardTargetName(method),
                method.parameters.joinToString(", ") { it.name },
            )
        } else {
            builder.addStatement(
                "statics.%L(%L)",
                companionForwardTargetName(method),
                method.parameters.joinToString(", ") { it.name },
            )
        }
        return builder.build()
    }

    private fun renderForwardingAsyncAwaitMethod(method: WinMdMethod, currentNamespace: String): FunSpec? {
        val asyncPlan = asyncMethodRuleRegistry.plan(method, currentNamespace) ?: return null
        val baseFunctionName = method.name.replaceFirstChar { it.lowercase() }
        val parameterSpecs = method.parameters.map { parameter ->
            ParameterSpec.builder(parameter.name, exposedTypeName(parameter.type, currentNamespace)).build()
        }
        val invocation = buildString {
            append("statics.")
            append(baseFunctionName)
            append('(')
            append(parameterSpecs.joinToString(", ") { it.name })
            append(')')
        }
        val builder = FunSpec.builder("${baseFunctionName}Await")
            .addModifiers(KModifier.SUSPEND)
            .returns(asyncPlan.awaitReturnType)
            .addParameters(parameterSpecs)
        asyncPlan.progressLambdaType?.let { progressLambdaType ->
            builder.addParameter(
                ParameterSpec.builder("onProgress", progressLambdaType)
                    .defaultValue("{ _ -> }")
                    .build(),
            )
            builder.addStatement("return %L.%M(onProgress = onProgress)", invocation, PoetSymbols.awaitMember)
        } ?: builder.addStatement("return %L.%M()", invocation, PoetSymbols.awaitMember)
        return builder.build()
    }

    private fun renderForwardingLambdaOverload(method: WinMdMethod, currentNamespace: String): FunSpec? {
        if (method.parameters.size != 1) return null
        val delegateTypeName = method.parameters.single().type
        val delegateType = typeRegistry.findType(delegateTypeName, currentNamespace) ?: return null
        if (delegateType.kind != dev.winrt.winmd.plugin.WinMdTypeKind.Delegate) return null
        val invoke = delegateType.methods.singleOrNull { it.name == "Invoke" } ?: return null
        val delegateClass = typeNameMapper.mapTypeName(method.parameters.single().type, currentNamespace)
        val plan = delegateLambdaPlanResolver.resolve(
            invokeMethod = invoke,
            currentNamespace = currentNamespace,
            genericParameters = emptySet(),
            supportsObjectType = { typeName -> supportsInterfaceObjectType(typeName, currentNamespace) },
        ) as? DelegateLambdaPlan.PlannedBridge ?: return null

        val builder = FunSpec.builder(method.name.replaceFirstChar { it.lowercase() })
            .returns(PoetSymbols.winRtDelegateHandleClass)
            .addParameter(method.parameters.single().name, plan.lambdaType)
        val argumentKindsLiteral = if (plan.bridge.argumentKinds.isEmpty()) {
            "emptyList()"
        } else {
            plan.bridge.argumentKinds.joinToString(
                prefix = "listOf(",
                postfix = ")",
            ) { kind -> "${PoetSymbols.winRtDelegateValueKindClass.canonicalName}.${kind.name}" }
        }
        val callbackInvocation = buildDelegateCallbackInvocation(plan)
        builder.addStatement(
            "val delegateHandle = %T.%L(%T.iid, %L) { args -> %L }",
            PoetSymbols.winRtDelegateBridgeClass,
            plan.bridge.factoryMethod,
            delegateClass,
            argumentKindsLiteral,
            callbackInvocation,
        )
        builder.addStatement("statics.%L(%T(delegateHandle.pointer))", companionForwardTargetName(method), delegateClass)
        builder.addStatement("return delegateHandle")
        return builder.build()
    }

    private fun buildDelegateCallbackInvocation(plan: DelegateLambdaPlan.PlannedBridge): CodeBlock {
        if (plan.lambdaType.parameters.isEmpty()) {
            return CodeBlock.of("%N()", "callback")
        }
        val builder = CodeBlock.builder().add("%N(", "callback")
        plan.lambdaType.parameters.indices.forEachIndexed { position, index ->
            if (position > 0) {
                builder.add(", ")
            }
            val lambdaType = plan.lambdaType.parameters[index]
            when (plan.bridge.argumentKinds[index]) {
                DelegateArgumentKind.OBJECT -> builder.add("%L(args[%L] as %T)", lambdaType, index, PoetSymbols.comPtrClass)
                else -> builder.add("args[%L] as %L", index, lambdaType)
            }
        }
        return builder.add(")").build()
    }

    private fun synthesizeGetterProperties(
        methods: List<WinMdMethod>,
        declaredPropertyNames: Set<String>,
        currentNamespace: String,
    ): List<PropertySpec> {
        return methods.asSequence()
            .filter { it.name.startsWith("get_") && it.parameters.isEmpty() }
            .map { method ->
                val propertyName = method.name.removePrefix("get_")
                propertyName to method
            }
            .filter { (propertyName, _) -> propertyName !in declaredPropertyNames }
            .map { (propertyName, method) ->
                PropertySpec.builder(propertyName.replaceFirstChar { it.lowercase() }, exposedTypeName(method.returnType, currentNamespace))
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return statics.%L()", method.name.replaceFirstChar { it.lowercase() })
                            .build(),
                    )
                    .build()
            }
            .toList()
    }

    private fun isGetterLike(method: WinMdMethod): Boolean {
        return method.name.startsWith("get_") && method.parameters.isEmpty()
    }

    private fun isSetterLike(method: WinMdMethod): Boolean {
        return method.name.startsWith("put_") && method.parameters.size == 1
    }

    private fun companionForwardTargetName(method: WinMdMethod): String {
        return method.name.replaceFirstChar { it.lowercase() }
    }

    private fun exposedTypeName(typeName: String, currentNamespace: String): TypeName {
        val runtimeType = typeRegistry.findType(typeName, currentNamespace)
        if (runtimeType != null) {
            kotlinCollectionProjectionMapper.runtimeClassProjection(runtimeType)?.let { return it.superinterface }
            kotlinCollectionProjectionMapper.runtimeClassInterfaceProjection(
                type = runtimeType,
                typeNameMapper = typeNameMapper,
                winRtSignatureMapper = winRtSignatureMapper,
                winRtProjectionTypeMapper = winRtProjectionTypeMapper,
            )?.let { return it.superinterface }
            kotlinCollectionProjectionMapper.runtimeClassIterableProjection(
                type = runtimeType,
                typeNameMapper = typeNameMapper,
                winRtSignatureMapper = winRtSignatureMapper,
                winRtProjectionTypeMapper = winRtProjectionTypeMapper,
            )?.let { return it.superinterface }
        }
        return typeNameMapper.mapTypeName(typeName, currentNamespace)
    }

    private fun supportsInterfaceObjectType(typeName: String, currentNamespace: String): Boolean {
        return typeName == "Object" || typeRegistry.findType(typeName, currentNamespace)?.kind == dev.winrt.winmd.plugin.WinMdTypeKind.Interface
    }

    private fun activationKindLiteral(kind: WinMdActivationKind): String {
        return when (kind) {
            WinMdActivationKind.Factory -> "Factory"
        }
    }

    private data class RuntimeCompanionEventMembers(
        val properties: List<PropertySpec>,
        val types: List<TypeSpec>,
    )

    private data class RuntimeCompanionEventSlotPlan(
        val propertyName: String,
        val typeName: String,
        val nestedType: ClassName,
        val delegateType: TypeName,
        val lambdaType: LambdaTypeName,
        val delegateGuid: String,
        val lambdaArgumentKindsLiteral: String,
        val lambdaCallbackInvocation: CodeBlock,
        val staticsClass: ClassName,
        val addVtableIndex: Int,
        val removeVtableIndex: Int,
    )
}
