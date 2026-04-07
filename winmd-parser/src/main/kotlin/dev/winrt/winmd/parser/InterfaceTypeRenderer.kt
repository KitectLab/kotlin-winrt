package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeVariableName.Companion.invoke
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import dev.winrt.winmd.plugin.stripValueTypeNameMarker

internal class InterfaceTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
    private val delegateLambdaPlanResolver: DelegateLambdaPlanResolver,
    private val eventSlotDelegatePlanResolver: EventSlotDelegatePlanResolver,
    private val typeRegistry: TypeRegistry,
    private val asyncMethodProjectionPlanner: AsyncMethodProjectionPlanner,
    private val asyncMethodRuleRegistry: AsyncMethodRuleRegistry,
    private val winRtSignatureMapper: WinRtSignatureMapper,
    private val winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    private val projectedObjectArgumentLowering: ProjectedObjectArgumentLowering =
        ProjectedObjectArgumentLowering(typeRegistry, winRtSignatureMapper, winRtProjectionTypeMapper),
    private val valueTypeProjectionSupport: ValueTypeProjectionSupport = ValueTypeProjectionSupport(typeNameMapper, typeRegistry),
    private val kotlinCollectionProjectionMapper: KotlinCollectionProjectionMapper = KotlinCollectionProjectionMapper(),
) {
    internal fun supportsForwardedMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
    ): Boolean = supportsInterfaceMethod(method, currentNamespace, genericParameters)

    fun render(type: WinMdType): List<TypeSpec> {
        if (typeRegistry.isRuntimeClassOverridesInterface(type.name, type.namespace)) {
            return emptyList()
        }
        return if (typeRegistry.isRuntimeProjectedInterface(type.name, type.namespace)) {
            listOf(renderInterfaceContract(type), renderInterfaceProjection(type))
        } else {
            listOf(renderInterfaceClass(type))
        }
    }

    private fun renderInterfaceClass(type: WinMdType): TypeSpec {
        val rawTypeClass = projectedDeclarationClassName(type.namespace, type.name)
        val typeVariables = type.genericParameters.map { TypeVariableName(it) }
        val typeClass = if (typeVariables.isEmpty()) rawTypeClass else rawTypeClass.parameterizedBy(typeVariables)
        val genericParameters = type.genericParameters.toSet()
        val declarationName = projectedDeclarationSimpleName(type.name)
        val directBaseInterface = directBaseInterface(type, type.namespace)
        val directBaseIsRuntimeProjected = directBaseInterface?.let {
            typeRegistry.isRuntimeProjectedInterface(it, type.namespace)
        } == true
        val directBaseInterfaceType = directBaseInterface
            ?.let { projectedInterfaceTypeName(it, type.namespace, genericParameters) }
        val inheritedSignatureKeys = inheritedSignatureKeys(directBaseInterface)
        val inheritedPropertyNames = inheritedPropertyNames(directBaseInterface)
        val projectedProperties = dedupeInterfaceProperties(
            declaredAndSyntheticInterfaceProperties(type, inheritedPropertyNames),
        )
        val declaredMethods = dedupeInterfaceMethods(type.methods)
        val collectionProjection = kotlinCollectionProjectionMapper.interfaceProjection(type)
        val renderedProperties = projectedProperties.mapNotNull { property ->
            renderProperty(property, type.namespace, genericParameters)?.let { rendered -> property.name to rendered }
        }
        val projectedPropertyNames = renderedProperties.mapTo(linkedSetOf()) { it.first }
        return TypeSpec.classBuilder(declarationName)
            .addModifiers(KModifier.OPEN)
            .apply {
                if (
                    typeRegistry.isRuntimeClassHelperInterface(type.name, type.namespace) ||
                    typeRegistry.isVersionedRuntimeClassInterface(type.name, type.namespace)
                ) {
                    addModifiers(KModifier.INTERNAL)
                }
            }
            .apply {
                typeVariables.forEach(::addTypeVariable)
            }
            .primaryConstructor(pointerConstructor())
            .superclass(
                directBaseInterfaceType
                    ?.takeUnless { directBaseIsRuntimeProjected }
                    ?: PoetSymbols.winRtInterfaceProjectionClass,
            )
            .addSuperclassConstructorParameter("pointer")
            .apply {
                directBaseInterfaceType
                    ?.takeIf { directBaseIsRuntimeProjected }
                    ?.let(::addSuperinterface)
                addCollectionSuperinterfaces(
                    baseInterfaces = type.baseInterfaces,
                    currentNamespace = type.namespace,
                    genericParameters = genericParameters,
                    projection = collectionProjection,
                    preferProjectionDelegate = true,
                )
                collectionProjection?.let { projection ->
                    projection.extraProperties.forEach(::addProperty)
                    projection.extraFunctions.forEach(::addFunction)
                    projection.winRtSizeSlot
                        ?.let(kotlinCollectionProjectionMapper::buildWinRtSizeProperty)
                        ?.let(::addProperty)
                }
                renderDispatchQueueOverride(type, type.namespace, genericParameters)?.let { dispatchOverride ->
                    addSuperinterface(PoetSymbols.dispatchQueueClass)
                    addFunction(dispatchOverride)
                }
            }
            .addProperties(
                renderedProperties.map { it.second },
            )
            .addProperties(renderEventSlotProperties(type, type.namespace, genericParameters))
            .addFunctions(
                declaredMethods
                    .filterNot { interfaceMethodRenderKey(it) in inheritedSignatureKeys }
                    .filterNot { it.isProjectedPropertyAccessor(projectedPropertyNames) }
                    .flatMap { renderMethods(it, type.namespace, genericParameters) },
            )
            .addFunctions(
                declaredMethods
                    .filterNot { interfaceMethodRenderKey(it) in inheritedSignatureKeys }
                    .filterNot { it.isProjectedPropertyAccessor(projectedPropertyNames) }
                    .mapNotNull { renderLambdaOverload(it, type.namespace, genericParameters) },
            )
            .addTypes(renderEventSlotTypes(type, type.namespace, genericParameters))
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addSuperinterface(PoetSymbols.winRtInterfaceMetadataClass)
                    .addProperty(overrideStringProperty("qualifiedName", "${type.namespace}.${type.name}"))
                    .addProperty(
                        overrideStringProperty(
                            "projectionTypeKey",
                            winRtProjectionTypeMapper.projectionTypeKeyFor("${type.namespace}.${type.name}", type.namespace),
                        ),
                    )
                    .addProperty(
                        PropertySpec.builder("iid", PoetSymbols.guidClass)
                            .addModifiers(KModifier.OVERRIDE)
                            .initializer("%M(%S)", PoetSymbols.guidOfMember, type.guid ?: "00000000-0000-0000-0000-000000000000")
                            .build(),
                    )
                    .apply {
                        addProjectionFactoryRegistration(type)
                        if (type.genericParameters.isEmpty()) {
                            addFunction(
                                FunSpec.builder("from")
                                    .apply {
                                typeVariables.forEach(::addTypeVariable)
                                    }
                                    .returns(typeClass)
                                    .addParameter("inspectable", PoetSymbols.inspectableClass)
                                    .addStatement("return inspectable.%M(this, ::%L)", PoetSymbols.projectInterfaceMember, declarationName)
                                    .build(),
                            )
                            addFunction(
                                FunSpec.builder("invoke")
                                    .addModifiers(KModifier.OPERATOR)
                                    .returns(typeClass)
                                    .addParameter("inspectable", PoetSymbols.inspectableClass)
                                    .addStatement("return from(inspectable)")
                                    .build(),
                            )
                        } else {
                            addFunction(renderGenericSignatureOf(type))
                            addFunction(renderGenericProjectionTypeKeyOf(type))
                            addFunction(renderGenericIidOf())
                            addFunction(renderGenericMetadataOf(type))
                            addFunction(renderGenericFrom(type, typeClass, typeVariables, declarationName))
                        }
                        type.methods.forEach { method ->
                            renderAsyncResultDescriptorProperty(method, type.namespace, genericParameters)?.let(::addProperty)
                            renderAsyncProgressDescriptorProperty(method, type.namespace, genericParameters)?.let(::addProperty)
                            renderAsyncAuthoringHelper(method, type.namespace, genericParameters)?.let(::addFunction)
                        }
                    }
                    .build(),
            )
            .build()
    }

    private fun renderInterfaceContract(type: WinMdType): TypeSpec {
        val typeVariables = type.genericParameters.map { TypeVariableName(it) }
        val genericParameters = type.genericParameters.toSet()
        val declarationName = projectedDeclarationSimpleName(type.name)
        val directBaseInterface = directBaseInterface(type, type.namespace)
        val directBaseInterfaceType = directBaseInterface
            ?.let { projectedInterfaceTypeName(it, type.namespace, genericParameters) }
        val inheritedSignatureKeys = inheritedSignatureKeys(directBaseInterface)
        val inheritedPropertyNames = inheritedPropertyNames(directBaseInterface)
        val projectedProperties = dedupeInterfaceProperties(
            declaredAndSyntheticInterfaceProperties(type, inheritedPropertyNames),
        )
        val declaredMethods = dedupeInterfaceMethods(type.methods)
        val projectedPropertyNames = projectedProperties.asSequence()
            .mapNotNull { property ->
                property.name.takeIf { renderInterfaceContractProperty(property, type.namespace, genericParameters) != null }
            }
            .toSet()
        val collectionProjection = kotlinCollectionProjectionMapper.interfaceProjection(type)
        return TypeSpec.interfaceBuilder(declarationName)
            .apply {
                if (typeRegistry.isVersionedRuntimeClassInterface(type.name, type.namespace)) {
                    addModifiers(KModifier.INTERNAL)
                }
                typeVariables.forEach(::addTypeVariable)
                directBaseInterfaceType?.let(::addSuperinterface)
                addCollectionSuperinterfaces(
                    baseInterfaces = type.baseInterfaces,
                    currentNamespace = type.namespace,
                    genericParameters = genericParameters,
                    projection = collectionProjection,
                    excludedBaseInterface = directBaseInterface,
                )
                renderDispatchQueueOverride(type, type.namespace, genericParameters)?.let { dispatchOverride ->
                    addSuperinterface(PoetSymbols.dispatchQueueClass)
                    addFunction(dispatchOverride)
                }
            }
            .addProperties(
                projectedProperties.mapNotNull { renderInterfaceContractProperty(it, type.namespace, genericParameters) },
            )
            .addFunctions(
                declaredMethods
                    .filterNot { interfaceMethodRenderKey(it) in inheritedSignatureKeys }
                    .filterNot { it.isProjectedPropertyAccessor(projectedPropertyNames) }
                    .mapNotNull { renderInterfaceContractMethod(it, type.namespace, genericParameters) },
            )
            .addType(renderInterfaceCompanion(type, typeVariables))
            .build()
    }

    private fun renderInterfaceProjection(type: WinMdType): TypeSpec {
        val genericParameters = type.genericParameters.toSet()
        val typeVariables = type.genericParameters.map { TypeVariableName(it) }
        val rawTypeClass = projectedDeclarationClassName(type.namespace, type.name)
        val typeClass = if (typeVariables.isEmpty()) rawTypeClass else rawTypeClass.parameterizedBy(typeVariables)
        val projectionName = "${projectedDeclarationSimpleName(type.name)}Projection"
        val projectedProperties = allInterfaceProperties(type)
        val projectedPropertyNames = projectedProperties.asSequence()
            .mapNotNull { property ->
                property.name.takeIf { renderProperty(property, type.namespace, genericParameters) != null }
            }
            .toSet()
        return TypeSpec.classBuilder(projectionName)
            .addModifiers(KModifier.PRIVATE)
            .apply {
                typeVariables.forEach(::addTypeVariable)
            }
            .primaryConstructor(pointerConstructor())
            .superclass(PoetSymbols.winRtInterfaceProjectionClass)
            .addSuperclassConstructorParameter("pointer")
            .apply {
                addSuperinterface(typeClass)
                kotlinCollectionProjectionMapper.interfaceProjection(type)?.let { projection ->
                    projection.delegateFactory?.let { delegateFactory ->
                        addSuperinterface(projection.superinterface, delegateFactory)
                    }
                    projection.extraProperties.forEach(::addProperty)
                    projection.extraFunctions.forEach(::addFunction)
                    projection.winRtSizeSlot
                        ?.let(kotlinCollectionProjectionMapper::buildWinRtSizeProperty)
                        ?.let(::addProperty)
                }
            }
            .addProperties(
                projectedProperties.mapNotNull {
                    renderProperty(it, type.namespace, genericParameters)
                        ?.toBuilder()
                        ?.addModifiers(KModifier.OVERRIDE)
                        ?.build()
                },
            )
            .addProperties(renderEventSlotProperties(type, type.namespace, genericParameters))
            .addFunctions(
                allInterfaceMethods(type)
                    .filterNot { it.isProjectedPropertyAccessor(projectedPropertyNames) }
                    .flatMap { method ->
                    renderMethods(method, type.namespace, genericParameters)
                        .map { it.toBuilder().addModifiers(KModifier.OVERRIDE).build() }
                },
            )
            .addFunctions(
                type.methods
                    .filterNot { it.isProjectedPropertyAccessor(projectedPropertyNames) }
                    .mapNotNull { renderLambdaOverload(it, type.namespace, genericParameters) },
            )
            .addTypes(renderEventSlotTypes(type, type.namespace, genericParameters))
            .build()
    }

    private fun renderInterfaceCompanion(type: WinMdType, typeVariables: List<TypeVariableName>): TypeSpec {
        val rawTypeClass = projectedDeclarationClassName(type.namespace, type.name)
        val typeClass = if (typeVariables.isEmpty()) rawTypeClass else rawTypeClass.parameterizedBy(typeVariables)
        val declarationName = projectedDeclarationSimpleName(type.name)
        return TypeSpec.companionObjectBuilder()
            .addSuperinterface(PoetSymbols.winRtInterfaceMetadataClass)
            .addProperty(overrideStringProperty("qualifiedName", "${type.namespace}.${type.name}"))
            .addProperty(
                overrideStringProperty(
                    "projectionTypeKey",
                    winRtProjectionTypeMapper.projectionTypeKeyFor("${type.namespace}.${type.name}", type.namespace),
                ),
            )
            .addProperty(
                PropertySpec.builder("iid", PoetSymbols.guidClass)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%M(%S)", PoetSymbols.guidOfMember, type.guid ?: "00000000-0000-0000-0000-000000000000")
                    .build(),
            )
            .apply {
                addProjectionFactoryRegistration(type)
                if (type.genericParameters.isEmpty()) {
                    addFunction(
                        FunSpec.builder("from")
                            .apply {
                                typeVariables.forEach(::addTypeVariable)
                            }
                            .returns(typeClass)
                            .addParameter("inspectable", PoetSymbols.inspectableClass)
                            .addStatement("return inspectable.%M(this, ::%LProjection)", PoetSymbols.projectInterfaceMember, declarationName)
                            .build(),
                    )
                    addFunction(
                        FunSpec.builder("invoke")
                            .addModifiers(KModifier.OPERATOR)
                            .returns(typeClass)
                            .addParameter("inspectable", PoetSymbols.inspectableClass)
                            .addStatement("return from(inspectable)")
                            .build(),
                    )
                } else {
                    addFunction(renderGenericSignatureOf(type))
                    addFunction(renderGenericProjectionTypeKeyOf(type))
                    addFunction(renderGenericIidOf())
                    addFunction(renderGenericMetadataOf(type))
                    addFunction(renderGenericFrom(type, typeClass, typeVariables, "${declarationName}Projection"))
                    addFunction(renderGenericInvoke(type, typeClass, typeVariables))
                }
                type.methods.forEach { method ->
                    renderAsyncResultDescriptorProperty(method, type.namespace, type.genericParameters.toSet())?.let(::addProperty)
                    renderAsyncProgressDescriptorProperty(method, type.namespace, type.genericParameters.toSet())?.let(::addProperty)
                    renderAsyncAuthoringHelper(method, type.namespace, type.genericParameters.toSet())?.let(::addFunction)
                }
            }
            .build()
    }

    private fun TypeSpec.Builder.addProjectionFactoryRegistration(type: WinMdType) {
        val projectionTypeKey = winRtProjectionTypeMapper.projectionTypeKeyFor("${type.namespace}.${type.name}", type.namespace)
        if (type.genericParameters.isEmpty()) {
            addInitializerBlock(
                CodeBlock.of(
                    "%T.registerFactory(%S) { inspectable -> from(inspectable) }\n",
                    PoetSymbols.winRtProjectionFactoryRegistryClass,
                    projectionTypeKey,
                ),
            )
            return
        }

        val builder = CodeBlock.builder()
            .add(
                "%T.registerOpenFactory(%S) { inspectable, argumentSignatures, argumentProjectionTypeKeys ->\n",
                PoetSymbols.winRtProjectionFactoryRegistryClass,
                projectionTypeKey,
            )
            .indent()
            .addStatement(
                "require(argumentSignatures.size == %L) { %S }",
                type.genericParameters.size,
                "Expected ${type.genericParameters.size} generic signatures for ${type.namespace}.${type.name}",
            )
            .addStatement(
                "require(argumentProjectionTypeKeys.size == %L) { %S }",
                type.genericParameters.size,
                "Expected ${type.genericParameters.size} projection type keys for ${type.namespace}.${type.name}",
            )
            .add("from(inspectable")
        type.genericParameters.indices.forEach { index ->
            builder.add(", argumentSignatures[%L]", index)
        }
        type.genericParameters.indices.forEach { index ->
            builder.add(", argumentProjectionTypeKeys[%L]", index)
        }
        builder
            .add(")\n")
            .unindent()
            .add("}\n")
        addInitializerBlock(builder.build())
    }

    private fun renderInterfaceContractProperty(
        property: WinMdProperty,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PropertySpec? {
        if (!supportsInterfaceProperty(property, currentNamespace, genericParameters)) {
            return null
        }
        return PropertySpec.builder(
            property.name.replaceFirstChar(Char::lowercase),
            typeNameMapper.mapTypeName(property.type, currentNamespace, genericParameters),
        ).apply {
            addModifiers(KModifier.ABSTRACT)
            if (property.mutable) {
                mutable()
            }
        }.build()
    }

    private fun renderInterfaceContractMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): FunSpec? {
        if (!isKotlinIdentifier(method.name) || !supportsInterfaceMethod(method, currentNamespace, genericParameters)) {
            return null
        }
        renderProjectedIndexOfInterfaceContractMethod(method, currentNamespace, genericParameters)?.let { return it }
        return FunSpec.builder(kotlinMethodName(method))
            .apply {
                addModifiers(KModifier.ABSTRACT)
                if (method.name == "ToString" && method.returnType == "String" && method.parameters.isEmpty()) {
                    addModifiers(KModifier.OVERRIDE)
                }
            }
            .returns(typeNameMapper.mapTypeName(method.returnType, currentNamespace, genericParameters))
            .addParameters(method.parameters.map { parameter ->
                ParameterSpec.builder(
                    parameter.name.replaceFirstChar(Char::lowercase),
                    typeNameMapper.mapTypeName(parameter.type, currentNamespace, genericParameters),
                ).build()
            })
            .build()
    }

    private fun supportsProjectedIndexOfInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
    ): Boolean {
        return method.isIndexOfOutUInt32Method() &&
            method.vtableIndex != null &&
            lowerInterfaceArrayMethodArgument(method.parameters.first(), currentNamespace) != null
    }

    private fun renderProjectedIndexOfInterfaceContractMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): FunSpec? {
        if (!supportsProjectedIndexOfInterfaceMethod(method, currentNamespace)) {
            return null
        }
        val valueParameter = method.parameters.first()
        return FunSpec.builder(kotlinMethodName(method))
            .addModifiers(KModifier.ABSTRACT)
            .returns(PoetSymbols.uint32Class.copy(nullable = true))
            .addParameter(
                valueParameter.name.replaceFirstChar(Char::lowercase),
                typeNameMapper.mapTypeName(valueParameter.type, currentNamespace, genericParameters),
            )
            .build()
    }

    private fun renderProjectedIndexOfInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): FunSpec? {
        if (!supportsProjectedIndexOfInterfaceMethod(method, currentNamespace)) {
            return null
        }
        val valueParameter = method.parameters.first()
        val valueParameterName = valueParameter.name.replaceFirstChar(Char::lowercase)
        val abiArgument = lowerInterfaceArrayMethodArgument(valueParameter, currentNamespace) ?: return null
        return FunSpec.builder(kotlinMethodName(method))
            .returns(PoetSymbols.uint32Class.copy(nullable = true))
            .addParameter(
                valueParameterName,
                typeNameMapper.mapTypeName(valueParameter.type, currentNamespace, genericParameters),
            )
            .addStatement(
                "val (found, index) = %T.invokeIndexOfMethod(pointer, %L, %L).getOrThrow()",
                PoetSymbols.platformComInteropClass,
                method.vtableIndex!!,
                abiArgument,
            )
            .addStatement("return if (found) %T(index) else null", PoetSymbols.uint32Class)
            .build()
    }

    private fun allInterfaceMethods(type: WinMdType): List<WinMdMethod> {
        val directBaseInterface = directBaseInterface(type, type.namespace)
        val inherited = directBaseInterface?.let { typeRegistry.findType(it, type.namespace) }?.let(::allInterfaceMethods).orEmpty()
        return dedupeInterfaceMethods(inherited + type.methods)
    }

    private fun allInterfaceProperties(type: WinMdType): List<WinMdProperty> {
        val directBaseInterface = directBaseInterface(type, type.namespace)
        val inherited = directBaseInterface?.let { typeRegistry.findType(it, type.namespace) }?.let(::allInterfaceProperties).orEmpty()
        return dedupeInterfaceProperties(inherited + declaredAndSyntheticInterfaceProperties(type))
    }

    private fun declaredAndSyntheticInterfaceProperties(
        type: WinMdType,
        excludedPropertyNames: Set<String> = emptySet(),
    ): List<WinMdProperty> {
        val declaredPropertyNames = type.properties.mapTo(linkedSetOf()) { it.name }
        return buildList {
            addAll(type.properties.filterNot { it.name in excludedPropertyNames })
            addAll(synthesizePropertiesFromAccessorMethods(type.methods, declaredPropertyNames + excludedPropertyNames))
        }
    }

    private fun renderEventSlotProperties(
        type: WinMdType,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): List<PropertySpec> {
        return eventSlotPlans(type, currentNamespace, genericParameters).flatMap { plan ->
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
        }
    }

    private fun renderEventSlotTypes(
        type: WinMdType,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): List<TypeSpec> {
        return eventSlotPlans(type, currentNamespace, genericParameters).map { plan ->
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
                        FunSpec.builder("subscribeScoped")
                            .addParameter("handler", plan.delegateType)
                            .returns(AutoCloseable::class)
                            .addStatement("val token = subscribe(handler)")
                            .addStatement("return AutoCloseable { unsubscribe(token) }")
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
                            .beginControlFlow("try")
                            .addStatement("val token = subscribe(%T(delegateHandle.pointer))", plan.delegateType)
                            .addStatement("delegateHandles[token] = delegateHandle")
                            .addStatement("return token")
                            .nextControlFlow("catch (t: Throwable)")
                            .addStatement("delegateHandle.close()")
                            .addStatement("throw t")
                            .endControlFlow()
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("subscribeScoped")
                            .addParameter("handler", plan.lambdaType)
                            .returns(AutoCloseable::class)
                            .addStatement("val token = subscribe(handler)")
                            .addStatement("return AutoCloseable { unsubscribe(token) }")
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
                        FunSpec.builder("invoke")
                            .addModifiers(KModifier.OPERATOR)
                            .addParameter("handler", plan.delegateType)
                            .returns(PoetSymbols.eventRegistrationTokenClass)
                            .addStatement("return subscribe(handler)")
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("invoke")
                            .addModifiers(KModifier.OPERATOR)
                            .addParameter("handler", plan.lambdaType)
                            .returns(PoetSymbols.eventRegistrationTokenClass)
                            .addStatement("return subscribe(handler)")
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("unsubscribe")
                            .addParameter("token", PoetSymbols.eventRegistrationTokenClass)
                            .beginControlFlow("try")
                            .addStatement("%T.invokeUnitMethodWithInt64Arg(pointer, %L, token.value).getOrThrow()", PoetSymbols.platformComInteropClass, plan.removeVtableIndex)
                            .nextControlFlow("finally")
                            .addStatement("delegateHandles.remove(token)?.close()")
                            .endControlFlow()
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
        }
    }

    private fun eventSlotPlans(
        type: WinMdType,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): List<InterfaceEventSlotPlan> {
        val dedupedMethods = dedupeInterfaceMethods(type.methods)
        val methodsByName = dedupedMethods.associateBy { it.name }
        return dedupedMethods.mapNotNull { addMethod ->
            if (!addMethod.name.startsWith("add_") || addMethod.parameters.size != 1 || !isEventRegistrationTokenType(addMethod.returnType)) {
                return@mapNotNull null
            }
            val eventName = addMethod.name.removePrefix("add_")
            val removeMethod = methodsByName["remove_$eventName"] ?: return@mapNotNull null
            if (
                removeMethod.parameters.size != 1 ||
                !isEventRegistrationTokenType(removeMethod.parameters.single().type) ||
                removeMethod.returnType != "Unit"
            ) {
                return@mapNotNull null
            }
            val delegateTypeName = addMethod.parameters.single().type
            val delegatePlan = eventSlotDelegatePlanResolver.resolve(delegateTypeName, currentNamespace, genericParameters)
                ?: return@mapNotNull null
            InterfaceEventSlotPlan(
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
    }

    private fun renderAsyncResultDescriptorProperty(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PropertySpec? {
        val resultType = asyncMethodRuleRegistry.plan(method, currentNamespace, genericParameters)
            ?.let { asyncMethodProjectionPlanner.asyncResultTypeName(method.returnType, currentNamespace, genericParameters) }
            ?: return null
        val descriptorExpression = asyncMethodProjectionPlanner.asyncResultDescriptorExpression(
            method.returnType,
            currentNamespace,
            genericParameters,
        )
            ?: return null
        return PropertySpec.builder("${kotlinMethodName(method)}ResultType", PoetSymbols.asyncResultTypeClass.parameterizedBy(resultType))
            .initializer("%L", descriptorExpression)
            .build()
    }

    private fun renderAsyncProgressDescriptorProperty(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PropertySpec? {
        val progressType = asyncMethodProjectionPlanner.asyncProgressTypeName(method.returnType, currentNamespace, genericParameters)
            ?: return null
        val descriptorExpression = asyncMethodProjectionPlanner.asyncProgressDescriptorExpression(
            method.returnType,
            currentNamespace,
            genericParameters,
        )
            ?: return null
        return PropertySpec.builder("${kotlinMethodName(method)}ProgressType", PoetSymbols.asyncProgressTypeClass.parameterizedBy(progressType))
            .initializer("%L", descriptorExpression)
            .build()
    }

    private fun renderAsyncAuthoringHelper(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): FunSpec? {
        val asyncPlan = asyncMethodRuleRegistry.plan(method, currentNamespace, genericParameters) ?: return null
        val functionName = "${kotlinMethodName(method)}Task"
        val builder = FunSpec.builder(functionName)
            .addParameter("scope", PoetSymbols.coroutineScopeClass)
            .returns(asyncPlan.rawReturnType)
        when {
            method.returnType == "Windows.Foundation.IAsyncAction" -> {
                builder.addParameter(
                    "block",
                    LambdaTypeName.get(
                        receiver = PoetSymbols.coroutineScopeClass,
                        returnType = Unit::class.asTypeName(),
                    ).copy(suspending = true),
                )
                builder.addStatement("return scope.%M(block = block)", PoetSymbols.asyncActionMember)
            }
            method.returnType.startsWith("Windows.Foundation.IAsyncOperation<") -> {
                val resultType = asyncMethodProjectionPlanner.asyncResultTypeName(method.returnType, currentNamespace, genericParameters)
                    ?: return null
                builder.addParameter(
                    "block",
                    LambdaTypeName.get(
                        receiver = PoetSymbols.coroutineScopeClass,
                        returnType = resultType,
                    ).copy(suspending = true),
                )
                builder.addStatement(
                    "return scope.%M(resultType = %N, block = block)",
                    PoetSymbols.asyncOperationMember,
                    "${kotlinMethodName(method)}ResultType",
                )
            }
            method.returnType.startsWith("Windows.Foundation.IAsyncActionWithProgress<") -> {
                val progressType = asyncMethodProjectionPlanner.asyncProgressTypeName(method.returnType, currentNamespace, genericParameters)
                    ?: return null
                val progressCallbackType = LambdaTypeName.get(
                    parameters = listOf(ParameterSpec.builder("value", progressType).build()),
                    returnType = Unit::class.asTypeName(),
                )
                builder.addParameter(
                    "block",
                    LambdaTypeName.get(
                        receiver = PoetSymbols.coroutineScopeClass,
                        progressCallbackType,
                        returnType = Unit::class.asTypeName(),
                    ).copy(suspending = true),
                )
                builder.addStatement(
                    "return scope.%M(progressType = %N, block = block)",
                    PoetSymbols.asyncActionWithProgressMember,
                    "${kotlinMethodName(method)}ProgressType",
                )
            }
            method.returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") -> {
                val resultType = asyncMethodProjectionPlanner.asyncResultTypeName(method.returnType, currentNamespace, genericParameters)
                    ?: return null
                val progressType = asyncMethodProjectionPlanner.asyncProgressTypeName(method.returnType, currentNamespace, genericParameters)
                    ?: return null
                val progressCallbackType = LambdaTypeName.get(
                    parameters = listOf(ParameterSpec.builder("value", progressType).build()),
                    returnType = Unit::class.asTypeName(),
                )
                builder.addParameter(
                    "block",
                    LambdaTypeName.get(
                        receiver = PoetSymbols.coroutineScopeClass,
                        progressCallbackType,
                        returnType = resultType,
                    ).copy(suspending = true),
                )
                builder.addStatement(
                    "return scope.%M(resultType = %N, progressType = %N, block = block)",
                    PoetSymbols.asyncOperationWithProgressMember,
                    "${kotlinMethodName(method)}ResultType",
                    "${kotlinMethodName(method)}ProgressType",
                )
            }
            else -> return null
        }
        return builder.build()
    }

    private fun renderMethods(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): List<FunSpec> {
        return listOfNotNull(
            renderMethod(method, currentNamespace, genericParameters),
            renderAsyncAwaitMethod(method, currentNamespace, genericParameters),
        )
    }

    private fun renderGenericSignatureOf(type: WinMdType): FunSpec {
        val argumentSignatureVars = type.genericParameters.indices.joinToString(", ") { index -> "arg${index}Signature" }
        return FunSpec.builder("signatureOf")
            .returns(String::class)
            .addParameters(
                type.genericParameters.indices.map { index ->
                    ParameterSpec.builder("arg${index}Signature", String::class).build()
                },
            )
            .addStatement(
                "return %T.parameterizedInterface(%S, $argumentSignatureVars)",
                PoetSymbols.winRtTypeSignatureClass,
                type.guid ?: "00000000-0000-0000-0000-000000000000",
            )
            .build()
    }

    private fun renderGenericIidOf(): FunSpec {
        return FunSpec.builder("iidOf")
            .returns(PoetSymbols.guidClass)
            .addParameter(
                ParameterSpec.builder("argumentSignatures", String::class)
                    .addModifiers(KModifier.VARARG)
                    .build(),
            )
            .addStatement("return %T.createFromSignature(signatureOf(*argumentSignatures))", PoetSymbols.parameterizedInterfaceIdClass)
            .build()
    }

    private fun renderGenericProjectionTypeKeyOf(type: WinMdType): FunSpec {
        val argumentTypeKeyVars = type.genericParameters.indices.joinToString(", ") { index -> "arg${index}ProjectionTypeKey" }
        val mappedRawType = winRtProjectionTypeMapper.projectionTypeKeyFor("${type.namespace}.${type.name}", type.namespace)
        return FunSpec.builder("projectionTypeKeyOf")
            .returns(String::class)
            .addParameters(
                type.genericParameters.indices.map { index ->
                    ParameterSpec.builder("arg${index}ProjectionTypeKey", String::class).build()
                },
            )
            .addStatement(
                "return %S + %S + listOf($argumentTypeKeyVars).joinToString(%S) + %S",
                "$mappedRawType<",
                "",
                ", ",
                ">",
            )
            .build()
    }

    private fun renderGenericMetadataOf(type: WinMdType): FunSpec {
        val metadataType = PoetSymbols.winRtInterfaceMetadataClass
        val argumentSignatureVars = type.genericParameters.indices.joinToString(", ") { index -> "arg${index}Signature" }
        val argumentTypeKeyVars = type.genericParameters.indices.joinToString(", ") { index -> "arg${index}ProjectionTypeKey" }
        return FunSpec.builder("metadataOf")
            .returns(metadataType)
            .addParameters(
                type.genericParameters.indices.map { index ->
                    ParameterSpec.builder("arg${index}Signature", String::class).build()
                } + type.genericParameters.indices.map { index ->
                    ParameterSpec.builder("arg${index}ProjectionTypeKey", String::class).build()
                }
            )
            .addCode(
                CodeBlock.builder()
                    .add("return object : %T {\n", metadataType)
                    .indent()
                    .add("override val qualifiedName: String = %S\n", "${type.namespace}.${type.name}")
                    .add("override val projectionTypeKey: String = projectionTypeKeyOf($argumentTypeKeyVars)\n")
                    .add("override val iid: %T = iidOf($argumentSignatureVars)\n", PoetSymbols.guidClass)
                    .add("override fun <T : dev.winrt.core.WinRtObject> project(pointer: dev.winrt.kom.ComPtr, constructor: (dev.winrt.kom.ComPtr) -> T): T {\n")
                    .indent()
                    .add("return constructor(pointer).also { wrapper ->\n")
                    .indent()
                    .apply {
                        type.genericParameters.indices.forEach { index ->
                            add(
                                "wrapper.additionalTypeData[%S] = arg${index}Signature\n",
                                "generic:${type.namespace}.${type.name}:arg${index}:signature",
                            )
                            add(
                                "wrapper.additionalTypeData[%S] = arg${index}ProjectionTypeKey\n",
                                "generic:${type.namespace}.${type.name}:arg${index}:projectionTypeKey",
                            )
                        }
                    }
                    .unindent()
                    .add("}\n")
                    .unindent()
                    .add("}\n")
                    .unindent()
                    .add("}\n")
                    .build(),
            )
            .build()
    }

    private fun renderGenericFrom(
        type: WinMdType,
        typeClass: TypeName,
        typeVariables: List<TypeVariableName>,
        constructorName: String,
    ): FunSpec {
        val argumentParameters = type.genericParameters.indices.map { index ->
            ParameterSpec.builder("arg${index}Signature", String::class).build()
        }
        val projectionTypeKeyParameters = type.genericParameters.indices.map { index ->
            ParameterSpec.builder("arg${index}ProjectionTypeKey", String::class).build()
        }
        val metadataArgumentNames = (argumentParameters + projectionTypeKeyParameters).joinToString(", ") { it.name }
        return FunSpec.builder("from")
            .apply {
                typeVariables.forEach(::addTypeVariable)
            }
            .returns(typeClass)
            .addParameter("inspectable", PoetSymbols.inspectableClass)
            .addParameters(argumentParameters)
            .addParameters(projectionTypeKeyParameters)
            .addStatement(
                "return inspectable.%M(metadataOf($metadataArgumentNames), ::%L)",
                PoetSymbols.projectInterfaceMember,
                constructorName,
            )
            .build()
    }

    private fun renderGenericInvoke(
        type: WinMdType,
        typeClass: TypeName,
        typeVariables: List<TypeVariableName>,
    ): FunSpec {
        val argumentParameters = type.genericParameters.indices.map { index ->
            ParameterSpec.builder("arg${index}Signature", String::class).build()
        }
        val projectionTypeKeyParameters = type.genericParameters.indices.map { index ->
            ParameterSpec.builder("arg${index}ProjectionTypeKey", String::class).build()
        }
        val callArguments = (listOf("inspectable") + argumentParameters.map { it.name } + projectionTypeKeyParameters.map { it.name })
            .joinToString(", ")
        return FunSpec.builder("invoke")
            .addModifiers(KModifier.OPERATOR)
            .apply {
                typeVariables.forEach(::addTypeVariable)
            }
            .returns(typeClass)
            .addParameter("inspectable", PoetSymbols.inspectableClass)
            .addParameters(argumentParameters)
            .addParameters(projectionTypeKeyParameters)
            .addStatement("return from($callArguments)")
            .build()
    }

    private fun renderProperty(
        property: WinMdProperty,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PropertySpec? {
        if (!supportsInterfaceProperty(property, currentNamespace, genericParameters)) {
            return null
        }
        val propertyName = property.name.replaceFirstChar(Char::lowercase)
        val propertyType = typeNameMapper.mapTypeName(property.type, currentNamespace, genericParameters)
        val getterVtableIndex = property.getterVtableIndex!!
        val getterBuilder = FunSpec.getterBuilder()
        val propertyProjection = valueTypeProjectionSupport.propertyProjection(property.type, currentNamespace)
        if (propertyProjection != null) {
            getterBuilder.addStatement(
                "return %L",
                propertyProjection.interfaceGetterExpression(
                    property.type,
                    currentNamespace,
                    genericParameters,
                    getterVtableIndex,
                ) ?: return null,
            )
        } else when (
                PropertyRuleRegistry.interfaceGetterRuleFamily(
                    property.type,
                    typeRegistry.isEnumType(property.type, currentNamespace),
                    supportsInterfaceObjectReturnType(property.type, currentNamespace),
                )
            ) {
            InterfacePropertyRuleFamily.ENUM -> {
                val underlyingType = enumUnderlyingTypeOrDefault(typeRegistry, property.type, currentNamespace)
                getterBuilder.addStatement(
                    "return %T.fromValue(%L)",
                    propertyType,
                    enumGetterAbiCall(underlyingType, getterVtableIndex),
                )
            }
            InterfacePropertyRuleFamily.OBJECT ->
                getterBuilder.addStatement(
                    "return %L",
                    projectedObjectReturnCode(
                        typeName = property.type,
                        currentNamespace = currentNamespace,
                        abiCall = AbiCallCatalog.objectMethod(getterVtableIndex),
                        genericParameters = genericParameters,
                    ),
                )
            InterfacePropertyRuleFamily.STRING ->
                getterBuilder.addStatement(
                    "return %L",
                    HStringSupport.toKotlinString("pointer", getterVtableIndex),
                )
            InterfacePropertyRuleFamily.UINT8,
            InterfacePropertyRuleFamily.INT16,
            InterfacePropertyRuleFamily.UINT16,
            InterfacePropertyRuleFamily.CHAR16,
            -> getterBuilder.addStatement(
                "return %L",
                valueTypeProjectionSupport.smallScalarAbiCall(property.type, getterVtableIndex, emptyList())
                    ?: return null,
            )
            InterfacePropertyRuleFamily.FLOAT32 ->
                getterBuilder.addStatement(
                    "return %T(%L)",
                    PoetSymbols.float32Class,
                    AbiCallCatalog.float32Method(getterVtableIndex),
                )
            InterfacePropertyRuleFamily.FLOAT64 ->
                getterBuilder.addStatement(
                    "return %T(%L)",
                    PoetSymbols.float64Class,
                    AbiCallCatalog.float64Method(getterVtableIndex),
                )
            InterfacePropertyRuleFamily.BOOLEAN ->
                getterBuilder.addStatement(
                    "return %T(%T.invokeBooleanGetter(pointer, %L).getOrThrow())",
                    PoetSymbols.winRtBooleanClass,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            InterfacePropertyRuleFamily.GUID ->
                getterBuilder.addStatement(
                    "return %T.parse(%T.invokeGuidGetter(pointer, %L).getOrThrow().toString())",
                    PoetSymbols.guidValueClass,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            InterfacePropertyRuleFamily.DATE_TIME ->
                getterBuilder.addStatement(
                    "val ticks = %T.invokeInt64Getter(pointer, %L).getOrThrow()\nreturn %T.fromEpochSeconds((ticks - %L) / 10000000L, ((ticks - %L) %% 10000000L * 100).toInt())",
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                    PoetSymbols.dateTimeClass,
                    WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
                    WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
                )
            InterfacePropertyRuleFamily.TIME_SPAN ->
                getterBuilder.addStatement(
                    "return %T(%T.invokeInt64Getter(pointer, %L).getOrThrow())",
                    PoetSymbols.timeSpanClass,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            InterfacePropertyRuleFamily.EVENT_REGISTRATION_TOKEN ->
                getterBuilder.addStatement(
                    "return %T(%T.invokeInt64Getter(pointer, %L).getOrThrow())",
                    PoetSymbols.eventRegistrationTokenClass,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            InterfacePropertyRuleFamily.HRESULT ->
                getterBuilder.addStatement(
                    "return %M(%T.invokeInt32Method(pointer, %L).getOrThrow())",
                    PoetSymbols.exceptionFromHResultMember,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            InterfacePropertyRuleFamily.INT32 ->
                getterBuilder.addStatement(
                    "return %T(%T.invokeInt32Method(pointer, %L).getOrThrow())",
                    PoetSymbols.int32Class,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            InterfacePropertyRuleFamily.UINT32 ->
                getterBuilder.addStatement(
                    "return %T(%T.invokeUInt32Method(pointer, %L).getOrThrow())",
                    PoetSymbols.uint32Class,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            InterfacePropertyRuleFamily.INT64 ->
                getterBuilder.addStatement(
                    "return %T(%T.invokeInt64Getter(pointer, %L).getOrThrow())",
                    PoetSymbols.int64Class,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            InterfacePropertyRuleFamily.UINT64 ->
                getterBuilder.addStatement(
                    "return %T(%T.invokeInt64Getter(pointer, %L).getOrThrow().toULong())",
                    PoetSymbols.uint64Class,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            else -> return null
        }
        val propertyBuilder = PropertySpec.builder(propertyName, propertyType)
            .getter(getterBuilder.build())
        val isEnumProperty = typeRegistry.isEnumType(property.type, currentNamespace)
        val setterRuleFamily = PropertyRuleRegistry.interfaceSetterRuleFamily(
            property.type,
            supportsInterfaceObjectType(property.type, currentNamespace),
        )
        val setterExpression = property
            .setterVtableIndex
            ?.let { setterVtableIndex ->
                propertyProjection?.interfaceSetterExpression(
                    property.type,
                    currentNamespace,
                    setterVtableIndex,
                    "value",
                )
            }
        if (
            property.mutable &&
            property.setterVtableIndex != null &&
            (
                setterExpression != null ||
                    isEnumProperty ||
                    setterRuleFamily != null
            )
        ) {
            val setterVtableIndex = property.setterVtableIndex!!
            propertyBuilder.mutable()
            propertyBuilder.setter(
                FunSpec.setterBuilder()
                    .addParameter("value", propertyType)
                    .apply {
                        when {
                            setterExpression != null -> addStatement(
                                "%L",
                                setterExpression,
                            )
                            isEnumProperty -> {
                                addStatement(
                                    "%L",
                                    enumSetterAbiCall(
                                        enumUnderlyingTypeOrDefault(typeRegistry, property.type, currentNamespace),
                                        setterVtableIndex,
                                    ),
                                )
                            }
                            else -> when (setterRuleFamily) {
                                InterfacePropertyRuleFamily.OBJECT -> addStatement(
                                    "%L",
                                    AbiCallCatalog.objectSetterExpression(
                                        setterVtableIndex,
                                        interfaceObjectArgumentExpression(
                                            argumentName = "value",
                                            typeName = property.type,
                                            currentNamespace = currentNamespace,
                                        ),
                                    ),
                                )
                                InterfacePropertyRuleFamily.STRING -> addStatement("%L", AbiCallCatalog.stringSetter(setterVtableIndex))
                                InterfacePropertyRuleFamily.UINT8,
                                InterfacePropertyRuleFamily.INT16,
                                InterfacePropertyRuleFamily.UINT16,
                                InterfacePropertyRuleFamily.CHAR16,
                                -> addStatement(
                                    "%L",
                                    valueTypeProjectionSupport.invokeUnitMethodWithArgs(
                                        vtableIndex = setterVtableIndex,
                                        arguments = listOf(CodeBlock.of("value")),
                                    ),
                                )
                                InterfacePropertyRuleFamily.FLOAT32 -> addStatement("%L", AbiCallCatalog.float32Setter(setterVtableIndex))
                                InterfacePropertyRuleFamily.FLOAT64 -> addStatement("%L", AbiCallCatalog.float64Setter(setterVtableIndex))
                                InterfacePropertyRuleFamily.BOOLEAN -> addStatement("%L", AbiCallCatalog.booleanSetter(setterVtableIndex))
                                InterfacePropertyRuleFamily.HRESULT -> addStatement(
                                    "%L",
                                    AbiCallCatalog.int32SetterExpression(
                                        setterVtableIndex,
                                        "hResultOfException(value)",
                                    ),
                                )
                                InterfacePropertyRuleFamily.INT32 -> addStatement("%L", AbiCallCatalog.int32Setter(setterVtableIndex))
                                InterfacePropertyRuleFamily.UINT32 -> addStatement("%L", AbiCallCatalog.uint32Setter(setterVtableIndex))
                                InterfacePropertyRuleFamily.INT64 -> addStatement("%L", AbiCallCatalog.int64Setter(setterVtableIndex))
                                InterfacePropertyRuleFamily.UINT64 -> addStatement("%L", AbiCallCatalog.uint64Setter(setterVtableIndex))
                                else -> return null
                            }
                        }
                    }
                    .build(),
            )
        }
        return propertyBuilder.build()
    }

    private fun renderMethod(method: WinMdMethod, currentNamespace: String, genericParameters: Set<String>): FunSpec? {
        if (!isKotlinIdentifier(method.name)) {
            return null
        }
        if (!supportsInterfaceMethod(method, currentNamespace, genericParameters)) {
            return null
        }
        renderAsyncTaskMethod(method, currentNamespace, genericParameters)?.let { return it }
        renderProjectedIndexOfInterfaceMethod(method, currentNamespace, genericParameters)?.let { return it }
        val functionName = kotlinMethodName(method)
        val builder = FunSpec.builder(functionName)
            .apply {
                if (method.name == "ToString" && method.returnType == "String" && method.parameters.isEmpty()) {
                    addModifiers(KModifier.OVERRIDE)
                }
            }
            .returns(typeNameMapper.mapTypeName(method.returnType, currentNamespace, genericParameters))
            .addParameters(method.parameters.map { parameter ->
                ParameterSpec.builder(
                    parameter.name.replaceFirstChar(Char::lowercase),
                    typeNameMapper.mapTypeName(parameter.type, currentNamespace, genericParameters),
                ).build()
            })
        return when {
            method.returnType == "String" && method.parameters.isEmpty() && method.vtableIndex != null -> {
                builder
                    .addStatement(
                        "val value = %T.invokeHStringMethod(pointer, %L).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        method.vtableIndex!!,
                    )
                    .beginControlFlow("return try")
                    .addStatement("value.toKotlinString()")
                    .nextControlFlow("finally")
                    .addStatement("value.close()")
                    .endControlFlow()
                    .build()
            }
            method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                builder
                    .addStatement(
                        "val value = %T.invokeHStringMethodWithStringArg(pointer, %L, %N).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        method.vtableIndex!!,
                        argumentName,
                    )
                    .beginControlFlow("return try")
                    .addStatement("value.toKotlinString()")
                    .nextControlFlow("finally")
                    .addStatement("value.close()")
                    .endControlFlow()
                    .build()
            }
            method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                builder
                    .addStatement(
                        "val value = %T.invokeHStringMethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        method.vtableIndex!!,
                        argumentName,
                    )
                    .beginControlFlow("return try")
                    .addStatement("value.toKotlinString()")
                    .nextControlFlow("finally")
                    .addStatement("value.close()")
                    .endControlFlow()
                    .build()
            }
            method.returnType == "Float64" && method.parameters.isEmpty() && method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeFloat64Method(pointer, %L).getOrThrow())",
                        PoetSymbols.float64Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            method.returnType == "Float64" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeFloat64MethodWithStringArg(pointer, %L, %N).getOrThrow())",
                        PoetSymbols.float64Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Float64" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeFloat64MethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow())",
                        PoetSymbols.float64Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Boolean" && method.parameters.isEmpty() && method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeBooleanGetter(pointer, %L).getOrThrow())",
                        PoetSymbols.winRtBooleanClass,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            method.returnType == "Boolean" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeBooleanMethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow())",
                        PoetSymbols.winRtBooleanClass,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Boolean" &&
                method.parameters.size == 1 &&
                supportsInterfaceObjectInput(method.parameters[0].type, currentNamespace) &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%L)",
                        PoetSymbols.winRtBooleanClass,
                        AbiCallCatalog.booleanMethodWithObject(
                            vtableIndex,
                            interfaceObjectArgumentExpression(argumentName, method.parameters[0].type, currentNamespace),
                        ),
                    )
                    .build()
            }
            method.returnType == "Boolean" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeBooleanMethodWithStringArg(pointer, %L, %N).getOrThrow())",
                        PoetSymbols.winRtBooleanClass,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Int32" && method.parameters.isEmpty() && method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeInt32Method(pointer, %L).getOrThrow())",
                        PoetSymbols.int32Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            typeRegistry.isEnumType(method.returnType, currentNamespace) &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
                val underlyingType = enumUnderlyingTypeOrDefault(typeRegistry, method.returnType, currentNamespace)
                builder
                    .addStatement(
                        "return %T.fromValue(%L)",
                        returnType,
                        enumGetterAbiCall(underlyingType, vtableIndex),
                    )
                    .build()
            }
            typeRegistry.isEnumType(method.returnType, currentNamespace) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
                val underlyingType = enumUnderlyingTypeOrDefault(typeRegistry, method.returnType, currentNamespace)
                builder
                    .addStatement(
                        "return %T.fromValue(%L)",
                        returnType,
                        enumMethodWithInt32ArgAbiCall(underlyingType, vtableIndex, argumentName),
                    )
                    .build()
            }
            supportsInterfaceObjectReturnType(method.returnType, currentNamespace) &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null -> {
                builder
                    .addStatement(
                        "return %L",
                        objectReturnCode(
                            method,
                            currentNamespace,
                            AbiCallCatalog.objectMethod(method.vtableIndex!!),
                            genericParameters,
                        ),
                    )
                    .build()
            }
            supportsInterfaceObjectReturnType(method.returnType, currentNamespace) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                builder
                    .addStatement(
                        "return %L",
                        objectReturnCode(
                            method,
                            currentNamespace,
                            AbiCallCatalog.objectMethodWithString(method.vtableIndex!!, argumentName),
                            genericParameters,
                        ),
                    )
                    .build()
            }
            supportsInterfaceObjectReturnType(method.returnType, currentNamespace) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                builder
                    .addStatement(
                        "return %L",
                        objectReturnCode(
                            method,
                            currentNamespace,
                            AbiCallCatalog.objectMethodWithUInt32(method.vtableIndex!!, "$argumentName.value"),
                            genericParameters,
                        ),
                    )
                    .build()
            }
            else -> {
                val methodPlan = plannedInterfaceMethod(method, currentNamespace, genericParameters)
                    ?: return null
                builder
                    .addStatement(methodPlan.statement, *methodPlan.args(method, currentNamespace))
                    .build()
            }
        }
    }

    private fun renderAsyncAwaitMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): FunSpec? {
        if (!isKotlinIdentifier(method.name) || !supportsInterfaceMethod(method, currentNamespace, genericParameters)) {
            return null
        }
        val asyncPlan = asyncMethodRuleRegistry.plan(method, currentNamespace, genericParameters) ?: return null
        val parameterSpecs = method.parameters.map { parameter ->
            ParameterSpec.builder(
                parameter.name.replaceFirstChar(Char::lowercase),
                typeNameMapper.mapTypeName(parameter.type, currentNamespace, genericParameters),
            ).build()
        }
        val invocation = buildString {
            append(kotlinMethodName(method))
            append('(')
            append(parameterSpecs.joinToString(", ") { it.name })
            append(')')
        }
        val builder = FunSpec.builder("${kotlinMethodName(method)}Await")
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

    private fun renderAsyncTaskMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): FunSpec? {
        val asyncPlan = asyncMethodRuleRegistry.plan(method, currentNamespace, genericParameters) ?: return null
        val functionName = kotlinMethodName(method)
        val builder = FunSpec.builder(functionName)
            .returns(asyncPlan.rawReturnType)
            .addParameters(method.parameters.map { parameter ->
                ParameterSpec.builder(
                    parameter.name.replaceFirstChar(Char::lowercase),
                    typeNameMapper.mapTypeName(parameter.type, currentNamespace, genericParameters),
                ).build()
            })
        asyncPlan.rawTaskCallFactory.create(asyncPlan.invocation)
            .let { plan -> builder.addStatement(plan.statementFormat, *plan.args) }
        return builder.build()
    }

    private fun renderLambdaOverload(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): FunSpec? {
        if (method.parameters.size != 1 || method.vtableIndex == null) {
            return null
        }
        val delegateType = typeRegistry.findType(method.parameters.single().type, currentNamespace) ?: return null
        if (delegateType.kind != dev.winrt.winmd.plugin.WinMdTypeKind.Delegate) {
            return null
        }
        val invokeMethod = delegateType.methods.singleOrNull { it.name == "Invoke" } ?: return null
        if (invokeMethod.returnType != "Unit" && invokeMethod.returnType != "Boolean") {
            return null
        }

        val functionName = kotlinMethodName(method)
        val delegateClass = typeNameMapper.mapTypeName(method.parameters.single().type, currentNamespace, genericParameters)
        val lambdaParameterName = "callback"
        val plan = delegateLambdaPlanResolver.resolve(
            invokeMethod = invokeMethod,
            currentNamespace = currentNamespace,
            genericParameters = genericParameters,
            supportsObjectType = { typeName -> supportsInterfaceObjectType(typeName, currentNamespace) },
        ) as? DelegateLambdaPlan.PlannedBridge ?: return null
        return FunSpec.builder(functionName)
            .returns(PoetSymbols.winRtDelegateHandleClass)
            .addParameter(lambdaParameterName, plan.lambdaType)
            .apply {
                val argumentKindsLiteral = if (plan.bridge.argumentKinds.isEmpty()) {
                    "emptyList()"
                } else {
                    plan.bridge.argumentKinds.joinToString(
                        prefix = "listOf(",
                        postfix = ")",
                    ) { kind -> "${PoetSymbols.winRtDelegateValueKindClass.canonicalName}.${kind.name}" }
                }
                val callbackInvocation = buildDelegateCallbackInvocation(plan, lambdaParameterName)
                addStatement(
                    "val delegateHandle = %T.%L(%T.iid, %L) { args -> %L }",
                    PoetSymbols.winRtDelegateBridgeClass,
                    plan.bridge.factoryMethod,
                    delegateClass,
                    argumentKindsLiteral,
                    callbackInvocation,
                )
                beginControlFlow("try")
                addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                nextControlFlow("catch (t: Throwable)")
                addStatement("delegateHandle.close()")
                addStatement("throw t")
                endControlFlow()
                addStatement("return delegateHandle")
            }
            .build()
    }

    private fun buildDelegateCallbackInvocation(
        plan: DelegateLambdaPlan.PlannedBridge,
        callbackName: String,
    ): CodeBlock {
        if (plan.lambdaType.parameters.isEmpty()) {
            return CodeBlock.of("%N()", callbackName)
        }
        val builder = CodeBlock.builder().add("%N(", callbackName)
        plan.lambdaType.parameters.indices.forEachIndexed { position, index ->
            if (position > 0) {
                builder.add(", ")
            }
            val parameterType = plan.lambdaType.parameters[index]
            when (plan.bridge.argumentKinds[index]) {
                DelegateArgumentKind.OBJECT -> builder.add("%L(args[%L] as %T)", parameterType, index, PoetSymbols.comPtrClass)
                else -> builder.add("args[%L] as %L", index, parameterType)
            }
        }
        return builder.add(")").build()
    }

    private fun kotlinMethodName(method: WinMdMethod): String = projectedMethodName(method)

    private fun kotlinMethodName(methodName: String): String =
        when (methodName) {
            "ToString" -> "toString"
            else -> methodName.replaceFirstChar(Char::lowercase)
        }

    private fun supportsInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): Boolean {
        return supportsProjectedIndexOfInterfaceMethod(method, currentNamespace) ||
            asyncMethodRuleRegistry.plan(method, currentNamespace, genericParameters) != null ||
            plannedInterfaceMethod(method, currentNamespace, genericParameters) != null ||
            (method.returnType == "String" &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            (method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null) ||
            (method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null) ||
            (method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null) ||
            (method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Boolean" &&
                method.vtableIndex != null) ||
            (method.returnType == "Int32" &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            (method.returnType == "UInt32" &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            (typeRegistry.isEnumType(method.returnType, currentNamespace) &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            (typeRegistry.isEnumType(method.returnType, currentNamespace) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Boolean" &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                supportsInterfaceObjectInput(method.parameters[0].type, currentNamespace) &&
                method.vtableIndex != null)
            || (method.returnType == "Int32" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null)
            || (method.returnType == "Int32" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null)
            || (method.returnType == "Int32" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null)
            || (method.returnType == "Int32" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Boolean" &&
                method.vtableIndex != null)
            || (method.returnType == "Int32" &&
                method.parameters.size == 1 &&
                supportsInterfaceObjectInput(method.parameters[0].type, currentNamespace) &&
                method.vtableIndex != null)
            || (method.returnType == "UInt32" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null)
            || (method.returnType == "UInt32" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null)
            || (method.returnType == "UInt32" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null)
            || (method.returnType == "UInt32" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Boolean" &&
                method.vtableIndex != null)
            || (method.returnType == "UInt32" &&
                method.parameters.size == 1 &&
                supportsInterfaceObjectInput(method.parameters[0].type, currentNamespace) &&
                method.vtableIndex != null)
    }

    private fun plannedInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        if (method.vtableIndex == null) {
            return null
        }
        if (method.isIndexOfOutUInt32Method()) {
            return null
        }
        plannedInt32FillArrayInterfaceMethod(method, currentNamespace, genericParameters)?.let { return it }
        plannedStandardReceiveArrayInterfaceMethod(method)?.let { return it }
        plannedStructReceiveArrayInterfaceMethod(method, currentNamespace, genericParameters)?.let { return it }
        plannedRuntimeClassReceiveArrayInterfaceMethod(method, currentNamespace, genericParameters)?.let { return it }
        plannedStructPassArrayInterfaceMethod(method, currentNamespace, genericParameters)?.let { return it }
        plannedStandardPassArrayInterfaceMethod(method, currentNamespace, genericParameters)?.let { return it }
        plannedValueTypeAwareInterfaceMethod(method, currentNamespace, genericParameters)?.let { return it }
        if (typeRegistry.isEnumType(method.returnType, currentNamespace)) {
            return null
        }
        val signatureKey = methodSignatureKey(
            returnType = method.returnType,
            parameterTypes = method.parameters.map { typeRegistry.signatureParameterType(it.type, currentNamespace) },
            supportsParameterObjectType = { typeName -> supportsInterfaceObjectInput(typeName, currentNamespace) },
            supportsReturnObjectType = { typeName -> supportsInterfaceObjectReturnType(typeName, currentNamespace) },
        ) ?: return null
        MethodRuleRegistry.sharedMethodPlan(signatureKey) ?: return null
        return plannedInterfaceMethodForKey(signatureKey, genericParameters)
    }

    private fun plannedInt32FillArrayInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        if (!method.isInt32FillArrayMethod()) {
            return null
        }
        val fillArrayParameter = method.int32FillArrayParameter() ?: return null
        val fillArrayBufferName = int32FillArrayBufferName(fillArrayParameter)
        val abiArguments = int32FillArrayAbiArguments(
            parameters = method.parameters,
            lowerNonArrayArgument = { parameter ->
                lowerInterfaceArrayMethodArgument(parameter, currentNamespace) ?: return@int32FillArrayAbiArguments null
            },
            lowerArrayArgument = { _ ->
                CodeBlock.of("%N", fillArrayBufferName)
            },
        ) ?: return null
        return when {
            method.returnType == "Unit" -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    arrayOf(
                        int32FillArrayWrappedCall(
                            fillArrayParameter,
                            interfaceVarargAbiCall("invokeUnitMethodWithArgs", method.vtableIndex!!, abiArguments),
                            returnsValue = false,
                        ),
                    )
                },
            )
            supportsInterfaceObjectReturnType(method.returnType, currentNamespace) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, _ ->
                    arrayOf(
                        int32FillArrayWrappedCall(
                            fillArrayParameter,
                            objectReturnCode(
                                method = method,
                                namespace = currentNamespace,
                                abiCall = interfaceVarargAbiCall("invokeObjectMethodWithArgs", method.vtableIndex!!, abiArguments),
                                genericParameters = genericParameters,
                            ),
                            returnsValue = true,
                        ),
                    )
                },
            )
            supportsFillArrayResultKind(method.returnType) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, _ ->
                    arrayOf(
                        int32FillArrayWrappedCall(
                            fillArrayParameter,
                            twoArgumentReturnCode(
                                method.returnType,
                                interfaceVarargResultKindAbiCall(
                                    vtableIndex = method.vtableIndex!!,
                                    returnType = method.returnType,
                                    abiArguments = abiArguments,
                                ),
                            ),
                            returnsValue = true,
                        ),
                    )
                },
            )
            else -> null
        }
    }

    private fun plannedStandardReceiveArrayInterfaceMethod(method: WinMdMethod): PlannedInterfaceMethod? =
        standardReceiveArrayMethodDescriptors.firstNotNullOfOrNull { descriptor ->
            plannedReceiveArrayInterfaceMethod(method, descriptor)
        }

    private fun plannedReceiveArrayInterfaceMethod(
        method: WinMdMethod,
        descriptor: ReceiveArrayMethodDescriptor,
    ): PlannedInterfaceMethod? {
        if (!descriptor.matches(method)) {
            return null
        }
        return PlannedInterfaceMethod(
            statement = "return %L",
            args = { currentMethod, currentNamespace ->
                val abiArguments = descriptor.abiArguments(currentMethod.parameters) { parameter ->
                    lowerInterfaceArrayMethodArgument(parameter, currentNamespace)
                } ?: error("Unsupported ${descriptor.label} receive-array interface method: ${currentMethod.name}")
                arrayOf(descriptor.returnExpression(currentMethod.vtableIndex!!, abiArguments))
            },
        )
    }

    private fun plannedStandardPassArrayInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? =
        standardPassArrayMethodDescriptors.firstNotNullOfOrNull { descriptor ->
            plannedPassArrayInterfaceMethod(method, currentNamespace, genericParameters, descriptor)
        }

    private fun plannedPassArrayInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
        descriptor: PassArrayMethodDescriptor,
    ): PlannedInterfaceMethod? {
        if (!descriptor.matches(method) { typeName -> supportsInterfaceObjectReturnType(typeName, currentNamespace) }) {
            return null
        }
        val abiArguments = descriptor.abiArguments(method.parameters) { parameter ->
            lowerInterfaceArrayMethodArgument(parameter, currentNamespace)
        } ?: return null
        return if (method.returnType == "Unit") {
            PlannedInterfaceMethod(
                statement = "%L",
                args = { currentMethod, _ ->
                    arrayOf(interfaceVarargAbiCall("invokeUnitMethodWithArgs", currentMethod.vtableIndex!!, abiArguments))
                },
            )
        } else {
            PlannedInterfaceMethod(
                statement = "return %L",
                args = { currentMethod, _ ->
                    arrayOf(
                        objectReturnCode(
                            method = currentMethod,
                            namespace = currentNamespace,
                            abiCall = interfaceVarargAbiCall("invokeObjectMethodWithArgs", currentMethod.vtableIndex!!, abiArguments),
                            genericParameters = genericParameters,
                        ),
                    )
                },
            )
        }
    }

    private fun plannedRuntimeClassReceiveArrayInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        val elementType = method.runtimeClassReceiveArrayElementType(currentNamespace, typeRegistry) ?: return null
        return PlannedInterfaceMethod(
            statement = "return %L",
            args = { method, namespace ->
                val abiArguments = runtimeClassReceiveArrayAbiArguments(
                    parameters = method.parameters,
                    currentNamespace = namespace,
                    typeRegistry = typeRegistry,
                    expectedElementType = elementType,
                ) { parameter ->
                    lowerInterfaceArrayMethodArgument(parameter, namespace) ?: return@runtimeClassReceiveArrayAbiArguments null
                } ?: error("Unsupported runtime-class receive-array interface method: ${method.name}")
                val runtimeClassType = typeNameMapper.mapTypeName(elementType, namespace, genericParameters)
                arrayOf(runtimeClassReceiveArrayReturnExpression(method.vtableIndex!!, runtimeClassType, abiArguments))
            },
        )
    }

    private fun plannedStructReceiveArrayInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        val elementType = method.supportedStructReceiveArrayElementType(currentNamespace, typeRegistry) ?: return null
        return PlannedInterfaceMethod(
            statement = "return %L",
            args = { method, namespace ->
                val abiArguments = structReceiveArrayAbiArguments(
                    parameters = method.parameters,
                    currentNamespace = namespace,
                    typeRegistry = typeRegistry,
                    expectedElementType = elementType,
                ) { parameter ->
                    lowerInterfaceArrayMethodArgument(parameter, namespace) ?: return@structReceiveArrayAbiArguments null
                } ?: error("Unsupported struct receive-array interface method: ${method.name}")
                val structType = typeNameMapper.mapTypeName(elementType, namespace, genericParameters)
                arrayOf(structReceiveArrayReturnExpression(method.vtableIndex!!, structType, abiArguments))
            },
        )
    }

    private fun plannedStructPassArrayInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        val elementType = method.supportedStructPassArrayElementType(currentNamespace, typeRegistry) ?: return null
        if (method.returnType != "Unit" && !supportsInterfaceObjectReturnType(method.returnType, currentNamespace)) {
            return null
        }
        val abiArguments = structPassArrayAbiArguments(
            parameters = method.parameters,
            currentNamespace = currentNamespace,
            typeRegistry = typeRegistry,
            expectedElementType = elementType,
        ) { parameter ->
            lowerInterfaceArrayMethodArgument(parameter, currentNamespace) ?: return@structPassArrayAbiArguments null
        } ?: return null
        return if (method.returnType == "Unit") {
            PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ -> arrayOf(interfaceVarargAbiCall("invokeUnitMethodWithArgs", method.vtableIndex!!, abiArguments)) },
            )
        } else {
            PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, _ ->
                    arrayOf(
                        objectReturnCode(
                            method = method,
                            namespace = currentNamespace,
                            abiCall = interfaceVarargAbiCall("invokeObjectMethodWithArgs", method.vtableIndex!!, abiArguments),
                            genericParameters = genericParameters,
                        ),
                    )
                },
            )
        }
    }

    private fun plannedValueTypeAwareInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        val argumentExpressions = method.parameters.map { parameter ->
            valueTypeProjectionSupport.lowerGenericAbiArgument(
                type = parameter.type,
                currentNamespace = currentNamespace,
                argumentName = parameter.name.replaceFirstChar(Char::lowercase),
                supportsObjectType = { typeName -> supportsInterfaceObjectInput(typeName, currentNamespace) },
                lowerObjectArgument = { argumentName, typeName ->
                    interfaceObjectArgumentExpression(argumentName, typeName, currentNamespace)
                },
            ) ?: return null
        }
        return when (
            valueTypeProjectionSupport.methodPlanKind(
                returnType = method.returnType,
                parameterTypes = method.parameters.map(WinMdParameter::type),
                currentNamespace = currentNamespace,
                supportsObjectReturnType = { typeName -> supportsInterfaceObjectReturnType(typeName, currentNamespace) },
            ) ?: return null
        ) {
            ValueAwareMethodPlanKind.SMALL_SCALAR -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, _ ->
                    arrayOf(
                        valueTypeProjectionSupport.smallScalarReturnExpression(
                            method.returnType,
                            valueTypeProjectionSupport.smallScalarAbiCall(
                                type = method.returnType,
                                vtableIndex = method.vtableIndex!!,
                                arguments = argumentExpressions,
                            ) ?: error("Unsupported small scalar projection type: ${method.returnType}"),
                        ) ?: error("Unsupported small scalar projection type: ${method.returnType}"),
                    )
                },
            )
            ValueAwareMethodPlanKind.STRUCT -> {
                val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace, genericParameters)
                PlannedInterfaceMethod(
                    statement = "return %T.fromAbi(%L)",
                    args = { method, _ ->
                        arrayOf(
                            returnType,
                            valueTypeProjectionSupport.invokeStructMethodWithArgs(
                                vtableIndex = method.vtableIndex!!,
                                structType = returnType,
                                arguments = argumentExpressions,
                            ),
                        )
                    },
                )
            }
            ValueAwareMethodPlanKind.IREFERENCE_VALUE -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, _ ->
                    arrayOf(
                        valueTypeProjectionSupport.nullableValueReturnExpression(
                            referenceType = method.returnType,
                            currentNamespace = currentNamespace,
                            abiCall = valueTypeProjectionSupport.invokeObjectMethodWithArgs(
                                vtableIndex = method.vtableIndex!!,
                                arguments = argumentExpressions,
                            ),
                        ) ?: error("Unsupported IReference projection type: ${method.returnType}"),
                    )
                },
            )
            ValueAwareMethodPlanKind.IREFERENCE_GENERIC_STRUCT -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, _ ->
                    arrayOf(
                        valueTypeProjectionSupport.genericStructReferenceReturnExpression(
                            referenceType = method.returnType,
                            currentNamespace = currentNamespace,
                            abiCall = valueTypeProjectionSupport.invokeObjectMethodWithArgs(
                                vtableIndex = method.vtableIndex!!,
                                arguments = argumentExpressions,
                            ),
                        ) ?: error("Unsupported IReference projection type: ${method.returnType}"),
                    )
                },
            )
            ValueAwareMethodPlanKind.IREFERENCE_GENERIC_ENUM -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, _ ->
                    arrayOf(
                        valueTypeProjectionSupport.genericEnumReferenceReturnExpression(
                            referenceType = method.returnType,
                            currentNamespace = currentNamespace,
                            abiCall = valueTypeProjectionSupport.invokeObjectMethodWithArgs(
                                vtableIndex = method.vtableIndex!!,
                                arguments = argumentExpressions,
                            ),
                        ) ?: error("Unsupported IReference projection type: ${method.returnType}"),
                    )
                },
            )
            ValueAwareMethodPlanKind.UNIT -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    arrayOf(
                        valueTypeProjectionSupport.invokeUnitMethodWithArgs(
                            vtableIndex = method.vtableIndex!!,
                            arguments = argumentExpressions,
                        ),
                    )
                },
            )
            ValueAwareMethodPlanKind.OBJECT_RETURN -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, _ ->
                    arrayOf(
                        objectReturnCode(
                            method = method,
                            namespace = currentNamespace,
                            abiCall = valueTypeProjectionSupport.invokeObjectMethodWithArgs(
                                vtableIndex = method.vtableIndex!!,
                                arguments = argumentExpressions,
                            ),
                            genericParameters = genericParameters,
                        ),
                    )
                },
            )
        }
    }

    private fun plannedInterfaceMethodForKey(
        signatureKey: MethodSignatureKey,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        val parameterCategories = signatureKey.shape.toParameterCategories() ?: return null
        return when {
            parameterCategories.size <= 1 -> plannedUnaryInterfaceMethod(signatureKey, genericParameters)
            signatureKey.returnKind == MethodReturnKind.UNIT -> plannedTwoArgumentUnitInterfaceMethod(signatureKey)
            parameterCategories.size == 2 -> plannedTwoArgumentReturnMethod(signatureKey, genericParameters)
            else -> null
        }
    }

    private fun plannedTwoArgumentUnitInterfaceMethod(
        signatureKey: MethodSignatureKey,
    ): PlannedInterfaceMethod? {
        val parameterCategories = signatureKey.shape.toParameterCategories()
            ?.takeIf(List<MethodParameterCategory>::isSupportedTwoArgumentUnitCategories)
            ?: return null
        return PlannedInterfaceMethod(
            statement = "%L",
            args = { method, namespace ->
                val argumentExpressions = abiArgumentExpressions(
                    parameters = method.parameters,
                    parameterCategories = parameterCategories,
                    argumentName = { parameter -> parameter.name.replaceFirstChar(Char::lowercase) },
                    parameterType = WinMdParameter::type,
                    lowerObjectArgument = { parameter ->
                        interfaceObjectArgumentExpression(
                            parameter.name.replaceFirstChar(Char::lowercase),
                            parameter.type,
                            namespace,
                        )
                    },
                )
                arrayOf(
                    AbiCallCatalog.unitMethodWithTwoArguments(
                        method.vtableIndex!!,
                        parameterCategories,
                        argumentExpressions[0],
                        argumentExpressions[1],
                    ),
                )
            },
        )
    }

    private fun plannedUnaryInterfaceMethod(
        signatureKey: MethodSignatureKey,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        val parameterCategories = signatureKey.shape.toParameterCategories() ?: return null
        if (parameterCategories.size > 1) return null
        val parameterCategory = parameterCategories.singleOrNull()
        return when (signatureKey.returnKind) {
            MethodReturnKind.STRING,
            MethodReturnKind.FLOAT32,
            MethodReturnKind.FLOAT64,
            MethodReturnKind.DATE_TIME,
            MethodReturnKind.TIME_SPAN,
            MethodReturnKind.BOOLEAN,
            MethodReturnKind.INT32,
            MethodReturnKind.UINT32,
            MethodReturnKind.INT64,
            MethodReturnKind.UINT64,
            MethodReturnKind.EVENT_REGISTRATION_TOKEN,
            MethodReturnKind.GUID,
            MethodReturnKind.OBJECT,
            MethodReturnKind.UNIT -> plannedUnaryInterfaceMethodForReturnKind(
                returnKind = signatureKey.returnKind,
                parameterCategory = parameterCategory,
                genericParameters = genericParameters,
            )
        }
    }

    private fun plannedUnaryInterfaceMethodForReturnKind(
        returnKind: MethodReturnKind,
        parameterCategory: MethodParameterCategory?,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod = PlannedInterfaceMethod(
        statement = unaryInterfaceStatement(returnKind),
        args = { method, namespace ->
            val parameter = method.parameters.singleOrNull()
            val argumentName = parameter?.name?.replaceFirstChar(Char::lowercase)
            val abiCall = unaryInterfaceAbiCall(
                vtableIndex = method.vtableIndex!!,
                returnKind = returnKind,
                parameterCategory = parameterCategory,
                parameterType = parameter?.type,
                currentNamespace = namespace,
                argumentName = argumentName,
            )
            unaryInterfaceArgs(method, namespace, returnKind, abiCall, genericParameters)
        },
    )

    private fun unaryInterfaceStatement(returnKind: MethodReturnKind): String =
        if (returnKind == MethodReturnKind.UNIT) "%L" else "return %L"

    private fun unaryInterfaceArgs(
        method: WinMdMethod,
        namespace: String,
        returnKind: MethodReturnKind,
        abiCall: CodeBlock,
        genericParameters: Set<String>,
    ): Array<Any> = when (returnKind) {
        MethodReturnKind.OBJECT -> arrayOf(objectReturnCode(method, namespace, abiCall, genericParameters))
        MethodReturnKind.UNIT -> arrayOf(abiCall)
        else -> arrayOf(twoArgumentReturnCode(method.returnType, abiCall))
    }

    private fun unaryInterfaceAbiCall(
        vtableIndex: Int,
        returnKind: MethodReturnKind,
        parameterCategory: MethodParameterCategory?,
        parameterType: String?,
        currentNamespace: String,
        argumentName: String?,
    ): CodeBlock {
        if (parameterCategory == null) {
            return zeroArgumentUnaryAbiCall(vtableIndex, returnKind)
        }
        val argument = requireNotNull(argumentName)
        val type = requireNotNull(parameterType)
        val loweredArgument = abiArgumentExpression(
            argumentName = argument,
            parameterType = type,
            category = parameterCategory,
        ) { interfaceObjectArgumentExpression(argument, type, currentNamespace) }
        return defaultUnaryAbiCall(
            vtableIndex = vtableIndex,
            returnKind = returnKind,
            parameterCategory = parameterCategory,
            argumentName = argument,
            loweredArgument = loweredArgument,
            unsupportedMessage = "Unsupported unary interface return kind: $returnKind",
        )
    }

    private fun lowerInterfaceArrayMethodArgument(
        parameter: WinMdParameter,
        currentNamespace: String,
    ): CodeBlock? {
        valueTypeProjectionSupport.lowerGenericAbiArgument(
            type = parameter.type,
            currentNamespace = currentNamespace,
            argumentName = parameter.name.replaceFirstChar(Char::lowercase),
            supportsObjectType = { typeName -> supportsInterfaceObjectInput(typeName, currentNamespace) },
            lowerObjectArgument = { argumentName, typeName ->
                interfaceObjectArgumentExpression(argumentName, typeName, currentNamespace)
            },
        )?.let { return CodeBlock.of("%L", it) }
        val parameterCategory = methodParameterCategory(
            typeRegistry.signatureParameterType(parameter.type, currentNamespace),
        ) { typeName -> supportsInterfaceObjectInput(typeName, currentNamespace) } ?: return null
        return CodeBlock.of(
            "%L",
            abiArgumentExpression(
                argumentName = parameter.name.replaceFirstChar(Char::lowercase),
                parameterType = parameter.type,
                category = parameterCategory,
            ) {
                interfaceObjectArgumentExpression(
                    parameter.name.replaceFirstChar(Char::lowercase),
                    parameter.type,
                    currentNamespace,
                )
            },
        )
    }

    private fun plannedTwoArgumentReturnMethod(
        signatureKey: MethodSignatureKey,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod = PlannedInterfaceMethod(
        statement = "return %L",
        args = { method, namespace ->
            val parameterCategories = methodParameterCategories(
                method.parameters.map { parameter -> typeRegistry.signatureParameterType(parameter.type, namespace) },
                { typeName -> supportsInterfaceObjectInput(typeName, namespace) },
            ) ?: error("Unsupported two-argument return shape: ${signatureKey.shape}")
            val argumentExpressions = abiArgumentExpressions(
                parameters = method.parameters,
                parameterCategories = parameterCategories,
                argumentName = { parameter -> parameter.name.replaceFirstChar(Char::lowercase) },
                parameterType = WinMdParameter::type,
                lowerObjectArgument = { parameter ->
                    interfaceObjectArgumentExpression(
                        parameter.name.replaceFirstChar(Char::lowercase),
                        parameter.type,
                        namespace,
                    )
                },
            )
            val abiCall = AbiCallCatalog.resultMethodWithTwoArguments(
                method.vtableIndex!!,
                if (signatureKey.returnKind == MethodReturnKind.OBJECT) "OBJECT" else resultKindName(method.returnType),
                if (signatureKey.returnKind == MethodReturnKind.OBJECT) PoetSymbols.requireObjectMember else resultExtractor(method.returnType),
                parameterCategories,
                argumentExpressions[0],
                argumentExpressions[1],
            )
            arrayOf(
                if (signatureKey.returnKind == MethodReturnKind.OBJECT) {
                    objectReturnCode(method, namespace, abiCall, genericParameters)
                } else {
                    twoArgumentReturnCode(method.returnType, abiCall)
                },
            )
        },
    )

    private fun interfaceVarargAbiCall(
        methodName: String,
        vtableIndex: Int,
        abiArguments: List<CodeBlock>,
    ): CodeBlock {
        return CodeBlock.builder()
            .add("%T.%L(pointer, %L", PoetSymbols.platformComInteropClass, methodName, vtableIndex)
            .apply {
                abiArguments.forEach { argument ->
                    add(", %L", argument)
                }
            }
            .add(").getOrThrow()")
            .build()
    }

    private fun interfaceVarargResultKindAbiCall(
        vtableIndex: Int,
        returnType: String,
        abiArguments: List<CodeBlock>,
    ): CodeBlock {
        return CodeBlock.builder()
            .add(
                "%T.invokeMethodWithResultKind(pointer, %L, %T.%L",
                PoetSymbols.platformComInteropClass,
                vtableIndex,
                PoetSymbols.comMethodResultKindClass,
                resultKindName(returnType),
            )
            .apply {
                abiArguments.forEach { argument ->
                    add(", %L", argument)
                }
            }
            .add(").getOrThrow().%M()", resultExtractor(returnType))
            .build()
    }

    private data class PlannedInterfaceMethod(
        val statement: String,
        val args: (WinMdMethod, String) -> Array<Any>,
    )

    private fun supportsInterfaceObjectInput(type: String, currentNamespace: String): Boolean {
        return projectedObjectArgumentLowering.supportsInputType(type, currentNamespace)
    }

    private fun supportsInterfaceObjectType(type: String, currentNamespace: String): Boolean {
        return supportsInterfaceObjectInput(type, currentNamespace)
    }

    private fun supportsInterfaceObjectReturnType(type: String, currentNamespace: String): Boolean {
        return !typeRegistry.isStructType(type, currentNamespace) &&
            !supportsIReferenceValueProjection(type, currentNamespace, typeRegistry) &&
            (
                supportsProjectedObjectTypeName(type) ||
                    typeRegistry.supportsClosedGenericInterfaceReturnType(type, currentNamespace, winRtSignatureMapper)
                )
    }

    private fun supportsInterfaceProperty(
        property: WinMdProperty,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): Boolean {
        return property.getterVtableIndex != null &&
            (
                valueTypeProjectionSupport.propertyProjection(property.type, currentNamespace) != null ||
                    PropertyRuleRegistry.interfaceGetterRuleFamily(
                        type = property.type,
                        isEnumType = typeRegistry.isEnumType(property.type, currentNamespace),
                        isObjectType = supportsInterfaceObjectReturnType(property.type, currentNamespace),
                    ) != null
                )
    }

    private fun collectionSuperinterface(
        baseInterface: String,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): TypeName? {
        val mapped = typeNameMapper.mapTypeName(baseInterface, currentNamespace, genericParameters)
        return if (mapped.toString().startsWith("kotlin.collections.")) mapped else null
    }

    private fun TypeSpec.Builder.addCollectionSuperinterfaces(
        baseInterfaces: List<String>,
        currentNamespace: String,
        genericParameters: Set<String>,
        projection: InterfaceCollectionProjection?,
        excludedBaseInterface: String? = null,
        preferProjectionDelegate: Boolean = false,
    ) {
        val projectionKey = projection?.superinterface?.toString()
        val inheritedCollections = baseInterfaces.asSequence()
            .filterNot { it == excludedBaseInterface }
            .mapNotNull { baseInterface ->
                collectionSuperinterface(baseInterface, currentNamespace, genericParameters)
            }
            .distinctBy { typeName -> typeName.toString() }
            .filterNot { typeName ->
                preferProjectionDelegate && typeName.toString() == projectionKey
            }
            .toList()
        inheritedCollections.forEach(::addSuperinterface)
        projection?.let {
            if (preferProjectionDelegate && it.delegateFactory != null) {
                addSuperinterface(it.superinterface, it.delegateFactory)
            } else if (inheritedCollections.none { inherited -> inherited.toString() == projectionKey }) {
                addSuperinterface(it.superinterface)
            }
        }
    }

    private fun directBaseInterface(type: WinMdType, currentNamespace: String): String? {
        return type.baseInterfaces.firstOrNull { baseInterface ->
            collectionSuperinterface(baseInterface, currentNamespace, type.genericParameters.toSet()) == null &&
                typeRegistry.findType(baseInterface, currentNamespace)?.kind == WinMdTypeKind.Interface
        }
    }

    private fun projectedInterfaceTypeName(
        typeName: String,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): TypeName {
        val normalizedTypeName = stripValueTypeNameMarker(typeName)
        if ('<' !in normalizedTypeName || !normalizedTypeName.endsWith(">")) {
            val interfaceType = typeRegistry.findType(normalizedTypeName, currentNamespace)
            return if (interfaceType?.kind == WinMdTypeKind.Interface) {
                projectedDeclarationClassName(interfaceType.namespace, interfaceType.name)
            } else {
                typeNameMapper.mapTypeName(typeName, currentNamespace, genericParameters)
            }
        }

        val rawTypeName = normalizedTypeName.substringBefore('<')
        val interfaceType = typeRegistry.findType(rawTypeName, currentNamespace)
        if (interfaceType?.kind != WinMdTypeKind.Interface) {
            return typeNameMapper.mapTypeName(typeName, currentNamespace, genericParameters)
        }

        val argumentSource = normalizedTypeName.substringAfter('<').substringBeforeLast('>')
        val arguments = splitGenericArguments(argumentSource)
            .map { argument -> typeNameMapper.mapTypeName(argument, currentNamespace, genericParameters) }
        return projectedDeclarationClassName(interfaceType.namespace, interfaceType.name)
            .parameterizedBy(arguments)
    }

    private fun projectedObjectReturnCode(
        typeName: String,
        currentNamespace: String,
        abiCall: CodeBlock,
        genericParameters: Set<String>,
    ): CodeBlock {
        typeRegistry.closedGenericInterfaceProjectionCall(
            typeName = typeName,
            currentNamespace = currentNamespace,
            abiCall = abiCall,
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { return it }
        val mappedType = typeNameMapper.mapTypeName(typeName, currentNamespace, genericParameters)
        return if (typeRegistry.findType(typeName, currentNamespace)?.kind == dev.winrt.winmd.plugin.WinMdTypeKind.Interface) {
            CodeBlock.of("%T.from(%T(%L))", mappedType, PoetSymbols.inspectableClass, abiCall)
        } else {
            CodeBlock.of("%T(%L)", mappedType, abiCall)
        }
    }

    private fun objectReturnCode(
        method: WinMdMethod,
        namespace: String,
        abiCall: CodeBlock,
        genericParameters: Set<String>,
    ): CodeBlock = projectedObjectReturnCode(method.returnType, namespace, abiCall, genericParameters)

    private fun supportsClosedGenericInterfaceInputType(typeName: String, currentNamespace: String): Boolean {
        if (!typeRegistry.supportsClosedGenericInterfaceReturnType(typeName, currentNamespace, winRtSignatureMapper)) {
            return false
        }
        return closedGenericRawProjectionTypeKey(typeName, currentNamespace) != null
    }

    private fun interfaceObjectArgumentExpression(
        argumentName: String,
        typeName: String,
        currentNamespace: String,
    ): CodeBlock {
        return projectedObjectArgumentLowering.expression(argumentName, typeName, currentNamespace)
    }

    private fun closedGenericRawProjectionTypeKey(typeName: String, currentNamespace: String): String? {
        return winRtProjectionTypeMapper.projectionTypeKeyFor(typeName, currentNamespace)
            .takeIf { '<' in it && it.endsWith(">") }
            ?.substringBefore('<')
    }

    private fun inheritedSignatureKeys(baseInterface: String?): Set<String> {
        val interfaceType = baseInterface?.let { typeRegistry.findType(it) } ?: return emptySet()
        return allInterfaceMethods(interfaceType).mapTo(linkedSetOf(), ::interfaceMethodRenderKey)
    }

    private fun inheritedPropertyNames(baseInterface: String?): Set<String> {
        val interfaceType = baseInterface?.let { typeRegistry.findType(it) } ?: return emptySet()
        return allInterfaceProperties(interfaceType).mapTo(linkedSetOf(), WinMdProperty::name)
    }

    private fun renderDispatchQueueOverride(
        type: WinMdType,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): FunSpec? {
        if (!isDispatchQueueType(type)) return null
        val tryEnqueue = type.methods.singleOrNull { method ->
            method.name == "TryEnqueue" &&
                method.parameters.size == 1 &&
                method.parameters.single().type == "${type.namespace}.DispatcherQueueHandler" &&
                method.returnType == "Windows.Foundation.WinRtBoolean"
        } ?: return null
        val handlerType = typeNameMapper.mapTypeName("${type.namespace}.DispatcherQueueHandler", currentNamespace, genericParameters)
        return FunSpec.builder("dispatch")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("block", LambdaTypeName.get(returnType = Unit::class.asTypeName()))
            .returns(Boolean::class)
            .addStatement("return %N(%T(block)).value", kotlinMethodName(tryEnqueue.name), handlerType)
            .build()
    }

    private fun isDispatchQueueType(type: WinMdType): Boolean {
        return (type.namespace == "Microsoft.UI.Dispatching" || type.namespace == "Windows.System") &&
            (type.name == "IDispatcherQueue" || type.name == "DispatcherQueue")
    }

    private data class InterfaceEventSlotPlan(
        val propertyName: String,
        val typeName: String,
        val delegateType: TypeName,
        val lambdaType: LambdaTypeName,
        val delegateGuid: String,
        val lambdaArgumentKindsLiteral: String,
        val lambdaCallbackInvocation: CodeBlock,
        val addVtableIndex: Int,
        val removeVtableIndex: Int,
    )

    private fun dedupeInterfaceProperties(properties: List<WinMdProperty>): List<WinMdProperty> {
        return properties.asReversed().distinctBy { it.name }.asReversed()
    }

    private fun dedupeInterfaceMethods(methods: List<WinMdMethod>): List<WinMdMethod> {
        return methods.asReversed().distinctBy(::interfaceMethodRenderKey).asReversed()
    }

    private fun interfaceMethodRenderKey(method: WinMdMethod): String {
        return method.overloadKey(renderedName = kotlinMethodName(method))
    }

}
