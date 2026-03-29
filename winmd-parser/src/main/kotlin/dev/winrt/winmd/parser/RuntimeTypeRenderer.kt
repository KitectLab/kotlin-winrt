package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdActivationKind
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdType

internal class RuntimeTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
    private val typeRegistry: TypeRegistry,
    private val delegateLambdaPlanResolver: DelegateLambdaPlanResolver,
    private val eventSlotDelegatePlanResolver: EventSlotDelegatePlanResolver,
    private val runtimePropertyRenderer: RuntimePropertyRenderer,
    private val runtimeMethodRenderer: RuntimeMethodRenderer,
    private val runtimeCompanionRenderer: RuntimeCompanionRenderer,
    private val winRtSignatureMapper: WinRtSignatureMapper,
    private val winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    private val kotlinCollectionProjectionMapper: KotlinCollectionProjectionMapper = KotlinCollectionProjectionMapper(),
) {
    fun render(type: WinMdType): TypeSpec {
        require(type.kind == dev.winrt.winmd.plugin.WinMdTypeKind.RuntimeClass) {
            "Unsupported type kind for runtime renderer: ${type.kind}"
        }
        return renderRuntimeClass(type)
    }

    private fun renderRuntimeClass(type: WinMdType): TypeSpec {
        val superclass = type.baseClass
            ?.takeUnless { it == "System.Object" }
            ?.let { typeNameMapper.mapTypeName(it, type.namespace) }
            ?: PoetSymbols.inspectableClass
        val builder = TypeSpec.classBuilder(type.name)
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(pointerConstructor())
            .superclass(superclass)
            .addSuperclassConstructorParameter("pointer")

        kotlinCollectionProjectionMapper.runtimeClassProjection(type)?.let { projection ->
            projection.delegateFactory?.let { delegateFactory ->
                builder.addSuperinterface(projection.superinterface, delegateFactory)
            } ?: builder.addSuperinterface(projection.superinterface)
            projection.extraProperties.forEach(builder::addProperty)
            projection.extraFunctions.forEach(builder::addFunction)
            builder.addProperty(kotlinCollectionProjectionMapper.buildWinRtSizeProperty(projection.winRtSizeSlot))
        }
        kotlinCollectionProjectionMapper.runtimeClassInterfaceProjection(
            type = type,
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { projection ->
            projection.delegateFactory?.let { delegateFactory ->
                builder.addSuperinterface(projection.superinterface, delegateFactory)
            } ?: builder.addSuperinterface(projection.superinterface)
            projection.extraProperties.forEach(builder::addProperty)
            projection.extraFunctions.forEach(builder::addFunction)
            builder.addProperty(kotlinCollectionProjectionMapper.buildWinRtSizeProperty(projection.winRtSizeSlot))
        }
        kotlinCollectionProjectionMapper.runtimeClassIterableProjection(
            type = type,
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { projection ->
            builder.addSuperinterface(projection.superinterface, projection.delegateFactory)
        }
        type.baseInterfaces.mapNotNull { baseInterface ->
            collectionSuperinterface(baseInterface, type.namespace, emptySet())
        }.forEach(builder::addSuperinterface)

        if (type.activationKind == WinMdActivationKind.Factory) {
            builder.addFunction(
                FunSpec.constructorBuilder()
                    .callThisConstructor(CodeBlock.of("Companion.%L().pointer", type.activationFunctionName))
                    .build(),
            )
        }

        type.properties.filter(runtimePropertyRenderer::canRenderRuntimeProperty).forEach { property ->
            builder.addProperty(runtimePropertyRenderer.renderBackingProperty(property, type.namespace))
            builder.addProperty(runtimePropertyRenderer.renderRuntimeProperty(property, type.namespace))
        }
        type.methods.filter { runtimeMethodRenderer.canRenderRuntimeMethod(it, type.namespace) }
            .flatMap { runtimeMethodRenderer.renderRuntimeMethods(it, type.namespace) }
            .forEach(builder::addFunction)
        type.methods
            .mapNotNull { runtimeMethodRenderer.renderRuntimeLambdaOverload(it, type.namespace) }
            .forEach(builder::addFunction)
        renderEventSlotMembers(type.methods, type.namespace).let { projection ->
            projection.properties.forEach(builder::addProperty)
            projection.types.forEach(builder::addType)
        }
        renderDefaultInterfaceMembers(type).let { projection ->
            projection.properties.forEach(builder::addProperty)
            projection.methods.forEach(builder::addFunction)
            projection.types.forEach(builder::addType)
        }
        renderBaseInterfaceMembers(type).let { projection ->
            projection.properties.forEach(builder::addProperty)
            projection.methods.forEach(builder::addFunction)
            projection.types.forEach(builder::addType)
        }
        runtimeCompanionRenderer.renderStaticEventSlotTypes(type).forEach(builder::addType)
        builder.addType(runtimeCompanionRenderer.render(type))
        return builder.build()
    }

    private fun renderDefaultInterfaceMembers(type: WinMdType): RuntimeProjectionMembers {
        val defaultInterface = typeRegistry.findDefaultInterfaceType(type.name, type.namespace)
            ?: return RuntimeProjectionMembers(emptyList(), emptyList())
        return collectProjectedInterfaceMembers(defaultInterface, type.namespace)
    }

    private fun renderBaseInterfaceMembers(type: WinMdType): RuntimeProjectionMembers {
        val methods = mutableListOf<FunSpec>()
        val properties = mutableListOf<PropertySpec>()
        val types = mutableListOf<TypeSpec>()
        typeRegistry.findImplementedInterfaceTypes(type.name, type.namespace)
            .forEach { interfaceType ->
                val members = collectProjectedInterfaceMembers(interfaceType, type.namespace)
                methods += members.methods
                properties += members.properties
                types += members.types
            }
        return RuntimeProjectionMembers(methods, properties, types)
    }

    private fun collectProjectedInterfaceMembers(interfaceType: WinMdType, currentNamespace: String): RuntimeProjectionMembers {
        val methods = interfaceType.methods
            .filter { runtimeMethodRenderer.canRenderRuntimeMethod(it, currentNamespace) }
            .flatMap { runtimeMethodRenderer.renderRuntimeMethods(it, currentNamespace) }
        val properties = interfaceType.properties
            .filter(runtimePropertyRenderer::canRenderRuntimeProperty)
            .flatMap { property ->
                listOfNotNull(
                    runtimePropertyRenderer.renderBackingProperty(property, currentNamespace),
                    runtimePropertyRenderer.renderRuntimeProperty(property, currentNamespace),
                )
            }
        val eventMembers = renderEventSlotMembers(interfaceType.methods, currentNamespace)
        return RuntimeProjectionMembers(methods, properties + eventMembers.properties, eventMembers.types)
    }

    private fun renderEventSlotMembers(methods: List<WinMdMethod>, currentNamespace: String): RuntimeEventMembers {
        val methodsByName = methods.associateBy { it.name }
        val plans = methods.mapNotNull { addMethod ->
            if (!addMethod.name.startsWith("add_") || addMethod.parameters.size != 1 || addMethod.returnType != "EventRegistrationToken") {
                return@mapNotNull null
            }
            val eventName = addMethod.name.removePrefix("add_")
            val removeMethod = methodsByName["remove_$eventName"] ?: return@mapNotNull null
            if (removeMethod.parameters.size != 1 || removeMethod.parameters.single().type != "EventRegistrationToken" || removeMethod.returnType != "Unit") {
                return@mapNotNull null
            }
            val delegateTypeName = addMethod.parameters.single().type
            val delegatePlan = eventSlotDelegatePlanResolver.resolve(delegateTypeName, currentNamespace)
                ?: return@mapNotNull null
            RuntimeEventSlotPlan(
                propertyName = eventName.replaceFirstChar(Char::lowercase),
                typeName = "${eventName}Event",
                delegateType = delegatePlan.delegateType,
                lambdaType = delegatePlan.lambdaType,
                delegateGuid = delegatePlan.delegateGuid,
                lambdaArgumentKindsLiteral = delegatePlan.argumentKindsLiteral(),
                lambdaCallbackInvocation = delegatePlan.callbackInvocation("handler"),
                addVtableIndex = addMethod.vtableIndex ?: return@mapNotNull null,
                removeVtableIndex = removeMethod.vtableIndex ?: return@mapNotNull null,
            )
        }
        return RuntimeEventMembers(
            properties = plans.flatMap { plan ->
                listOf(
                    PropertySpec.builder("${plan.propertyName}EventSlot", ClassName("", plan.typeName))
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("%L()", plan.typeName)
                        .build(),
                    PropertySpec.builder("${plan.propertyName}Event", ClassName("", plan.typeName))
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
                    .addModifiers(KModifier.INNER)
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
                                "return %T(%L)",
                                PoetSymbols.eventRegistrationTokenClass,
                                AbiCallCatalog.int64MethodWithObject(plan.addVtableIndex, "handler.pointer"),
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
                            .addStatement("%T.invokeUnitMethodWithInt64Arg(pointer, %L, token.value).getOrThrow()", PoetSymbols.platformComInteropClass, plan.removeVtableIndex)
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

    private fun collectionSuperinterface(
        baseInterface: String,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): com.squareup.kotlinpoet.TypeName? {
        val mapped = typeNameMapper.mapTypeName(baseInterface, currentNamespace, genericParameters)
        return if (mapped.toString().startsWith("kotlin.collections.")) mapped else null
    }

    private data class RuntimeProjectionMembers(
        val methods: List<FunSpec>,
        val properties: List<PropertySpec>,
        val types: List<TypeSpec> = emptyList(),
    )

    private data class RuntimeEventMembers(
        val properties: List<PropertySpec>,
        val types: List<TypeSpec>,
    )

    private data class RuntimeEventSlotPlan(
        val propertyName: String,
        val typeName: String,
        val delegateType: com.squareup.kotlinpoet.TypeName,
        val lambdaType: LambdaTypeName,
        val delegateGuid: String,
        val lambdaArgumentKindsLiteral: String,
        val lambdaCallbackInvocation: CodeBlock,
        val addVtableIndex: Int,
        val removeVtableIndex: Int,
    )
}
