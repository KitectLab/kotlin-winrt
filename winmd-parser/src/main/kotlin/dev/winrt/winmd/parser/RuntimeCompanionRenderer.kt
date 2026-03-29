package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdActivationKind
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType

internal class RuntimeCompanionRenderer(
    private val typeRegistry: TypeRegistry,
    private val typeNameMapper: TypeNameMapper,
    private val delegateLambdaPlanResolver: DelegateLambdaPlanResolver,
    private val winRtSignatureMapper: WinRtSignatureMapper,
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
        val staticsType = typeRegistry.findType("I${type.name}Statics", type.namespace) ?: return emptyList()
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
                renderForwardingLambdaOverload(method, type.namespace)?.let(members::add)
            }
        synthesizeGetterProperties(staticsType.methods, declaredPropertyNames, type.namespace).forEach(members::add)
        return members
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
        ) ?: return null

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
        when (plan.bridge.argumentKinds.singleOrNull()) {
            null -> {
                builder.addStatement(
                    "val delegateHandle = %T.%L(%T.iid, %L) { _ -> %N() }",
                    PoetSymbols.winRtDelegateBridgeClass,
                    plan.bridge.factoryMethod,
                    delegateClass,
                    argumentKindsLiteral,
                    method.parameters.single().name,
                )
            }
            DelegateArgumentKind.OBJECT -> {
                val callbackType = plan.lambdaType.parameters.single()
                builder.addStatement(
                    "val delegateHandle = %T.%L(%T.iid, %L) { args -> %N(%L(args.single() as %T)) }",
                    PoetSymbols.winRtDelegateBridgeClass,
                    plan.bridge.factoryMethod,
                    delegateClass,
                    argumentKindsLiteral,
                    method.parameters.single().name,
                    callbackType,
                    PoetSymbols.comPtrClass,
                )
            }
            else -> {
                val callbackType = plan.lambdaType.parameters.single()
                builder.addStatement(
                    "val delegateHandle = %T.%L(%T.iid, %L) { args -> %N(args.single() as %T) }",
                    PoetSymbols.winRtDelegateBridgeClass,
                    plan.bridge.factoryMethod,
                    delegateClass,
                    argumentKindsLiteral,
                    method.parameters.single().name,
                    callbackType,
                )
            }
        }
        builder.addStatement("statics.%L(%T(delegateHandle.pointer))", companionForwardTargetName(method), delegateClass)
        builder.addStatement("return delegateHandle")
        return builder.build()
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
}
