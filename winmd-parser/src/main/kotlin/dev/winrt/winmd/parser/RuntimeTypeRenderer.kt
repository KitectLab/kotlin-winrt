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
        val runtimeInterfaceTypes = runtimeInterfaceTypes(type)
        val overrideInterfaceTypes = typeRegistry.findRuntimeClassOverridesTypes(type.name, type.namespace)
        val exposedRuntimeInterfaceTypes = runtimeInterfaceTypes.filterNot { interfaceType ->
            typeRegistry.isVersionedRuntimeClassInterface(interfaceType.name, interfaceType.namespace) ||
                typeRegistry.isPrimaryRuntimeClassInterface(interfaceType.name, interfaceType.namespace) ||
                typeRegistry.isRuntimeClassOverridesInterface(interfaceType.name, interfaceType.namespace)
        }
        val overridePropertyNames = exposedRuntimeInterfaceTypes.flatMapTo(linkedSetOf(), ::allInterfacePropertyNames)
        val overrideMethodKeys = exposedRuntimeInterfaceTypes.flatMapTo(linkedSetOf(), ::allInterfaceMethodKeys)
        val inheritedOverridePropertyNames = inheritedOverrideHookPropertyNames(type)
        val inheritedOverrideMethodKeys = inheritedOverrideHookMethodKeys(type)
        val runtimeProperties = dedupeRuntimeProperties(
            type.properties.filter { runtimePropertyRenderer.canRenderRuntimeProperty(it, type.namespace) },
        )
        val overrideProperties = dedupeRuntimeProperties(
            overrideInterfaceProperties(type, overrideInterfaceTypes)
                .filter { runtimePropertyRenderer.canRenderRuntimeProperty(it, type.namespace) },
        )
        val runtimeMethods = dedupeRuntimeMethods(
            type.methods.filter { runtimeMethodRenderer.canRenderRuntimeMethod(it, type.namespace) },
        )
        val runtimeLambdaMethods = dedupeRuntimeMethods(
            type.methods.filter { runtimeMethodRenderer.renderRuntimeLambdaOverload(it, type.namespace) != null },
        )
        val superclass = type.baseClass
            ?.takeUnless { it == "System.Object" }
            ?.let { typeNameMapper.mapTypeName(it, type.namespace) }
            ?: PoetSymbols.inspectableClass
        val builder = TypeSpec.classBuilder(type.name)
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(pointerConstructor())
            .superclass(superclass)
            .addSuperclassConstructorParameter("pointer")

        exposedRuntimeInterfaceTypes
            .map { interfaceType -> typeNameMapper.mapTypeName("${interfaceType.namespace}.${interfaceType.name}", type.namespace) }
            .forEach(builder::addSuperinterface)

        if (isDispatchQueueRuntimeClass(type)) {
            builder.addSuperinterface(PoetSymbols.dispatchQueueClass)
            builder.addFunction(
                FunSpec.builder("dispatch")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("block", LambdaTypeName.get(returnType = Unit::class.asTypeName()))
                    .returns(Boolean::class)
                    .addStatement("return tryEnqueue(%T(block)).value", ClassName(type.namespace.lowercase(), "DispatcherQueueHandler"))
                    .build(),
            )
        }

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
        if (type.activationKind == WinMdActivationKind.Factory) {
            builder.addFunction(
                FunSpec.constructorBuilder()
                    .callThisConstructor(CodeBlock.of("Companion.%L().pointer", type.activationFunctionName))
                    .build(),
            )
        }
        renderFactoryConstructors(type).forEach(builder::addFunction)

        runtimeProperties.forEach { property ->
            builder.addProperty(runtimePropertyRenderer.renderBackingProperty(property, type.namespace))
            val runtimeProperty = runtimePropertyRenderer.renderRuntimeProperty(property, type.namespace)
            builder.addProperty(
                if (property.name in overridePropertyNames) {
                    runtimeProperty.toBuilder().addModifiers(KModifier.OVERRIDE).build()
                } else {
                    runtimeProperty
                },
            )
        }
        overrideProperties.forEach { property ->
            builder.addProperty(runtimePropertyRenderer.renderBackingProperty(property, type.namespace))
            builder.addProperty(
                runtimePropertyRenderer.renderRuntimeProperty(property, type.namespace)
                    .toBuilder()
                    .addModifiers(
                        KModifier.PROTECTED,
                        if (property.name in inheritedOverridePropertyNames) KModifier.OVERRIDE else KModifier.OPEN,
                    )
                    .build(),
            )
        }
        runtimeMethods
            .flatMap { method ->
                runtimeMethodRenderer.renderRuntimeMethods(method, type.namespace).map { rendered ->
                    if (runtimeMethodRenderKey(method) in overrideMethodKeys && KModifier.OVERRIDE !in rendered.modifiers) {
                        rendered.toBuilder().addModifiers(KModifier.OVERRIDE).build()
                    } else {
                        rendered
                    }
                }
            }
            .forEach(builder::addFunction)
        runtimeLambdaMethods
            .mapNotNull { runtimeMethodRenderer.renderRuntimeLambdaOverload(it, type.namespace) }
            .forEach(builder::addFunction)
        overrideInterfaceMethods(type, overrideInterfaceTypes)
            .mapNotNull { method ->
                runtimeMethodRenderer.renderRuntimeMethod(method, type.namespace)?.let { rendered -> method to rendered }
            }
            .map { (method, rendered) ->
                rendered.toBuilder()
                    .addModifiers(
                        KModifier.PROTECTED,
                        if (runtimeMethodRenderKey(method) in inheritedOverrideMethodKeys) KModifier.OVERRIDE else KModifier.OPEN,
                    )
                    .build()
            }
            .forEach(builder::addFunction)
        renderEventSlotMembers(type.methods, type.namespace).let { projection ->
            projection.properties.forEach(builder::addProperty)
            projection.types.forEach(builder::addType)
        }
        runtimeCompanionRenderer.renderStaticEventSlotTypes(type).forEach(builder::addType)
        builder.addType(runtimeCompanionRenderer.render(type))
        return builder.build()
    }

    private fun renderFactoryConstructors(type: WinMdType): List<FunSpec> {
        return typeRegistry.findRuntimeClassFactoryTypes(type.name, type.namespace)
            .flatMap { factoryType ->
                val factoryPropertyName = helperAccessorName(factoryType.name)
                factoryType.methods
                    .filter { method -> method.returnType == "${type.namespace}.${type.name}" }
                    .map { method ->
                        FunSpec.constructorBuilder()
                            .addParameters(
                                method.parameters.map { parameter ->
                                    com.squareup.kotlinpoet.ParameterSpec.builder(
                                        parameter.name,
                                        typeNameMapper.mapTypeName(parameter.type, type.namespace),
                                    ).build()
                                },
                            )
                            .callThisConstructor(
                                CodeBlock.of(
                                    "Companion.%L%L(%L).pointer",
                                    factoryPropertyName,
                                    method.name,
                                    method.parameters.joinToString(", ") { it.name },
                                ),
                            )
                            .build()
                    }
            }
    }

    private fun overrideInterfaceMethods(type: WinMdType, overrideInterfaceTypes: List<WinMdType>): List<WinMdMethod> {
        val existingKeys = type.methods.mapTo(linkedSetOf(), ::runtimeMethodRenderKey)
        return overrideInterfaceTypes
            .flatMap(::allInterfaceMethods)
            .filterNot { runtimeMethodRenderKey(it) in existingKeys }
            .distinctBy(::runtimeMethodRenderKey)
    }

    private fun overrideInterfaceProperties(type: WinMdType, overrideInterfaceTypes: List<WinMdType>): List<dev.winrt.winmd.plugin.WinMdProperty> {
        val existingNames = type.properties.mapTo(linkedSetOf()) { it.name }
        return overrideInterfaceTypes
            .flatMap(::allInterfaceProperties)
            .filterNot { it.name in existingNames }
            .distinctBy { it.name }
    }

    private fun inheritedOverrideHookMethodKeys(type: WinMdType): Set<String> {
        val baseType = type.baseClass
            ?.takeUnless { it == "System.Object" }
            ?.let { typeRegistry.findType(it, type.namespace) }
            ?.takeIf { it.kind == dev.winrt.winmd.plugin.WinMdTypeKind.RuntimeClass }
            ?: return emptySet()
        return buildSet {
            addAll(inheritedOverrideHookMethodKeys(baseType))
            addAll(
                typeRegistry.findRuntimeClassOverridesTypes(baseType.name, baseType.namespace)
                    .flatMap(::allInterfaceMethods)
                    .map(::runtimeMethodRenderKey),
            )
        }
    }

    private fun inheritedOverrideHookPropertyNames(type: WinMdType): Set<String> {
        val baseType = type.baseClass
            ?.takeUnless { it == "System.Object" }
            ?.let { typeRegistry.findType(it, type.namespace) }
            ?.takeIf { it.kind == dev.winrt.winmd.plugin.WinMdTypeKind.RuntimeClass }
            ?: return emptySet()
        return buildSet {
            addAll(inheritedOverrideHookPropertyNames(baseType))
            addAll(
                typeRegistry.findRuntimeClassOverridesTypes(baseType.name, baseType.namespace)
                    .flatMap(::allInterfaceProperties)
                    .map { it.name },
            )
        }
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
                            .addStatement("val delegateHandle = delegateHandles[token]")
                            .addStatement("%T.invokeUnitMethodWithInt64Arg(pointer, %L, token.value).getOrThrow()", PoetSymbols.platformComInteropClass, plan.removeVtableIndex)
                            .addStatement("delegateHandles.remove(token)")
                            .addStatement("delegateHandle?.close()")
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

    private fun runtimeInterfaceTypes(type: WinMdType): List<WinMdType> {
        val defaultInterfaceType = type.defaultInterface
            ?.takeIf { typeRegistry.isRuntimeProjectedInterface(it, type.namespace) }
            ?.let { interfaceName -> typeRegistry.findType(interfaceName, type.namespace) }
        val defaultInterfaceName = defaultInterfaceType?.let { canonicalInterfaceName("${it.namespace}.${it.name}") }
        val inheritedByDefault = defaultInterfaceType
            ?.let(::allImplementedInterfaceNames)
            .orEmpty()
        val collectionBackedInterfaces = buildSet {
            addAll(type.implementedInterfaces)
            addAll(type.baseInterfaces)
            type.defaultInterface?.let(::add)
        }.filterTo(linkedSetOf()) { interfaceName ->
            collectionSuperinterface(interfaceName, type.namespace, emptySet()) != null ||
                isCollectionRuntimeInterface(interfaceName) ||
                isIterableProjectionInterface(interfaceName)
        }
        return buildList {
            defaultInterfaceType
                ?.takeIf { "${it.namespace}.${it.name}" !in collectionBackedInterfaces }
                ?.let(::add)
            (type.implementedInterfaces + type.baseInterfaces)
                .filter {
                    typeRegistry.isRuntimeProjectedInterface(it, type.namespace) &&
                        it != type.defaultInterface &&
                        canonicalInterfaceName(it) !in inheritedByDefault &&
                        it !in collectionBackedInterfaces
                }
                .mapNotNull { interfaceName -> typeRegistry.findType(interfaceName, type.namespace) }
                .filterNot { interfaceType ->
                    defaultInterfaceName != null &&
                        defaultInterfaceName in allImplementedInterfaceNames(interfaceType)
                }
                .forEach(::add)
        }.distinctBy { "${it.namespace}.${it.name}" }
    }

    private fun isDispatchQueueRuntimeClass(type: WinMdType): Boolean {
        return (type.namespace == "Microsoft.UI.Dispatching" || type.namespace == "Windows.System") &&
            type.name == "DispatcherQueue" &&
            type.methods.any { method ->
                method.name == "TryEnqueue" &&
                    method.parameters.size == 1 &&
                    method.parameters.single().type == "${type.namespace}.DispatcherQueueHandler" &&
                    method.returnType == "Windows.Foundation.WinRtBoolean"
            }
    }

    private fun allImplementedInterfaceNames(type: WinMdType): Set<String> {
        return buildSet {
            add(canonicalInterfaceName("${type.namespace}.${type.name}"))
            type.baseInterfaces.forEach { baseInterface ->
                add(canonicalInterfaceName(baseInterface))
                typeRegistry.findType(baseInterface, type.namespace)
                    ?.takeIf { it.kind == dev.winrt.winmd.plugin.WinMdTypeKind.Interface }
                    ?.let { addAll(allImplementedInterfaceNames(it)) }
            }
        }
    }

    private fun dedupeRuntimeProperties(properties: List<dev.winrt.winmd.plugin.WinMdProperty>): List<dev.winrt.winmd.plugin.WinMdProperty> {
        return properties.asReversed().distinctBy { it.name }.asReversed()
    }

    private fun dedupeRuntimeMethods(methods: List<WinMdMethod>): List<WinMdMethod> {
        return methods.asReversed().distinctBy(::runtimeMethodRenderKey).asReversed()
    }

    private fun runtimeMethodRenderKey(method: WinMdMethod): String {
        return buildString {
            append(renderedRuntimeMethodName(method))
            append('(')
            append(method.parameters.joinToString(",") { it.type })
            append(')')
        }
    }

    private fun renderedRuntimeMethodName(method: WinMdMethod): String {
        return if (method.name == "ToString" && method.returnType == "String" && method.parameters.isEmpty()) {
            "toString"
        } else {
            method.name.replaceFirstChar(Char::lowercase)
        }
    }

    private fun isIterableProjectionInterface(interfaceName: String): Boolean {
        return interfaceName == "Microsoft.UI.Xaml.Interop.IBindableIterable" ||
            interfaceName == "Microsoft.UI.Xaml.Interop.IBindableIterator" ||
            interfaceName.startsWith("Windows.Foundation.Collections.IIterable<") ||
            interfaceName.startsWith("Windows.Foundation.Collections.IIterator<")
    }

    private fun isCollectionRuntimeInterface(interfaceName: String): Boolean {
        return when (canonicalInterfaceName(interfaceName).substringAfterLast('.')) {
            "IIterable",
            "IIterator",
            "IVector",
            "IVectorView",
            "IMap",
            "IMapView",
            "IKeyValuePair",
            "IBindableIterable",
            "IBindableIterator",
            "IBindableVector",
            "IBindableVectorView",
            -> true
            else -> false
        }
    }

    private fun canonicalInterfaceName(interfaceName: String): String {
        return interfaceName.substringBefore('<')
            .substringBefore('`')
    }

    private fun allInterfaceMethodKeys(type: WinMdType): Set<String> {
        return buildSet {
            addAll(type.methods.map(::runtimeMethodRenderKey))
            type.baseInterfaces
                .mapNotNull { baseInterface -> typeRegistry.findType(baseInterface, type.namespace) }
                .filter { it.kind == dev.winrt.winmd.plugin.WinMdTypeKind.Interface }
                .forEach { addAll(allInterfaceMethodKeys(it)) }
        }
    }

    private fun allInterfaceMethods(type: WinMdType): List<WinMdMethod> {
        val inherited = type.baseInterfaces
            .mapNotNull { baseInterface -> typeRegistry.findType(baseInterface, type.namespace) }
            .filter { it.kind == dev.winrt.winmd.plugin.WinMdTypeKind.Interface }
            .flatMap(::allInterfaceMethods)
        return (inherited + type.methods).asReversed().distinctBy(::runtimeMethodRenderKey).asReversed()
    }

    private fun allInterfacePropertyNames(type: WinMdType): Set<String> {
        return buildSet {
            addAll(type.properties.map { it.name })
            type.baseInterfaces
                .mapNotNull { baseInterface -> typeRegistry.findType(baseInterface, type.namespace) }
                .filter { it.kind == dev.winrt.winmd.plugin.WinMdTypeKind.Interface }
                .forEach { addAll(allInterfacePropertyNames(it)) }
        }
    }

    private fun allInterfaceProperties(type: WinMdType): List<dev.winrt.winmd.plugin.WinMdProperty> {
        val inherited = type.baseInterfaces
            .mapNotNull { baseInterface -> typeRegistry.findType(baseInterface, type.namespace) }
            .filter { it.kind == dev.winrt.winmd.plugin.WinMdTypeKind.Interface }
            .flatMap(::allInterfaceProperties)
        return (inherited + type.properties).asReversed().distinctBy { it.name }.asReversed()
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
