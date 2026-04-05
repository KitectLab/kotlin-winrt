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
        val inheritedSignatureKeys = inheritedSignatureKeys(directBaseInterface)
        val inheritedPropertyNames = inheritedPropertyNames(directBaseInterface)
        val projectedProperties = declaredAndSyntheticInterfaceProperties(type, inheritedPropertyNames)
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
                directBaseInterface?.let { typeNameMapper.mapTypeName(it, type.namespace, genericParameters) }
                    ?: PoetSymbols.winRtInterfaceProjectionClass,
            )
            .addSuperclassConstructorParameter("pointer")
            .apply {
                type.baseInterfaces.mapNotNull { baseInterface ->
                    collectionSuperinterface(baseInterface, type.namespace, genericParameters)
                }.forEach { addSuperinterface(it) }
                kotlinCollectionProjectionMapper.interfaceProjection(type)?.let { projection ->
                    if (projection.delegateFactory != null) {
                        addSuperinterface(projection.superinterface, projection.delegateFactory)
                    } else {
                        addSuperinterface(projection.superinterface)
                    }
                    projection.extraProperties.forEach(::addProperty)
                    projection.extraFunctions.forEach(::addFunction)
                    addProperty(kotlinCollectionProjectionMapper.buildWinRtSizeProperty(projection.winRtSizeSlot))
                }
                if (type.namespace == "Microsoft.UI.Xaml.Interop" && type.name == "IBindableIterable") {
                    addSuperinterface(PoetSymbols.iterableClass.parameterizedBy(PoetSymbols.inspectableClass))
                    addFunction(
                        FunSpec.builder("iterator")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(PoetSymbols.iteratorClass.parameterizedBy(PoetSymbols.inspectableClass))
                            .addStatement("return first()")
                            .build(),
                    )
                }
                if (type.namespace == "Microsoft.UI.Xaml.Interop" && type.name == "IBindableIterator") {
                    addSuperinterface(PoetSymbols.iteratorClass.parameterizedBy(PoetSymbols.inspectableClass))
                    addProperty(
                        PropertySpec.builder("winRtCurrent", PoetSymbols.inspectableClass)
                            .getter(
                                FunSpec.getterBuilder()
                                    .addStatement("return %T(%T.invokeObjectMethod(pointer, 6).getOrThrow())", PoetSymbols.inspectableClass, PoetSymbols.platformComInteropClass)
                                    .build(),
                            )
                            .build(),
                    )
                    addProperty(
                        PropertySpec.builder("winRtHasCurrent", PoetSymbols.winRtBooleanClass)
                            .getter(
                                FunSpec.getterBuilder()
                                    .addStatement("return %M(%L)", PoetSymbols.winRtBooleanMember, AbiCallCatalog.booleanGetter(7))
                                    .build(),
                            )
                            .build(),
                    )
                    addFunction(
                        FunSpec.builder("hasNext")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(Boolean::class)
                            .addStatement("return winRtHasCurrent.value")
                            .build(),
                    )
                    addFunction(
                        FunSpec.builder("next")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(PoetSymbols.inspectableClass)
                            .beginControlFlow("if (!hasNext())")
                            .addStatement("throw %T()", NoSuchElementException::class)
                            .endControlFlow()
                            .addStatement("val current = winRtCurrent")
                            .addStatement("moveNext()")
                            .addStatement("return current")
                            .build(),
                    )
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
                type.methods
                    .filterNot { interfaceMethodRenderKey(it) in inheritedSignatureKeys }
                    .filterNot { it.isProjectedPropertyAccessor(projectedPropertyNames) }
                    .flatMap { renderMethods(it, type.namespace, genericParameters) },
            )
            .addFunctions(
                type.methods
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
                            addFunction(renderGenericFrom(type, typeClass, typeVariables))
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
        val inheritedSignatureKeys = inheritedSignatureKeys(directBaseInterface)
        val inheritedPropertyNames = inheritedPropertyNames(directBaseInterface)
        val projectedProperties = declaredAndSyntheticInterfaceProperties(type, inheritedPropertyNames)
        val projectedPropertyNames = projectedProperties.asSequence()
            .mapNotNull { property ->
                property.name.takeIf { renderInterfaceContractProperty(property, type.namespace, genericParameters) != null }
            }
            .toSet()
        return TypeSpec.interfaceBuilder(declarationName)
            .apply {
                if (typeRegistry.isVersionedRuntimeClassInterface(type.name, type.namespace)) {
                    addModifiers(KModifier.INTERNAL)
                }
                typeVariables.forEach(::addTypeVariable)
                directBaseInterface?.let {
                    addSuperinterface(typeNameMapper.mapTypeName(it, type.namespace, genericParameters))
                }
                type.baseInterfaces
                    .filterNot { it == directBaseInterface }
                    .mapNotNull { baseInterface ->
                        collectionSuperinterface(baseInterface, type.namespace, genericParameters)
                    }
                    .forEach(::addSuperinterface)
                renderDispatchQueueOverride(type, type.namespace, genericParameters)?.let { dispatchOverride ->
                    addSuperinterface(PoetSymbols.dispatchQueueClass)
                    addFunction(dispatchOverride)
                }
            }
            .addProperties(
                projectedProperties.mapNotNull { renderInterfaceContractProperty(it, type.namespace, genericParameters) },
            )
            .addFunctions(
                type.methods
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
            .addSuperinterface(typeClass)
            .addSuperclassConstructorParameter("pointer")
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
                    addFunction(renderGenericFrom(type, typeClass, typeVariables))
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
        return FunSpec.builder(kotlinMethodName(method.name))
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

    private fun allInterfaceMethods(type: WinMdType): List<WinMdMethod> {
        val directBaseInterface = directBaseInterface(type, type.namespace)
        val inherited = directBaseInterface?.let { typeRegistry.findType(it, type.namespace) }?.let(::allInterfaceMethods).orEmpty()
        return (inherited + type.methods).distinctBy(::interfaceMethodRenderKey)
    }

    private fun allInterfaceProperties(type: WinMdType): List<WinMdProperty> {
        val directBaseInterface = directBaseInterface(type, type.namespace)
        val inherited = directBaseInterface?.let { typeRegistry.findType(it, type.namespace) }?.let(::allInterfaceProperties).orEmpty()
        return (inherited + declaredAndSyntheticInterfaceProperties(type)).distinctBy { it.name }
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
        val methodsByName = type.methods.associateBy { it.name }
        return type.methods.mapNotNull { addMethod ->
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
        return PropertySpec.builder("${kotlinMethodName(method.name)}ResultType", PoetSymbols.asyncResultTypeClass.parameterizedBy(resultType))
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
        return PropertySpec.builder("${kotlinMethodName(method.name)}ProgressType", PoetSymbols.asyncProgressTypeClass.parameterizedBy(progressType))
            .initializer("%L", descriptorExpression)
            .build()
    }

    private fun renderAsyncAuthoringHelper(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): FunSpec? {
        val asyncPlan = asyncMethodRuleRegistry.plan(method, currentNamespace, genericParameters) ?: return null
        val functionName = "${kotlinMethodName(method.name)}Task"
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
                    "${kotlinMethodName(method.name)}ResultType",
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
                    "${kotlinMethodName(method.name)}ProgressType",
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
                    "${kotlinMethodName(method.name)}ResultType",
                    "${kotlinMethodName(method.name)}ProgressType",
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
                projectedDeclarationSimpleName(type.name),
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
        val isStructProperty = typeRegistry.isStructType(property.type, currentNamespace)
        val supportsNullableValueReference = supportsIReferenceValueProjection(property.type, currentNamespace, typeRegistry)
        when {
            isStructProperty -> getterBuilder.addStatement(
                "return %T.fromAbi(%L)",
                propertyType,
                valueTypeProjectionSupport.invokeStructMethodWithArgs(
                    vtableIndex = getterVtableIndex,
                    structType = propertyType,
                    arguments = emptyList(),
                ),
            )
            supportsNullableValueReference -> getterBuilder.addStatement(
                "return %L",
                valueTypeProjectionSupport.nullableValueReturnExpression(
                    referenceType = property.type,
                    currentNamespace = currentNamespace,
                    abiCall = AbiCallCatalog.objectMethod(getterVtableIndex),
                ) ?: return null,
            )
            supportsGenericIReferenceStructProjection(property.type, currentNamespace, typeRegistry) -> getterBuilder.addStatement(
                "return %L",
                valueTypeProjectionSupport.genericStructReferenceReturnExpression(
                    referenceType = property.type,
                    currentNamespace = currentNamespace,
                    abiCall = AbiCallCatalog.objectMethod(getterVtableIndex),
                ) ?: return null,
            )
            supportsGenericIReferenceEnumProjection(property.type, currentNamespace, typeRegistry) -> getterBuilder.addStatement(
                "return %L",
                valueTypeProjectionSupport.genericEnumReferenceReturnExpression(
                    referenceType = property.type,
                    currentNamespace = currentNamespace,
                    abiCall = AbiCallCatalog.objectMethod(getterVtableIndex),
                ) ?: return null,
            )
            else -> when (
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
        }
        val propertyBuilder = PropertySpec.builder(propertyName, propertyType)
            .getter(getterBuilder.build())
        val isEnumProperty = typeRegistry.isEnumType(property.type, currentNamespace)
        val setterRuleFamily = PropertyRuleRegistry.interfaceSetterRuleFamily(
            property.type,
            supportsInterfaceObjectType(property.type, currentNamespace),
        )
        if (
            property.mutable &&
            property.setterVtableIndex != null &&
            (
                isStructProperty ||
                    supportsNullableValueReference ||
                    supportsGenericIReferenceStructProjection(property.type, currentNamespace, typeRegistry) ||
                    supportsGenericIReferenceEnumProjection(property.type, currentNamespace, typeRegistry) ||
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
                            isStructProperty -> addStatement(
                                "%L",
                                valueTypeProjectionSupport.invokeUnitMethodWithArgs(
                                    vtableIndex = setterVtableIndex,
                                    arguments = listOf(CodeBlock.of("value.toAbi()")),
                                ),
                            )
                            supportsNullableValueReference -> addStatement(
                                "%L",
                                AbiCallCatalog.objectSetterExpression(
                                    setterVtableIndex,
                                    valueTypeProjectionSupport.nullableValuePointerExpression(
                                        property.type,
                                        currentNamespace,
                                        "value",
                                    ) ?: return null,
                                ),
                            )
                            supportsGenericIReferenceStructProjection(property.type, currentNamespace, typeRegistry) -> addStatement(
                                "%L",
                                AbiCallCatalog.objectSetterExpression(
                                    setterVtableIndex,
                                    valueTypeProjectionSupport.genericStructReferencePointerExpression(
                                        property.type,
                                        currentNamespace,
                                        "value",
                                    ) ?: return null,
                                ),
                            )
                            supportsGenericIReferenceEnumProjection(property.type, currentNamespace, typeRegistry) -> addStatement(
                                "%L",
                                AbiCallCatalog.objectSetterExpression(
                                    setterVtableIndex,
                                    valueTypeProjectionSupport.genericEnumReferencePointerExpression(
                                        property.type,
                                        currentNamespace,
                                        "value",
                                    ) ?: return null,
                                ),
                            )
                            isEnumProperty -> addStatement(
                                "%L",
                                enumSetterAbiCall(
                                    enumUnderlyingTypeOrDefault(typeRegistry, property.type, currentNamespace),
                                    setterVtableIndex,
                                ),
                            )
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
        val functionName = kotlinMethodName(method.name)
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
            append(kotlinMethodName(method.name))
            append('(')
            append(parameterSpecs.joinToString(", ") { it.name })
            append(')')
        }
        val builder = FunSpec.builder("${kotlinMethodName(method.name)}Await")
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
        val functionName = kotlinMethodName(method.name)
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

        val functionName = kotlinMethodName(method.name)
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

    private fun kotlinMethodName(methodName: String): String {
        return when (methodName) {
            "ToString" -> "toString"
            else -> methodName.replaceFirstChar(Char::lowercase)
        }
    }

    private fun supportsInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): Boolean {
        return asyncMethodRuleRegistry.plan(method, currentNamespace, genericParameters) != null ||
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
        plannedInt32FillArrayInterfaceMethod(method, currentNamespace, genericParameters)?.let { return it }
        plannedInt32ReceiveArrayInterfaceMethod(method)?.let { return it }
        plannedInt32PassArrayInterfaceMethod(method, currentNamespace, genericParameters)?.let { return it }
        plannedValueTypeAwareInterfaceMethod(method, currentNamespace, genericParameters)?.let { return it }
        if (typeRegistry.isEnumType(method.returnType, currentNamespace)) {
            return null
        }
        val signatureKey = methodSignatureKey(
            returnType = method.returnType,
            parameterTypes = method.parameters.map { signatureParameterType(it.type, currentNamespace) },
            supportsParameterObjectType = { typeName -> supportsInterfaceObjectInput(typeName, currentNamespace) },
            supportsReturnObjectType = { typeName -> supportsInterfaceObjectReturnType(typeName, currentNamespace) },
        )
        return signatureKey
            ?.takeIf { MethodRuleRegistry.sharedMethodRuleFamily(it) != null }
            ?.let { plannedInterfaceMethodForKey(it, genericParameters) }
    }

    private fun plannedInt32FillArrayInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        if (!method.isInt32FillArrayMethod()) {
            return null
        }
        val abiArguments = int32FillArrayAbiArguments(method.parameters) { parameter ->
            val parameterCategory = methodParameterCategory(
                signatureParameterType(parameter.type, currentNamespace),
            ) { typeName -> supportsInterfaceObjectInput(typeName, currentNamespace) } ?: return@int32FillArrayAbiArguments null
            CodeBlock.of(
                "%L",
                unaryArgumentExpression(
                    argumentName = parameter.name.replaceFirstChar(Char::lowercase),
                    parameterType = parameter.type,
                    category = parameterCategory,
                    currentNamespace = currentNamespace,
                ),
            )
        } ?: return null
        return when {
            method.returnType == "Unit" -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    arrayOf(
                        interfaceVarargAbiCall("invokeUnitMethodWithArgs", method.vtableIndex!!, abiArguments),
                    )
                },
            )
            supportsInterfaceObjectReturnType(method.returnType, currentNamespace) -> PlannedInterfaceMethod(
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
            supportsFillArrayResultKind(method.returnType) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, _ ->
                    arrayOf(
                        twoArgumentReturnCode(
                            method,
                            interfaceVarargResultKindAbiCall(
                                vtableIndex = method.vtableIndex!!,
                                returnType = method.returnType,
                                abiArguments = abiArguments,
                            ),
                        ),
                    )
                },
            )
            else -> null
        }
    }

    private fun plannedInt32ReceiveArrayInterfaceMethod(method: WinMdMethod): PlannedInterfaceMethod? {
        if (!method.isInt32ReceiveArrayReturnMethod()) {
            return null
        }
        return PlannedInterfaceMethod(
            statement = "return %L",
            args = { method, currentNamespace ->
                val abiArguments = int32ReceiveArrayAbiArguments(method.parameters) { parameter ->
                    val parameterCategory = methodParameterCategory(
                        signatureParameterType(parameter.type, currentNamespace),
                    ) { typeName -> supportsInterfaceObjectInput(typeName, currentNamespace) } ?: return@int32ReceiveArrayAbiArguments null
                    CodeBlock.of(
                        "%L",
                        unaryArgumentExpression(
                            argumentName = parameter.name.replaceFirstChar(Char::lowercase),
                            parameterType = parameter.type,
                            category = parameterCategory,
                            currentNamespace = currentNamespace,
                        ),
                    )
                } ?: error("Unsupported Int32 receive-array interface method: ${method.name}")
                arrayOf(
                    int32ReceiveArrayReturnExpression(method.vtableIndex!!, abiArguments),
                )
            },
        )
    }

    private fun plannedInt32PassArrayInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        if (!method.isInt32PassArrayMethod { typeName -> supportsInterfaceObjectReturnType(typeName, currentNamespace) }) {
            return null
        }
        val abiArguments = int32PassArrayAbiArguments(method.parameters) { parameter ->
            val parameterCategory = methodParameterCategory(
                signatureParameterType(parameter.type, currentNamespace),
            ) { typeName -> supportsInterfaceObjectInput(typeName, currentNamespace) } ?: return@int32PassArrayAbiArguments null
            CodeBlock.of(
                "%L",
                unaryArgumentExpression(
                    argumentName = parameter.name.replaceFirstChar(Char::lowercase),
                    parameterType = parameter.type,
                    category = parameterCategory,
                    currentNamespace = currentNamespace,
                ),
            )
        } ?: return null
        return if (method.returnType == "Unit") {
            PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    arrayOf(
                        interfaceVarargAbiCall("invokeUnitMethodWithArgs", method.vtableIndex!!, abiArguments),
                    )
                },
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
        return when {
            valueTypeProjectionSupport.supportsSmallScalarProjection(method.returnType) -> PlannedInterfaceMethod(
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
            typeRegistry.isStructType(method.returnType, currentNamespace) -> {
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
            supportsIReferenceValueProjection(method.returnType, currentNamespace, typeRegistry) -> PlannedInterfaceMethod(
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
            supportsGenericIReferenceStructProjection(method.returnType, currentNamespace, typeRegistry) -> PlannedInterfaceMethod(
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
            supportsGenericIReferenceEnumProjection(method.returnType, currentNamespace, typeRegistry) -> PlannedInterfaceMethod(
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
            method.returnType == "Unit" &&
                method.parameters.any { parameter ->
                    valueTypeProjectionSupport.requiresValueAwareGenericAbi(parameter.type, currentNamespace)
                } -> PlannedInterfaceMethod(
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
            supportsInterfaceObjectReturnType(method.returnType, currentNamespace) &&
                method.parameters.any { parameter ->
                    valueTypeProjectionSupport.requiresValueAwareGenericAbi(parameter.type, currentNamespace)
                } -> PlannedInterfaceMethod(
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
            else -> null
        }
    }

    private fun plannedInterfaceMethodForKey(
        signatureKey: MethodSignatureKey,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        if (signatureKey.isTwoArgumentUnifiedReturnShape()) {
            return plannedTwoArgumentReturnMethod(signatureKey, genericParameters)
        }
        plannedUnaryInterfaceMethod(signatureKey, genericParameters)?.let { return it }
        signatureKey.shape.toParameterCategories()
            ?.takeIf { signatureKey.returnKind == MethodReturnKind.UNIT && it.isSupportedTwoArgumentUnitCategories() }
            ?.let {
                return PlannedInterfaceMethod(
                    statement = "%L",
                    args = { method, namespace ->
                        arrayOf(
                            AbiCallCatalog.unitMethodWithTwoArguments(
                                method.vtableIndex!!,
                                it,
                                twoArgumentArgumentExpressions(method.parameters, it, namespace)[0],
                                twoArgumentArgumentExpressions(method.parameters, it, namespace)[1],
                            ),
                        )
                    },
                )
            }
        return when (signatureKey) {
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.EMPTY) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethod(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithString(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.UINT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithUInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.INT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.BOOLEAN) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithBoolean(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.INT64) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        PoetSymbols.winRtBooleanClass,
                        AbiCallCatalog.booleanMethodWithInt64(
                            method.vtableIndex!!,
                            int64AbiArgumentExpression(argumentName, method.parameters.single().type),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.OBJECT) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        PoetSymbols.winRtBooleanClass,
                        AbiCallCatalog.booleanMethodWithObject(
                            method.vtableIndex!!,
                            interfaceObjectArgumentExpression(argumentName, method.parameters.single().type, namespace),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.int32Class, AbiCallCatalog.int32MethodWithString(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.INT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.int32Class, AbiCallCatalog.int32MethodWithInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.UINT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.int32Class, AbiCallCatalog.int32MethodWithUInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.BOOLEAN) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.int32Class, AbiCallCatalog.int32MethodWithBoolean(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.INT64) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        PoetSymbols.int32Class,
                        AbiCallCatalog.int32MethodWithInt64(
                            method.vtableIndex!!,
                            int64AbiArgumentExpression(argumentName, method.parameters.single().type),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.OBJECT) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        PoetSymbols.int32Class,
                        AbiCallCatalog.int32MethodWithObject(
                            method.vtableIndex!!,
                            interfaceObjectArgumentExpression(argumentName, method.parameters.single().type, namespace),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.EMPTY) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    arrayOf(PoetSymbols.uint32Class, AbiCallCatalog.uint32Method(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.uint32Class, AbiCallCatalog.uint32MethodWithString(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.INT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.uint32Class, AbiCallCatalog.uint32MethodWithInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.UINT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.uint32Class, AbiCallCatalog.uint32MethodWithUInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.BOOLEAN) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.uint32Class, AbiCallCatalog.uint32MethodWithBoolean(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.INT64) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        PoetSymbols.uint32Class,
                        AbiCallCatalog.uint32MethodWithInt64(
                            method.vtableIndex!!,
                            int64AbiArgumentExpression(argumentName, method.parameters.single().type),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.OBJECT) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        PoetSymbols.uint32Class,
                        AbiCallCatalog.uint32MethodWithObject(
                            method.vtableIndex!!,
                            interfaceObjectArgumentExpression(argumentName, method.parameters.single().type, namespace),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.OBJECT) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        PoetSymbols.int64Class,
                        AbiCallCatalog.int64MethodWithObject(
                            method.vtableIndex!!,
                            interfaceObjectArgumentExpression(argumentName, method.parameters.single().type, namespace),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.int64Class, AbiCallCatalog.int64MethodWithString(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.INT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.int64Class, AbiCallCatalog.int64MethodWithInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.UINT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.int64Class, AbiCallCatalog.int64MethodWithUInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.BOOLEAN) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.int64Class, AbiCallCatalog.int64MethodWithBoolean(method.vtableIndex!!, "$argumentName.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.OBJECT) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        PoetSymbols.int64Class,
                        AbiCallCatalog.int64MethodWithObject(
                            method.vtableIndex!!,
                            interfaceObjectArgumentExpression(argumentName, method.parameters.single().type, namespace),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.uint64Class, AbiCallCatalog.uint64MethodWithString(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.INT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.uint64Class, AbiCallCatalog.uint64MethodWithInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.UINT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.uint64Class, AbiCallCatalog.uint64MethodWithUInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.BOOLEAN) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.uint64Class, AbiCallCatalog.uint64MethodWithBoolean(method.vtableIndex!!, "$argumentName.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.OBJECT) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        PoetSymbols.uint64Class,
                        AbiCallCatalog.uint64MethodWithObject(
                            method.vtableIndex!!,
                            interfaceObjectArgumentExpression(argumentName, method.parameters.single().type, namespace),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.EVENT_REGISTRATION_TOKEN, MethodSignatureShape.EMPTY) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    arrayOf(PoetSymbols.eventRegistrationTokenClass, AbiCallCatalog.int64Getter(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.EMPTY) -> PlannedInterfaceMethod(
                statement = "return %T.parse(%L.toString())",
                args = { method, _ ->
                    arrayOf(PoetSymbols.guidValueClass, AbiCallCatalog.guidGetter(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "return %T.parse(%L.toString())",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.guidValueClass, AbiCallCatalog.guidMethodWithString(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.INT32) -> PlannedInterfaceMethod(
                statement = "return %T.parse(%L.toString())",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.guidValueClass, AbiCallCatalog.guidMethodWithInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.UINT32) -> PlannedInterfaceMethod(
                statement = "return %T.parse(%L.toString())",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.guidValueClass, AbiCallCatalog.guidMethodWithUInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.BOOLEAN) -> PlannedInterfaceMethod(
                statement = "return %T.parse(%L.toString())",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.guidValueClass, AbiCallCatalog.guidMethodWithBoolean(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.INT64) -> PlannedInterfaceMethod(
                statement = "return %T.parse(%L.toString())",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        PoetSymbols.guidValueClass,
                        AbiCallCatalog.guidMethodWithInt64(
                            method.vtableIndex!!,
                            int64AbiArgumentExpression(argumentName, method.parameters.single().type),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.OBJECT) -> PlannedInterfaceMethod(
                statement = "return %T.parse(%L.toString())",
                args = { method, namespace ->
                    val parameter = method.parameters.single()
                    val argumentName = parameter.name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        PoetSymbols.guidValueClass,
                        AbiCallCatalog.guidMethodWithObject(
                            method.vtableIndex!!,
                            interfaceObjectArgumentExpression(argumentName, parameter.type, namespace),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.EMPTY) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, namespace ->
                    arrayOf(objectReturnCode(method, namespace, AbiCallCatalog.objectMethod(method.vtableIndex!!), genericParameters))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(objectReturnCode(
                        method,
                        namespace,
                        AbiCallCatalog.objectMethodWithString(method.vtableIndex!!, argumentName),
                        genericParameters,
                    ))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.UINT32) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(objectReturnCode(
                        method,
                        namespace,
                        AbiCallCatalog.objectMethodWithUInt32(method.vtableIndex!!, "${argumentName}.value"),
                        genericParameters,
                    ))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.INT32) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(objectReturnCode(
                        method,
                        namespace,
                        AbiCallCatalog.objectMethodWithInt32(method.vtableIndex!!, "${argumentName}.value"),
                        genericParameters,
                    ))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.BOOLEAN) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(objectReturnCode(
                        method,
                        namespace,
                        AbiCallCatalog.objectMethodWithBoolean(method.vtableIndex!!, "${argumentName}.value"),
                        genericParameters,
                    ))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.INT64) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(objectReturnCode(
                        method,
                        namespace,
                        AbiCallCatalog.objectMethodWithInt64(method.vtableIndex!!, "${argumentName}.value"),
                        genericParameters,
                    ))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.OBJECT) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, namespace ->
                    val parameter = method.parameters.single()
                    val argumentName = parameter.name.replaceFirstChar(Char::lowercase)
                    arrayOf(objectReturnCode(
                        method,
                        namespace,
                        AbiCallCatalog.objectMethodWithObject(
                            method.vtableIndex!!,
                            interfaceObjectArgumentExpression(argumentName, parameter.type, namespace),
                        ),
                        genericParameters,
                    ))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.OBJECT_STRING) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, namespace ->
                    val firstParameter = method.parameters[0]
                    val firstArgumentName = firstParameter.name.replaceFirstChar(Char::lowercase)
                    val secondArgumentName = method.parameters[1].name.replaceFirstChar(Char::lowercase)
                    arrayOf(objectReturnCode(
                        method,
                        namespace,
                        AbiCallCatalog.objectMethodWithObjectAndString(
                            method.vtableIndex!!,
                            interfaceObjectArgumentExpression(firstArgumentName, firstParameter.type, namespace),
                            secondArgumentName,
                        ),
                        genericParameters,
                    ))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.STRING_OBJECT) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, namespace ->
                    val secondParameter = method.parameters[1]
                    val firstArgumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                    val secondArgumentName = secondParameter.name.replaceFirstChar(Char::lowercase)
                    arrayOf(objectReturnCode(
                        method,
                        namespace,
                        AbiCallCatalog.objectMethodWithStringAndObject(
                            method.vtableIndex!!,
                            firstArgumentName,
                            interfaceObjectArgumentExpression(secondArgumentName, secondParameter.type, namespace),
                        ),
                        genericParameters,
                    ))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.TWO_OBJECT) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, namespace ->
                    val firstParameter = method.parameters[0]
                    val secondParameter = method.parameters[1]
                    val firstArgumentName = firstParameter.name.replaceFirstChar(Char::lowercase)
                    val secondArgumentName = secondParameter.name.replaceFirstChar(Char::lowercase)
                    arrayOf(objectReturnCode(
                        method,
                        namespace,
                        AbiCallCatalog.resultMethodWithTwoObject(
                            method.vtableIndex!!,
                            "OBJECT",
                            PoetSymbols.requireObjectMember,
                            interfaceObjectArgumentExpression(firstArgumentName, firstParameter.type, namespace),
                            interfaceObjectArgumentExpression(secondArgumentName, secondParameter.type, namespace),
                        ),
                        genericParameters,
                    ))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.EMPTY) -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    arrayOf(AbiCallCatalog.unitMethod(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "%T.invokeStringSetter(pointer, %L, %N).getOrThrow()",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.platformComInteropClass, method.vtableIndex!!, argumentName)
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.INT32) -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(AbiCallCatalog.unitMethodWithInt32(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.UINT32) -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(AbiCallCatalog.unitMethodWithUInt32(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.BOOLEAN) -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(AbiCallCatalog.unitMethodWithInt32Expression(method.vtableIndex!!, AbiCallCatalog.booleanAsInt32Expression(argumentName)))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.INT64) -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(AbiCallCatalog.unitMethodWithInt64(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.EVENT_REGISTRATION_TOKEN) -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(AbiCallCatalog.unitMethodWithInt64(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.OBJECT) -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        AbiCallCatalog.objectSetterExpression(
                            method.vtableIndex!!,
                            interfaceObjectArgumentExpression(argumentName, method.parameters.single().type, namespace),
                        ),
                    )
                },
            )
            else -> null
        }
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
            MethodReturnKind.EVENT_REGISTRATION_TOKEN,
            MethodReturnKind.OBJECT,
            MethodReturnKind.UNIT -> plannedUnaryInterfaceMethodForReturnKind(
                returnKind = signatureKey.returnKind,
                parameterCategory = parameterCategory,
                genericParameters = genericParameters,
            )
            else -> null
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

    private fun unaryInterfaceStatement(returnKind: MethodReturnKind): String = when (returnKind) {
        MethodReturnKind.STRING -> "return %L"
        MethodReturnKind.DATE_TIME,
        MethodReturnKind.TIME_SPAN -> "return %L"
        MethodReturnKind.FLOAT32,
        MethodReturnKind.FLOAT64,
        MethodReturnKind.EVENT_REGISTRATION_TOKEN -> "return %T(%L)"
        MethodReturnKind.OBJECT -> "return %L"
        MethodReturnKind.UNIT -> "%L"
        else -> error("Unsupported unary interface return kind: $returnKind")
    }

    private fun unaryInterfaceArgs(
        method: WinMdMethod,
        namespace: String,
        returnKind: MethodReturnKind,
        abiCall: CodeBlock,
        genericParameters: Set<String>,
    ): Array<Any> = when (returnKind) {
        MethodReturnKind.STRING -> arrayOf(HStringSupport.fromCall(abiCall))
        MethodReturnKind.DATE_TIME -> arrayOf(
            CodeBlock.of(
                "%T.fromEpochSeconds((%L - %L) / 10000000L, ((%L - %L) %% 10000000L * 100).toInt())",
                PoetSymbols.dateTimeClass,
                abiCall,
                WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
                abiCall,
                WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
            ),
        )
        MethodReturnKind.TIME_SPAN -> arrayOf(CodeBlock.of("%T(%L)", PoetSymbols.timeSpanClass, abiCall))
        MethodReturnKind.FLOAT32 -> arrayOf(PoetSymbols.float32Class, abiCall)
        MethodReturnKind.FLOAT64 -> arrayOf(PoetSymbols.float64Class, abiCall)
        MethodReturnKind.EVENT_REGISTRATION_TOKEN -> arrayOf(PoetSymbols.eventRegistrationTokenClass, abiCall)
        MethodReturnKind.OBJECT -> arrayOf(objectReturnCode(method, namespace, abiCall, genericParameters))
        MethodReturnKind.UNIT -> arrayOf(abiCall)
        else -> error("Unsupported unary interface return kind: $returnKind")
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
            return when (returnKind) {
                MethodReturnKind.STRING -> AbiCallCatalog.hstringMethod(vtableIndex)
                MethodReturnKind.FLOAT32 -> AbiCallCatalog.float32Method(vtableIndex)
                MethodReturnKind.FLOAT64 -> AbiCallCatalog.float64Method(vtableIndex)
                MethodReturnKind.DATE_TIME,
                MethodReturnKind.TIME_SPAN -> AbiCallCatalog.int64Getter(vtableIndex)
                MethodReturnKind.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.int64Getter(vtableIndex)
                MethodReturnKind.OBJECT -> AbiCallCatalog.objectMethod(vtableIndex)
                MethodReturnKind.UNIT -> AbiCallCatalog.unitMethod(vtableIndex)
                else -> error("Unsupported unary interface return kind: $returnKind")
            }
        }
        val loweredArgument = unaryArgumentExpression(
            argumentName = requireNotNull(argumentName),
            parameterType = requireNotNull(parameterType),
            category = parameterCategory,
            currentNamespace = currentNamespace,
        )
        return when (returnKind) {
            MethodReturnKind.STRING -> when (parameterCategory) {
                MethodParameterCategory.STRING -> AbiCallCatalog.hstringMethodWithString(vtableIndex, argumentName)
                MethodParameterCategory.INT32 -> AbiCallCatalog.hstringMethodWithInt32(vtableIndex, loweredArgument)
                MethodParameterCategory.UINT32 -> AbiCallCatalog.hstringMethodWithUInt32(vtableIndex, loweredArgument)
                MethodParameterCategory.BOOLEAN -> AbiCallCatalog.hstringMethodWithBoolean(vtableIndex, loweredArgument)
                MethodParameterCategory.INT64,
                MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.hstringMethodWithInt64(vtableIndex, loweredArgument)
                MethodParameterCategory.OBJECT -> AbiCallCatalog.hstringMethodWithObject(vtableIndex, loweredArgument)
            }
            MethodReturnKind.FLOAT32 -> when (parameterCategory) {
                MethodParameterCategory.STRING -> AbiCallCatalog.float32MethodWithString(vtableIndex, argumentName)
                MethodParameterCategory.INT32 -> AbiCallCatalog.float32MethodWithInt32(vtableIndex, loweredArgument)
                MethodParameterCategory.UINT32 -> AbiCallCatalog.float32MethodWithUInt32(vtableIndex, loweredArgument)
                MethodParameterCategory.BOOLEAN -> AbiCallCatalog.float32MethodWithBoolean(vtableIndex, loweredArgument)
                MethodParameterCategory.INT64,
                MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.float32MethodWithInt64(vtableIndex, loweredArgument)
                MethodParameterCategory.OBJECT -> AbiCallCatalog.float32MethodWithObject(vtableIndex, loweredArgument)
            }
            MethodReturnKind.FLOAT64 -> when (parameterCategory) {
                MethodParameterCategory.STRING -> AbiCallCatalog.float64MethodWithString(vtableIndex, argumentName)
                MethodParameterCategory.INT32 -> AbiCallCatalog.float64MethodWithInt32(vtableIndex, loweredArgument)
                MethodParameterCategory.UINT32 -> AbiCallCatalog.float64MethodWithUInt32(vtableIndex, loweredArgument)
                MethodParameterCategory.BOOLEAN -> AbiCallCatalog.float64MethodWithBoolean(vtableIndex, loweredArgument)
                MethodParameterCategory.INT64,
                MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.float64MethodWithInt64(vtableIndex, loweredArgument)
                MethodParameterCategory.OBJECT -> AbiCallCatalog.float64MethodWithObject(vtableIndex, loweredArgument)
            }
            MethodReturnKind.DATE_TIME,
            MethodReturnKind.TIME_SPAN -> when (parameterCategory) {
                MethodParameterCategory.STRING -> AbiCallCatalog.int64MethodWithString(vtableIndex, argumentName)
                MethodParameterCategory.INT32 -> AbiCallCatalog.int64MethodWithInt32(vtableIndex, loweredArgument)
                MethodParameterCategory.UINT32 -> AbiCallCatalog.int64MethodWithUInt32(vtableIndex, loweredArgument)
                MethodParameterCategory.BOOLEAN -> AbiCallCatalog.int64MethodWithBoolean(vtableIndex, loweredArgument)
                MethodParameterCategory.INT64,
                MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.int64MethodWithInt64(vtableIndex, loweredArgument)
                MethodParameterCategory.OBJECT -> AbiCallCatalog.int64MethodWithObject(vtableIndex, loweredArgument)
            }
            MethodReturnKind.EVENT_REGISTRATION_TOKEN -> when (parameterCategory) {
                MethodParameterCategory.STRING -> AbiCallCatalog.int64MethodWithString(vtableIndex, argumentName)
                MethodParameterCategory.INT32 -> AbiCallCatalog.int64MethodWithInt32(vtableIndex, loweredArgument)
                MethodParameterCategory.UINT32 -> AbiCallCatalog.int64MethodWithUInt32(vtableIndex, loweredArgument)
                MethodParameterCategory.BOOLEAN -> AbiCallCatalog.int64MethodWithBoolean(vtableIndex, loweredArgument)
                MethodParameterCategory.INT64,
                MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.int64MethodWithInt64(vtableIndex, loweredArgument)
                MethodParameterCategory.OBJECT -> AbiCallCatalog.int64MethodWithObject(vtableIndex, loweredArgument)
            }
            MethodReturnKind.OBJECT -> when (parameterCategory) {
                MethodParameterCategory.STRING -> AbiCallCatalog.objectMethodWithString(vtableIndex, argumentName)
                MethodParameterCategory.INT32 -> AbiCallCatalog.objectMethodWithInt32(vtableIndex, loweredArgument)
                MethodParameterCategory.UINT32 -> AbiCallCatalog.objectMethodWithUInt32(vtableIndex, loweredArgument)
                MethodParameterCategory.BOOLEAN -> AbiCallCatalog.objectMethodWithBoolean(vtableIndex, loweredArgument)
                MethodParameterCategory.INT64,
                MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.objectMethodWithInt64(vtableIndex, loweredArgument)
                MethodParameterCategory.OBJECT -> AbiCallCatalog.objectMethodWithObject(vtableIndex, loweredArgument)
            }
            MethodReturnKind.UNIT -> when (parameterCategory) {
                MethodParameterCategory.INT32 -> AbiCallCatalog.unitMethodWithInt32(vtableIndex, argumentName)
                MethodParameterCategory.UINT32 -> AbiCallCatalog.unitMethodWithUInt32(vtableIndex, "$argumentName.value")
                MethodParameterCategory.BOOLEAN -> AbiCallCatalog.unitMethodWithInt32Expression(vtableIndex, "if ($loweredArgument) 1 else 0")
                MethodParameterCategory.INT64,
                MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.unitMethodWithInt64Expression(vtableIndex, loweredArgument)
                MethodParameterCategory.STRING -> AbiCallCatalog.unitMethodWithString(vtableIndex, argumentName)
                MethodParameterCategory.OBJECT -> AbiCallCatalog.objectSetterExpression(vtableIndex, loweredArgument)
            }
            else -> error("Unsupported unary interface return kind: $returnKind")
        }
    }

    private fun unaryArgumentExpression(
        argumentName: String,
        parameterType: String,
        category: MethodParameterCategory,
        currentNamespace: String,
    ): Any = when (category) {
        MethodParameterCategory.OBJECT -> interfaceObjectArgumentExpression(argumentName, parameterType, currentNamespace)
        MethodParameterCategory.INT32,
        MethodParameterCategory.UINT32,
        MethodParameterCategory.BOOLEAN,
        MethodParameterCategory.INT64,
        MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> int64AbiArgumentExpression(argumentName, parameterType)
        MethodParameterCategory.STRING -> argumentName
    }

    private fun plannedTwoArgumentReturnMethod(
        signatureKey: MethodSignatureKey,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod = PlannedInterfaceMethod(
        statement = "return %L",
        args = { method, namespace ->
            val parameterCategories = methodParameterCategories(
                method.parameters.map { parameter -> signatureParameterType(parameter.type, namespace) },
                { typeName -> supportsInterfaceObjectInput(typeName, namespace) },
            ) ?: error("Unsupported two-argument return shape: ${signatureKey.shape}")
            val argumentExpressions = twoArgumentArgumentExpressions(method.parameters, parameterCategories, namespace)
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
                    twoArgumentReturnCode(method, abiCall)
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

    private fun twoArgumentArgumentExpressions(
        parameters: List<WinMdParameter>,
        parameterCategories: List<MethodParameterCategory>,
        currentNamespace: String,
    ): List<Any> =
        parameters.zip(parameterCategories) { parameter, category ->
            unaryArgumentExpression(
                argumentName = parameter.name.replaceFirstChar(Char::lowercase),
                parameterType = parameter.type,
                category = category,
                currentNamespace = currentNamespace,
            )
        }

    private data class PlannedInterfaceMethod(
        val statement: String,
        val args: (WinMdMethod, String) -> Array<Any>,
    )

    private fun supportsInterfaceObjectInput(type: String, currentNamespace: String): Boolean {
        return projectedObjectArgumentLowering.supportsInputType(type, currentNamespace)
    }

    private fun signatureParameterType(type: String, currentNamespace: String): String {
        return if (typeRegistry.isEnumType(type, currentNamespace)) {
            enumSignatureType(typeRegistry, type, currentNamespace)
        } else {
            type
        }
    }

    private fun supportsInterfaceObjectType(type: String, currentNamespace: String): Boolean {
        return supportsInterfaceObjectInput(type, currentNamespace)
    }

    private fun supportsInterfaceObjectReturnType(type: String, currentNamespace: String): Boolean {
        return !typeRegistry.isStructType(type, currentNamespace) &&
            !supportsIReferenceValueProjection(type, currentNamespace, typeRegistry) &&
            (supportsProjectedObjectTypeName(type) || supportsClosedGenericInterfaceReturnType(type, currentNamespace))
    }

    private fun supportsInterfaceProperty(
        property: WinMdProperty,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): Boolean {
        return property.getterVtableIndex != null &&
            (
                typeRegistry.isStructType(property.type, currentNamespace) ||
                    supportsIReferenceValueProjection(property.type, currentNamespace, typeRegistry) ||
                    supportsGenericIReferenceStructProjection(property.type, currentNamespace, typeRegistry) ||
                    supportsGenericIReferenceEnumProjection(property.type, currentNamespace, typeRegistry) ||
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

    private fun directBaseInterface(type: WinMdType, currentNamespace: String): String? {
        return type.baseInterfaces.firstOrNull { baseInterface ->
            collectionSuperinterface(baseInterface, currentNamespace, type.genericParameters.toSet()) == null &&
                typeRegistry.findType(baseInterface, currentNamespace)?.kind == dev.winrt.winmd.plugin.WinMdTypeKind.Interface
        }
    }

    private fun projectedObjectReturnCode(
        typeName: String,
        currentNamespace: String,
        abiCall: CodeBlock,
        genericParameters: Set<String>,
    ): CodeBlock {
        closedGenericInterfaceProjectionCall(typeName, currentNamespace, abiCall)?.let { return it }
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

    private fun supportsClosedGenericInterfaceReturnType(typeName: String, currentNamespace: String): Boolean {
        val rawTypeName = closedGenericRawTypeName(typeName) ?: return false
        if (rawTypeName == "Windows.Foundation.IReference") {
            return false
        }
        val genericArgumentSource = typeName.substringAfter('<').substringBeforeLast('>')
        val hasResolvableArguments = splitGenericArguments(genericArgumentSource).all { argument ->
            try {
                winRtSignatureMapper.signatureFor(argument, currentNamespace)
                true
            } catch (_: IllegalStateException) {
                false
            }
        }
        if (!hasResolvableArguments) {
            return false
        }
        val rawType = typeRegistry.findType(rawTypeName, currentNamespace)
        return when {
            rawType != null -> rawType.kind == dev.winrt.winmd.plugin.WinMdTypeKind.Interface
            '.' in rawTypeName -> true
            else -> false
        }
    }

    private fun supportsClosedGenericInterfaceInputType(typeName: String, currentNamespace: String): Boolean {
        if (!supportsClosedGenericInterfaceReturnType(typeName, currentNamespace)) {
            return false
        }
        return closedGenericRawProjectionTypeKey(typeName, currentNamespace) != null
    }

    private fun closedGenericInterfaceProjectionCall(
        typeName: String,
        currentNamespace: String,
        abiCall: CodeBlock,
    ): CodeBlock? {
        val rawTypeName = closedGenericRawTypeName(typeName) ?: return null
        if (!supportsClosedGenericInterfaceReturnType(typeName, currentNamespace)) {
            return null
        }
        val rawTypeClass = typeNameMapper.mapTypeName(rawTypeName, currentNamespace) as? ClassName ?: return null
        val genericArgumentSource = typeName.substringAfter('<').substringBeforeLast('>')
        val genericArguments = splitGenericArguments(genericArgumentSource)
        val builder = CodeBlock.builder()
            .add("%T.from(%T(%L)", rawTypeClass, PoetSymbols.inspectableClass, abiCall)
        genericArguments.forEach { argument ->
            builder.add(", %S", winRtSignatureMapper.signatureFor(argument, currentNamespace))
        }
        genericArguments.forEach { argument ->
            builder.add(", %S", winRtProjectionTypeMapper.projectionTypeKeyFor(argument, currentNamespace))
        }
        return builder.add(")").build()
    }

    private fun closedGenericRawTypeName(typeName: String): String? {
        return typeName
            .takeIf { '<' in it && it.endsWith(">") }
            ?.substringBefore('<')
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

    private fun splitGenericArguments(source: String): List<String> {
        if (source.isBlank()) {
            return emptyList()
        }
        val arguments = mutableListOf<String>()
        var depth = 0
        var start = 0
        source.forEachIndexed { index, char ->
            when (char) {
                '<' -> depth++
                '>' -> depth--
                ',' -> if (depth == 0) {
                    arguments += source.substring(start, index).trim()
                    start = index + 1
                }
            }
        }
        arguments += source.substring(start).trim()
        return arguments
    }

    private fun twoArgumentReturnCode(method: WinMdMethod, abiCall: CodeBlock): CodeBlock {
        return when (method.returnType) {
            "String" -> HStringSupport.fromCall(abiCall)
            "Float32" -> CodeBlock.of("%T(%L)", PoetSymbols.float32Class, abiCall)
            "Float64" -> CodeBlock.of("%T(%L)", PoetSymbols.float64Class, abiCall)
            "DateTime" -> CodeBlock.of("%T.fromEpochSeconds((%L - %L) / 10000000L, ((%L - %L) %% 10000000L * 100).toInt())", PoetSymbols.dateTimeClass, abiCall, WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET, abiCall, WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET)
            "TimeSpan" -> CodeBlock.of("%T(%L)", PoetSymbols.timeSpanClass, abiCall)
            "Boolean" -> CodeBlock.of("%T(%L)", PoetSymbols.winRtBooleanClass, abiCall)
            "Int32" -> CodeBlock.of("%T(%L)", PoetSymbols.int32Class, abiCall)
            "UInt32" -> CodeBlock.of("%T(%L)", PoetSymbols.uint32Class, abiCall)
            "Int64" -> CodeBlock.of("%T(%L)", PoetSymbols.int64Class, abiCall)
            "UInt64" -> CodeBlock.of("%T(%L)", PoetSymbols.uint64Class, abiCall)
            "Guid" -> CodeBlock.of("%T.parse(%L.toString())", PoetSymbols.guidValueClass, abiCall)
            else -> error("Unsupported two-argument return type: ${method.returnType}")
        }
    }

    private fun resultKindName(returnType: String): String {
        return when (returnType) {
            "String" -> "HSTRING"
            "Float32" -> "FLOAT32"
            "Float64" -> "FLOAT64"
            "DateTime" -> "INT64"
            "TimeSpan" -> "INT64"
            "Boolean" -> "BOOLEAN"
            "Int32" -> "INT32"
            "UInt32" -> "UINT32"
            "Int64" -> "INT64"
            "UInt64" -> "UINT64"
            "Guid" -> "GUID"
            else -> error("Unsupported result kind for two-argument return type: $returnType")
        }
    }

    private fun resultExtractor(returnType: String): Any {
        return when (returnType) {
            "String" -> PoetSymbols.requireHStringMember
            "Float32" -> PoetSymbols.requireFloat32Member
            "Float64" -> PoetSymbols.requireFloat64Member
            "DateTime" -> PoetSymbols.requireInt64Member
            "TimeSpan" -> PoetSymbols.requireInt64Member
            "Boolean" -> PoetSymbols.requireBooleanMember
            "Int32" -> PoetSymbols.requireInt32Member
            "UInt32" -> PoetSymbols.requireUInt32Member
            "Int64" -> PoetSymbols.requireInt64Member
            "UInt64" -> PoetSymbols.requireUInt64Member
            "Guid" -> PoetSymbols.requireGuidMember
            else -> error("Unsupported result extractor for two-argument return type: $returnType")
        }
    }

    private fun supportsFillArrayResultKind(returnType: String): Boolean {
        return when (returnType) {
            "String",
            "Float32",
            "Float64",
            "DateTime",
            "TimeSpan",
            "Boolean",
            "Int32",
            "UInt32",
            "Int64",
            "UInt64",
            "Guid",
            -> true
            else -> false
        }
    }

    private fun inheritedSignatureKeys(baseInterface: String?): Set<String> {
        val interfaceType = baseInterface?.let { typeRegistry.findType(it) } ?: return emptySet()
        return interfaceType.methods.mapTo(linkedSetOf(), ::interfaceMethodRenderKey)
    }

    private fun inheritedPropertyNames(baseInterface: String?): Set<String> {
        val interfaceType = baseInterface?.let { typeRegistry.findType(it) } ?: return emptySet()
        return interfaceType.properties.mapTo(linkedSetOf(), WinMdProperty::name)
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

    private fun interfaceMethodRenderKey(method: WinMdMethod): String {
        return method.overloadKey(renderedName = kotlinMethodName(method.name))
    }

}
