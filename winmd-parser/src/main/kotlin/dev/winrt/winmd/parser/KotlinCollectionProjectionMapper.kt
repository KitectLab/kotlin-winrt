package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdType

internal class KotlinCollectionProjectionMapper {
    fun runtimeClassCollectionInterfaces(
        type: WinMdType,
    ): Sequence<String> {
        return sequenceOf(type.defaultInterface)
            .filterNotNull()
            .plus(type.baseInterfaces.asSequence())
            .distinct()
    }

    fun runtimeClassProjection(type: WinMdType): RuntimeCollectionProjection? {
        if (type.namespace == "Windows.Foundation.Collections" && type.name == "StringVectorView") {
            return RuntimeCollectionProjection(
                superinterface = PoetSymbols.listClass.parameterizedBy(String::class.asTypeName()),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { winRtSize.value.toInt() }, getter = { index -> getAt(%T(index.toUInt())) })",
                    PoetSymbols.winRtListProjectionClass.parameterizedBy(String::class.asTypeName()),
                    PoetSymbols.uint32Class,
                ),
                winRtSizeSlot = 7,
                extraProperties = listOf(
                    PropertySpec.builder("size", Int::class)
                        .getter(
                            FunSpec.getterBuilder()
                                .addStatement(
                                    "return %T(%L).value.toInt()",
                                    PoetSymbols.uint32Class,
                                    AbiCallCatalog.uint32Method(7),
                                )
                                .build(),
                        )
                        .build(),
                ),
                extraFunctions = listOf(
                    FunSpec.builder("getAt")
                        .addParameter("index", PoetSymbols.uint32Class)
                        .returns(String::class)
                        .addStatement(
                            "return %L",
                            HStringSupport.fromCall(AbiCallCatalog.hstringMethodWithUInt32(6, "index.value")),
                        )
                        .build(),
                ),
            )
        }
        return null
    }

    fun runtimeClassInterfaceProjection(
        type: WinMdType,
        typeNameMapper: TypeNameMapper,
        winRtSignatureMapper: WinRtSignatureMapper,
        winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    ): RuntimeCollectionProjection? {
        val collectionInterface = runtimeClassCollectionInterfaces(type)
            .mapNotNull {
                interfaceProjectionMetadata(
                    qualifiedName = it,
                    typeNameMapper = typeNameMapper,
                    winRtSignatureMapper = winRtSignatureMapper,
                    winRtProjectionTypeMapper = winRtProjectionTypeMapper,
                )
            }
            .firstOrNull()
            ?: return null
        return RuntimeCollectionProjection(
            superinterface = collectionInterface.collectionSuperinterface,
            delegateFactory = collectionInterface.delegateFactory,
            winRtSizeSlot = collectionInterface.winRtSizeSlot,
            extraProperties = collectionInterface.extraProperties,
            extraFunctions = collectionInterface.extraFunctions,
        )
    }

    fun runtimeClassIterableProjection(
        type: WinMdType,
        typeNameMapper: TypeNameMapper,
        winRtSignatureMapper: WinRtSignatureMapper,
        winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    ): RuntimeIterableProjection? {
        val iterableInterface = runtimeClassCollectionInterfaces(type)
            .firstOrNull { qualifiedName ->
                xamlBindableRuntimeIterableSuperinterface(qualifiedName) != null ||
                    qualifiedName.startsWith("Windows.Foundation.Collections.IIterable<") ||
                    qualifiedName.startsWith("Windows.Foundation.Collections.IIterator<")
            }
            ?: return null
        xamlBindableRuntimeIterableSuperinterface(iterableInterface)?.let { superinterface ->
            return RuntimeIterableProjection(
                superinterface = superinterface,
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer))",
                    typeNameMapper.mapTypeName(iterableInterface, iterableInterface.substringBeforeLast('.')) as ClassName,
                    PoetSymbols.inspectableClass,
                ),
            )
        }
        return closedGenericIterableProjection(
            qualifiedName = iterableInterface,
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )
    }

    fun interfaceProjection(type: WinMdType): InterfaceCollectionProjection? {
        windowsFoundationCollectionProjection(type)?.let { return it }
        val descriptor = xamlBindableDescriptor(type.namespace, type.name) ?: return null
        return InterfaceCollectionProjection(
            superinterface = descriptor.collectionSuperinterface,
            delegateFactory = descriptor.collectionDelegateProjection?.let { delegateProjection ->
                namedDelegateFactory(delegateProjection, descriptor.collectionDelegateArguments)
            },
            winRtSizeSlot = descriptor.winRtSizeSlot,
            extraProperties = descriptor.extraProperties,
            extraFunctions = descriptor.extraFunctions,
        )
    }

    private fun windowsFoundationCollectionProjection(type: WinMdType): InterfaceCollectionProjection? {
        if (type.namespace != "Windows.Foundation.Collections") {
            return null
        }
        return when (type.name) {
            "IIterable`1" -> iterableInterfaceProjection(type)
            "IIterator`1" -> iteratorInterfaceProjection(type)
            "IKeyValuePair`2" -> keyValuePairInterfaceProjection(type)
            "IMapView`2" -> mapInterfaceProjection(type, readOnlyMapProjectionDescriptor)
            "IMap`2",
            "IObservableMap`2",
            -> mapInterfaceProjection(type, mutableMapProjectionDescriptor)
            "IVectorView`1" -> vectorInterfaceProjection(type, readOnlyVectorProjectionDescriptor)
            "IVector`1",
            "IObservableVector`1",
            -> vectorInterfaceProjection(type, mutableVectorProjectionDescriptor)
            else -> null
        }
    }

    private fun iterableInterfaceProjection(type: WinMdType): InterfaceCollectionProjection? {
        val elementTypeName = genericTypeVariables(type, 1)?.singleOrNull() ?: return null
        val rawIteratorClass = projectedDeclarationClassName(type.namespace, "IIterator")
        return genericCollectionProjection(
            type = type,
            superinterface = PoetSymbols.iterableClass.parameterizedBy(elementTypeName),
            extraFunctions = listOf(
                iterableIteratorOverrideFunction(elementTypeName),
                FunSpec.builder("first")
                    .returns(rawIteratorClass.parameterizedBy(elementTypeName))
                    .addStatement(
                        "return %T.from(%T(%T.invokeObjectMethod(pointer, %L).getOrThrow()), genericArg0Signature, genericArg0ProjectionTypeKey)",
                        rawIteratorClass,
                        PoetSymbols.inspectableClass,
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "First", 6),
                    )
                    .build(),
            ),
        )
    }

    private fun iteratorInterfaceProjection(type: WinMdType): InterfaceCollectionProjection? {
        val elementTypeName = genericTypeVariables(type, 1)?.singleOrNull() ?: return null
        return genericCollectionProjection(
            type = type,
            superinterface = PoetSymbols.iteratorClass.parameterizedBy(elementTypeName),
            extraProperties = listOf(
                PropertySpec.builder("winRtCurrent", elementTypeName)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement(
                                "return projectedGenericMethodResult(%L, genericArg0Signature, genericArg0ProjectionTypeKey) as %T",
                                getterSlot(type, "Current", 6),
                                elementTypeName,
                            )
                            .build(),
                    )
                    .build(),
                PropertySpec.builder("current", elementTypeName)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return winRtCurrent")
                            .build(),
                    )
                    .build(),
                winRtHasCurrentProperty(
                    CodeBlock.of(
                        "return %M(%T.invokeBooleanGetter(pointer, %L).getOrThrow())\n",
                        PoetSymbols.winRtBooleanMember,
                        PoetSymbols.platformComInteropClass,
                        getterSlot(type, "HasCurrent", 7),
                    ),
                ),
            ),
            extraFunctions = iteratorTraversalFunctions(elementTypeName),
        )
    }

    private fun keyValuePairInterfaceProjection(type: WinMdType): InterfaceCollectionProjection? {
        val (keyTypeName, valueTypeName) = genericTypeVariables(type, 2)?.let { it[0] to it[1] } ?: return null
        return genericCollectionProjection(
            type = type,
            superinterface = PoetSymbols.mapEntryClass.parameterizedBy(keyTypeName, valueTypeName),
            extraProperties = listOf(
                PropertySpec.builder("key", keyTypeName)
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement(
                                "return projectedGenericMethodResult(%L, genericArg0Signature, genericArg0ProjectionTypeKey) as %T",
                                getterSlot(type, "Key", 6),
                                keyTypeName,
                            )
                            .build(),
                    )
                    .build(),
                PropertySpec.builder("value", valueTypeName)
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement(
                                "return projectedGenericMethodResult(%L, genericArg1Signature, genericArg1ProjectionTypeKey) as %T",
                                getterSlot(type, "Value", 7),
                                valueTypeName,
                            )
                            .build(),
                    )
                    .build(),
            ),
        )
    }

    private fun mapInterfaceProjection(
        type: WinMdType,
        descriptor: MapProjectionDescriptor,
    ): InterfaceCollectionProjection? {
        val (keyTypeName, valueTypeName) = genericTypeVariables(type, 2)?.let { it[0] to it[1] } ?: return null
        val context = MapProjectionContext(
            type = type,
            keyTypeName = keyTypeName,
            valueTypeName = valueTypeName,
            entriesExpression = mapEntryIterableExpression(
                rawIterableClass = projectedDeclarationClassName(type.namespace, "IIterable"),
                rawEntryClass = projectedDeclarationClassName(type.namespace, "IKeyValuePair"),
                keySignature = genericArgSignature(0),
                valueSignature = genericArgSignature(1),
                keyProjectionTypeKey = genericArgProjectionTypeKey(0),
                valueProjectionTypeKey = genericArgProjectionTypeKey(1),
            ),
        )
        return genericCollectionProjection(
            type = type,
            superinterface = descriptor.superinterface(context),
            delegateFactory = namedDelegateFactory(
                descriptor.delegateProjection(context),
                descriptor.delegateArguments.map { argument ->
                    argument.name to argument.expression(context)
                },
            ),
            winRtSizeSlot = 7,
            extraFunctions = descriptor.operations.map { operation -> projectionOperation(context, operation) },
        )
    }

    private fun vectorInterfaceProjection(
        type: WinMdType,
        descriptor: VectorProjectionDescriptor,
    ): InterfaceCollectionProjection? {
        val elementTypeName = genericTypeVariables(type, 1)?.singleOrNull() ?: return null
        val context = VectorProjectionContext(
            type = type,
            elementTypeName = elementTypeName,
            indexOfDefaultSlot = descriptor.indexOfDefaultSlot,
        )
        return genericCollectionProjection(
            type = type,
            superinterface = descriptor.superinterface(context),
            delegateFactory = namedDelegateFactory(
                descriptor.delegateProjection(context),
                descriptor.delegateArguments.map { argument ->
                    argument.name to argument.expression(context)
                },
            ),
            winRtSizeSlot = 7,
            extraFunctions = descriptor.operations.map { operation -> projectionOperation(context, operation) },
        )
    }

    private fun namedDelegateFactory(
        projectionType: TypeName,
        arguments: List<Pair<String, CodeBlock>>,
    ): CodeBlock {
        return CodeBlock.builder()
            .add("%T(", projectionType)
            .apply {
                arguments.forEachIndexed { index, (name, expression) ->
                    if (index > 0) {
                        add(", ")
                    }
                    add("%L = %L", name, expression)
                }
            }
            .add(")")
            .build()
    }

    private fun genericCollectionProjection(
        type: WinMdType,
        superinterface: TypeName,
        delegateFactory: CodeBlock? = null,
        winRtSizeSlot: Int? = null,
        extraProperties: List<PropertySpec> = emptyList(),
        extraFunctions: List<FunSpec> = emptyList(),
    ): InterfaceCollectionProjection {
        return InterfaceCollectionProjection(
            superinterface = superinterface,
            delegateFactory = delegateFactory,
            winRtSizeSlot = winRtSizeSlot,
            extraProperties = genericStateProperties(type) + extraProperties,
            extraFunctions = genericProjectionHelperFunctions() + extraFunctions,
        )
    }

    private fun genericTypeVariables(
        type: WinMdType,
        expectedCount: Int,
    ): List<TypeVariableName>? {
        return type.genericParameters
            .takeIf { it.size == expectedCount }
            ?.map { parameter -> TypeVariableName(parameter) }
    }

    private fun <C> projectionOperation(
        context: C,
        operation: ProjectionOperation<C>,
    ): FunSpec {
        return FunSpec.builder(operation.name)
            .apply {
                if (operation.modifiers.isNotEmpty()) {
                    addModifiers(*operation.modifiers.toTypedArray())
                }
                operation.parameters(context).forEach(::addParameter)
                operation.returnType?.invoke(context)?.let(::returns)
                addCode(operation.body(context))
            }
            .build()
    }

    private val commonMapDelegateArguments = listOf(
        ProjectionDelegateArgument<MapProjectionContext>("sizeProvider") {
            CodeBlock.of("{ winRtSize.value.toInt() }")
        },
        ProjectionDelegateArgument("lookupFn") {
            CodeBlock.of("{ key -> lookup(key) }")
        },
        ProjectionDelegateArgument("containsKeyFn") {
            CodeBlock.of("{ key -> hasKey(key).value }")
        },
        ProjectionDelegateArgument("entriesProvider") { context ->
            CodeBlock.of("{ %L.asSequence().toList() }", context.entriesExpression)
        },
    )

    private val mutableMapDelegateArguments = listOf(
        ProjectionDelegateArgument<MapProjectionContext>("putValueFn") {
            CodeBlock.of("{ key, value -> insert(key, value).value }")
        },
        ProjectionDelegateArgument("removeKeyFn") {
            CodeBlock.of("{ key -> winRtRemoveKey(key); true }")
        },
        ProjectionDelegateArgument("clearerFn") {
            CodeBlock.of("{ clear() }")
        },
    )

    private val commonMapOperations = listOf(
        ProjectionOperation<MapProjectionContext>(
            name = "lookup",
            parameters = { context -> mapParameters(context, listOf(MapParameter.KEY)) },
            returnType = { context -> context.valueTypeName },
            body = { context -> mapProjectedResultBody(context, "Lookup", 6, 1, listOf(MapParameter.KEY)) },
        ),
        ProjectionOperation(
            name = "hasKey",
            parameters = { context -> mapParameters(context, listOf(MapParameter.KEY)) },
            returnType = { PoetSymbols.winRtBooleanClass },
            body = { context -> mapBooleanResultBody(context, "HasKey", 8, listOf(MapParameter.KEY)) },
        ),
    )

    private val mutableMapOperations = listOf(
        ProjectionOperation<MapProjectionContext>(
            name = "getView",
            returnType = { context ->
                projectedDeclarationClassName(context.type.namespace, "IMapView")
                    .parameterizedBy(context.keyTypeName, context.valueTypeName)
            },
            body = { context -> objectFactoryBody(context.type, "IMapView", "GetView", 9, listOf(0, 1)) },
        ),
        ProjectionOperation(
            name = "insert",
            parameters = { context -> mapParameters(context, listOf(MapParameter.KEY, MapParameter.VALUE)) },
            returnType = { PoetSymbols.winRtBooleanClass },
            body = { context ->
                mapBooleanResultBody(context, "Insert", 10, listOf(MapParameter.KEY, MapParameter.VALUE))
            },
        ),
        ProjectionOperation(
            name = "winRtRemoveKey",
            modifiers = listOf(KModifier.PROTECTED),
            parameters = { context -> mapParameters(context, listOf(MapParameter.KEY)) },
            body = { context -> mapUnitBody(context, "Remove", 11, listOf(MapParameter.KEY)) },
        ),
        ProjectionOperation(
            name = "clear",
            body = { context -> unitBody(context.type, "Clear", 12) },
        ),
    )

    private val readOnlyMapProjectionDescriptor = MapProjectionDescriptor(
        superinterface = { context ->
            PoetSymbols.mapClass.parameterizedBy(context.keyTypeName, context.valueTypeName)
        },
        delegateProjection = { context ->
            PoetSymbols.winRtMapProjectionClass.parameterizedBy(context.keyTypeName, context.valueTypeName)
        },
        delegateArguments = commonMapDelegateArguments,
        operations = commonMapOperations,
    )

    private val mutableMapProjectionDescriptor = MapProjectionDescriptor(
        superinterface = { context ->
            PoetSymbols.mutableMapClass.parameterizedBy(context.keyTypeName, context.valueTypeName)
        },
        delegateProjection = { context ->
            PoetSymbols.winRtMutableMapProjectionClass.parameterizedBy(context.keyTypeName, context.valueTypeName)
        },
        delegateArguments = commonMapDelegateArguments + mutableMapDelegateArguments,
        operations = commonMapOperations + mutableMapOperations,
    )

    private val commonVectorDelegateArguments = listOf(
        ProjectionDelegateArgument<VectorProjectionContext>("sizeProvider") {
            CodeBlock.of("{ winRtSize.value.toInt() }")
        },
        ProjectionDelegateArgument("getter") {
            CodeBlock.of("{ index -> getAt(%T(index.toUInt())) }", PoetSymbols.uint32Class)
        },
    )

    private val mutableVectorDelegateArguments = listOf(
        ProjectionDelegateArgument<VectorProjectionContext>("append") {
            CodeBlock.of("{ value -> append(value) }")
        },
        ProjectionDelegateArgument("clearer") {
            CodeBlock.of("{ clear() }")
        },
    )

    private val commonVectorOperations = listOf(
        ProjectionOperation<VectorProjectionContext>(
            name = "getAt",
            parameters = { context -> vectorParameters(context, listOf(VectorParameter.INDEX)) },
            returnType = { context -> context.elementTypeName },
            body = { context -> vectorProjectedResultBody(context, "GetAt", 6, listOf(VectorParameter.INDEX)) },
        ),
        ProjectionOperation(
            name = "winRtIndexOf",
            parameters = { context -> vectorParameters(context, listOf(VectorParameter.VALUE)) },
            returnType = { PoetSymbols.uint32Class.copy(nullable = true) },
            body = { context -> vectorIndexOfBody(context, "IndexOf") },
        ),
    )

    private val mutableVectorOperations = listOf(
        ProjectionOperation<VectorProjectionContext>(
            name = "getView",
            returnType = { context ->
                projectedDeclarationClassName(context.type.namespace, "IVectorView")
                    .parameterizedBy(context.elementTypeName)
            },
            body = { context -> objectFactoryBody(context.type, "IVectorView", "GetView", 8, listOf(0)) },
        ),
        ProjectionOperation(
            name = "setAt",
            parameters = { context -> vectorParameters(context, listOf(VectorParameter.INDEX, VectorParameter.VALUE)) },
            body = { context -> vectorUnitBody(context, "SetAt", 10, listOf(VectorParameter.INDEX, VectorParameter.VALUE)) },
        ),
        ProjectionOperation(
            name = "insertAt",
            parameters = { context -> vectorParameters(context, listOf(VectorParameter.INDEX, VectorParameter.VALUE)) },
            body = { context -> vectorUnitBody(context, "InsertAt", 11, listOf(VectorParameter.INDEX, VectorParameter.VALUE)) },
        ),
        ProjectionOperation(
            name = "removeAt",
            parameters = { context -> vectorParameters(context, listOf(VectorParameter.INDEX)) },
            body = { context -> vectorRemoveAtBody(context, "RemoveAt", 12) },
        ),
        ProjectionOperation(
            name = "append",
            parameters = { context -> vectorParameters(context, listOf(VectorParameter.VALUE)) },
            body = { context -> vectorUnitBody(context, "Append", 13, listOf(VectorParameter.VALUE)) },
        ),
        ProjectionOperation(
            name = "removeAtEnd",
            body = { context -> unitBody(context.type, "RemoveAtEnd", 14) },
        ),
        ProjectionOperation(
            name = "clear",
            body = { context -> unitBody(context.type, "Clear", 15) },
        ),
    )

    private val readOnlyVectorProjectionDescriptor = VectorProjectionDescriptor(
        indexOfDefaultSlot = 8,
        superinterface = { context ->
            PoetSymbols.listClass.parameterizedBy(context.elementTypeName)
        },
        delegateProjection = { context ->
            PoetSymbols.winRtListProjectionClass.parameterizedBy(context.elementTypeName)
        },
        delegateArguments = commonVectorDelegateArguments,
        operations = commonVectorOperations,
    )

    private val mutableVectorProjectionDescriptor = VectorProjectionDescriptor(
        indexOfDefaultSlot = 9,
        superinterface = { context ->
            PoetSymbols.mutableListClass.parameterizedBy(context.elementTypeName)
        },
        delegateProjection = { context ->
            PoetSymbols.winRtMutableListProjectionClass.parameterizedBy(context.elementTypeName)
        },
        delegateArguments = commonVectorDelegateArguments + mutableVectorDelegateArguments,
        operations = commonVectorOperations + mutableVectorOperations,
    )

    private fun mapParameters(
        context: MapProjectionContext,
        parameters: List<MapParameter>,
    ): List<ParameterSpec> {
        return parameters.map { parameter ->
            ParameterSpec.builder(parameter.parameterName, parameter.typeName(context)).build()
        }
    }

    private fun vectorParameters(
        context: VectorProjectionContext,
        parameters: List<VectorParameter>,
    ): List<ParameterSpec> {
        return parameters.map { parameter ->
            ParameterSpec.builder(parameter.parameterName, parameter.typeName(context)).build()
        }
    }

    private fun mapProjectedResultBody(
        context: MapProjectionContext,
        slotName: String,
        defaultSlot: Int,
        returnArgIndex: Int,
        parameters: List<MapParameter>,
    ): CodeBlock {
        val call = CodeBlock.builder()
            .add(
                "projectedGenericMethodResult(%L, %L, %L",
                methodSlot(context.type, slotName, defaultSlot),
                genericArgSignature(returnArgIndex),
                genericArgProjectionTypeKey(returnArgIndex),
            )
            .apply {
                mapAbiArguments(parameters).forEach { argument ->
                    add(", %L", argument)
                }
            }
            .add(")")
            .build()
        return CodeBlock.of("return %L as %T\n", call, context.valueTypeName)
    }

    private fun mapBooleanResultBody(
        context: MapProjectionContext,
        slotName: String,
        defaultSlot: Int,
        parameters: List<MapParameter>,
    ): CodeBlock {
        val call = CodeBlock.builder()
            .add(
                "%M(projectedGenericMethodResult(%L, %S, %S",
                PoetSymbols.winRtBooleanMember,
                methodSlot(context.type, slotName, defaultSlot),
                "b1",
                "Boolean",
            )
            .apply {
                mapAbiArguments(parameters).forEach { argument ->
                    add(", %L", argument)
                }
            }
            .add(") as Boolean)")
            .build()
        return CodeBlock.of("return %L\n", call)
    }

    private fun objectFactoryBody(
        type: WinMdType,
        projectionName: String,
        slotName: String,
        defaultSlot: Int,
        genericArgumentIndexes: List<Int>,
    ): CodeBlock {
        val projectionClass = projectedDeclarationClassName(type.namespace, projectionName)
        val call = CodeBlock.builder()
            .add(
                "%T.from(%T(%T.invokeObjectMethod(pointer, %L).getOrThrow())",
                projectionClass,
                PoetSymbols.inspectableClass,
                PoetSymbols.platformComInteropClass,
                methodSlot(type, slotName, defaultSlot),
            )
            .apply {
                genericArgumentIndexes.forEach { index ->
                    add(", %L", genericArgSignature(index))
                }
                genericArgumentIndexes.forEach { index ->
                    add(", %L", genericArgProjectionTypeKey(index))
                }
            }
            .add(")")
            .build()
        return CodeBlock.of("return %L\n", call)
    }

    private fun mapUnitBody(
        context: MapProjectionContext,
        slotName: String,
        defaultSlot: Int,
        parameters: List<MapParameter>,
    ): CodeBlock {
        return CodeBlock.builder()
            .add(
                "%T.invokeUnitMethodWithArgs(pointer, %L",
                PoetSymbols.platformComInteropClass,
                methodSlot(context.type, slotName, defaultSlot),
            )
            .apply {
                mapAbiArguments(parameters).forEach { argument ->
                    add(", %L", argument)
                }
            }
            .add(").getOrThrow()\n")
            .build()
    }

    private fun vectorProjectedResultBody(
        context: VectorProjectionContext,
        slotName: String,
        defaultSlot: Int,
        parameters: List<VectorParameter>,
    ): CodeBlock {
        val call = CodeBlock.builder()
            .add(
                "projectedGenericMethodResult(%L, %L, %L",
                methodSlot(context.type, slotName, defaultSlot),
                genericArgSignature(0),
                genericArgProjectionTypeKey(0),
            )
            .apply {
                vectorAbiArguments(parameters).forEach { argument ->
                    add(", %L", argument)
                }
            }
            .add(")")
            .build()
        return CodeBlock.of("return %L as %T\n", call, context.elementTypeName)
    }

    private fun vectorIndexOfBody(
        context: VectorProjectionContext,
        slotName: String,
    ): CodeBlock {
        return CodeBlock.builder()
            .addStatement(
                "val (found, index) = %T.invokeIndexOfMethod(pointer, %L, %L).getOrThrow()",
                PoetSymbols.platformComInteropClass,
                methodSlot(context.type, slotName, context.indexOfDefaultSlot),
                vectorAbiArguments(listOf(VectorParameter.VALUE)).single(),
            )
            .addStatement("return if (found) %T(index) else null", PoetSymbols.uint32Class)
            .build()
    }

    private fun vectorUnitBody(
        context: VectorProjectionContext,
        slotName: String,
        defaultSlot: Int,
        parameters: List<VectorParameter>,
    ): CodeBlock {
        return CodeBlock.builder()
            .add(
                "%T.invokeUnitMethodWithArgs(pointer, %L",
                PoetSymbols.platformComInteropClass,
                methodSlot(context.type, slotName, defaultSlot),
            )
            .apply {
                vectorAbiArguments(parameters).forEach { argument ->
                    add(", %L", argument)
                }
            }
            .add(").getOrThrow()\n")
            .build()
    }

    private fun vectorRemoveAtBody(
        context: VectorProjectionContext,
        slotName: String,
        defaultSlot: Int,
    ): CodeBlock {
        return CodeBlock.of(
            "%T.invokeUnitMethodWithUInt32Arg(pointer, %L, index.value).getOrThrow()\n",
            PoetSymbols.platformComInteropClass,
            methodSlot(context.type, slotName, defaultSlot),
        )
    }

    private fun unitBody(
        type: WinMdType,
        slotName: String,
        defaultSlot: Int,
    ): CodeBlock {
        return CodeBlock.of(
            "%T.invokeUnitMethod(pointer, %L).getOrThrow()\n",
            PoetSymbols.platformComInteropClass,
            methodSlot(type, slotName, defaultSlot),
        )
    }

    private fun mapAbiArguments(parameters: List<MapParameter>): List<CodeBlock> {
        return parameters.map { parameter ->
            when (parameter) {
                MapParameter.KEY -> CodeBlock.of(
                    "projectedGenericArgument(key, genericArg0Signature, genericArg0ProjectionTypeKey)",
                )
                MapParameter.VALUE -> CodeBlock.of(
                    "projectedGenericArgument(value, genericArg1Signature, genericArg1ProjectionTypeKey)",
                )
            }
        }
    }

    private fun vectorAbiArguments(parameters: List<VectorParameter>): List<CodeBlock> {
        return parameters.map { parameter ->
            when (parameter) {
                VectorParameter.INDEX -> CodeBlock.of("index.value")
                VectorParameter.VALUE -> CodeBlock.of(
                    "projectedGenericArgument(value, genericArg0Signature, genericArg0ProjectionTypeKey)",
                )
            }
        }
    }

    private data class ProjectionDelegateArgument<C>(
        val name: String,
        val expression: (C) -> CodeBlock,
    )

    private data class ProjectionOperation<C>(
        val name: String,
        val modifiers: List<KModifier> = emptyList(),
        val parameters: (C) -> List<ParameterSpec> = { emptyList() },
        val returnType: ((C) -> TypeName)? = null,
        val body: (C) -> CodeBlock,
    )

    private enum class MapParameter(
        val parameterName: String,
    ) {
        KEY("key"),
        VALUE("value"),
        ;

        fun typeName(context: MapProjectionContext): TypeName {
            return when (this) {
                KEY -> context.keyTypeName
                VALUE -> context.valueTypeName
            }
        }
    }

    private data class MapProjectionContext(
        val type: WinMdType,
        val keyTypeName: TypeName,
        val valueTypeName: TypeName,
        val entriesExpression: CodeBlock,
    )

    private data class MapProjectionDescriptor(
        val superinterface: (MapProjectionContext) -> TypeName,
        val delegateProjection: (MapProjectionContext) -> TypeName,
        val delegateArguments: List<ProjectionDelegateArgument<MapProjectionContext>>,
        val operations: List<ProjectionOperation<MapProjectionContext>>,
    )

    private enum class VectorParameter(
        val parameterName: String,
    ) {
        INDEX("index"),
        VALUE("value"),
        ;

        fun typeName(context: VectorProjectionContext): TypeName {
            return when (this) {
                INDEX -> PoetSymbols.uint32Class
                VALUE -> context.elementTypeName
            }
        }
    }

    private data class VectorProjectionContext(
        val type: WinMdType,
        val elementTypeName: TypeName,
        val indexOfDefaultSlot: Int,
    )

    private data class VectorProjectionDescriptor(
        val indexOfDefaultSlot: Int,
        val superinterface: (VectorProjectionContext) -> TypeName,
        val delegateProjection: (VectorProjectionContext) -> TypeName,
        val delegateArguments: List<ProjectionDelegateArgument<VectorProjectionContext>>,
        val operations: List<ProjectionOperation<VectorProjectionContext>>,
    )

    private data class ClosedGenericRuntimeIterableElement(
        val elementTypeName: TypeName,
        val elementSignature: String,
        val elementProjectionTypeKey: String,
        val currentExpression: CodeBlock,
    )

    fun buildWinRtSizeProperty(slot: Int): PropertySpec {
        return PropertySpec.builder("winRtSize", PoetSymbols.uint32Class)
            .getter(
                FunSpec.getterBuilder()
                    .addStatement(
                        "return %T(%L)",
                        PoetSymbols.uint32Class,
                        AbiCallCatalog.uint32Method(slot),
                    )
                    .build(),
            )
            .build()
    }

    private fun methodSlot(
        type: WinMdType,
        name: String,
        default: Int,
    ): Int {
        return type.methods.firstOrNull { it.name == name }?.vtableIndex
            ?: type.methods.firstOrNull { it.name.equals(name, ignoreCase = true) }?.vtableIndex
            ?: default
    }

    private fun getterSlot(
        type: WinMdType,
        name: String,
        default: Int,
    ): Int {
        return type.properties.firstOrNull { it.name == name }?.getterVtableIndex
            ?: type.methods.firstOrNull { it.name == "get_$name" }?.vtableIndex
            ?: default
    }

    private fun genericStateProperties(type: WinMdType): List<PropertySpec> {
        return type.genericParameters.indices.flatMap { index ->
            listOf(
                PropertySpec.builder("genericArg${index}Signature", String::class)
                    .addModifiers(KModifier.PRIVATE)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement(
                                "return additionalTypeData[%S] as? String ?: error(%S)",
                                "generic:${type.namespace}.${type.name}:arg${index}:signature",
                                "Missing generic signature for ${type.namespace}.${type.name} arg${index}",
                            )
                            .build(),
                    )
                    .build(),
                PropertySpec.builder("genericArg${index}ProjectionTypeKey", String::class)
                    .addModifiers(KModifier.PRIVATE)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement(
                                "return additionalTypeData[%S] as? String ?: error(%S)",
                                "generic:${type.namespace}.${type.name}:arg${index}:projectionTypeKey",
                                "Missing projection type key for ${type.namespace}.${type.name} arg${index}",
                            )
                            .build(),
                    )
                    .build(),
            )
        }
    }

    private fun genericProjectionHelperFunctions(): List<FunSpec> {
        return listOf(
            FunSpec.builder("projectedGenericArgument")
                .addModifiers(KModifier.PRIVATE)
                .addParameter("value", Any::class)
                .addParameter("signature", String::class)
                .addParameter("projectionTypeKey", String::class)
                .returns(Any::class)
                .addStatement("return %M(value, signature, projectionTypeKey)", PoetSymbols.projectedGenericArgumentMember)
                .build(),
            FunSpec.builder("projectedGenericMethodResult")
                .addModifiers(KModifier.PRIVATE)
                .addParameter("vtableIndex", Int::class)
                .addParameter("signature", String::class)
                .addParameter("projectionTypeKey", String::class)
                .addParameter(
                    ParameterSpec.builder("arguments", Any::class)
                        .addModifiers(KModifier.VARARG)
                        .build(),
                )
                .returns(Any::class)
                .addStatement(
                    "return %M(pointer, vtableIndex, signature, projectionTypeKey, *arguments)",
                    PoetSymbols.projectedGenericMethodResultMember,
                )
                .build(),
        )
    }

    private fun mapEntryIterableExpression(
        rawIterableClass: ClassName,
        rawEntryClass: ClassName,
        keySignature: CodeBlock,
        valueSignature: CodeBlock,
        keyProjectionTypeKey: CodeBlock,
        valueProjectionTypeKey: CodeBlock,
    ): CodeBlock {
        return CodeBlock.of(
            "%T.from(%T(pointer), %T.signatureOf(%L, %L), %T.projectionTypeKeyOf(%L, %L))",
            rawIterableClass,
            PoetSymbols.inspectableClass,
            rawEntryClass,
            keySignature,
            valueSignature,
            rawEntryClass,
            keyProjectionTypeKey,
            valueProjectionTypeKey,
        )
    }

    private fun genericArgSignature(index: Int): CodeBlock = CodeBlock.of("genericArg%LSignature", index)

    private fun genericArgProjectionTypeKey(index: Int): CodeBlock = CodeBlock.of("genericArg%LProjectionTypeKey", index)

    private fun interfaceProjectionMetadata(
        qualifiedName: String,
        typeNameMapper: TypeNameMapper,
        winRtSignatureMapper: WinRtSignatureMapper,
        winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    ): CollectionInterfaceMetadata? =
        closedGenericCollectionMetadataDescriptors.firstNotNullOfOrNull { descriptor ->
            closedGenericCollectionMetadata(
                qualifiedName = qualifiedName,
                descriptor = descriptor,
                typeNameMapper = typeNameMapper,
                winRtSignatureMapper = winRtSignatureMapper,
                winRtProjectionTypeMapper = winRtProjectionTypeMapper,
            )
        } ?: xamlBindableCollectionMetadata(qualifiedName, typeNameMapper)

    private fun closedGenericCollectionMetadata(
        qualifiedName: String,
        descriptor: ClosedGenericCollectionMetadataDescriptor,
        typeNameMapper: TypeNameMapper,
        winRtSignatureMapper: WinRtSignatureMapper,
        winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    ): CollectionInterfaceMetadata? {
        val arguments = closedGenericArguments(
            qualifiedName = qualifiedName,
            rawQualifiedName = descriptor.rawQualifiedName,
            expectedCount = descriptor.argumentCount,
        ) ?: return null
        if (!descriptor.supportsTypeArguments(arguments)) {
            return null
        }
        val typeArguments = arguments.map { argument -> collectionElementTypeName(argument, typeNameMapper) }
        val rawInterfaceClass = typeNameMapper.mapTypeName(
            descriptor.rawQualifiedName,
            descriptor.rawQualifiedName.substringBeforeLast('.'),
        ) as ClassName
        return CollectionInterfaceMetadata(
            collectionSuperinterface = descriptor.collectionSuperinterface(typeArguments),
            delegateFactory = closedGenericInterfaceFactory(
                rawInterfaceClass = rawInterfaceClass,
                signatures = arguments.map { argument ->
                    winRtSignatureMapper.signatureFor(argument, "Windows.Foundation.Collections")
                },
                projectionTypeKeys = arguments.map { argument ->
                    winRtProjectionTypeMapper.projectionTypeKeyFor(argument, "Windows.Foundation.Collections")
                },
            ),
            winRtSizeSlot = descriptor.winRtSizeSlot,
        )
    }

    private fun xamlBindableCollectionMetadata(
        qualifiedName: String,
        typeNameMapper: TypeNameMapper,
    ): CollectionInterfaceMetadata? {
        val namespace = qualifiedName.substringBeforeLast('.', "")
        val descriptor = xamlBindableDescriptor(namespace, qualifiedName.substringAfterLast('.')) ?: return null
        if (descriptor.collectionDelegateProjection == null) {
            return null
        }
        return CollectionInterfaceMetadata(
            collectionSuperinterface = descriptor.collectionSuperinterface,
            delegateFactory = CodeBlock.of(
                "%T.from(%T(pointer))",
                typeNameMapper.mapTypeName(qualifiedName, namespace) as ClassName,
                PoetSymbols.inspectableClass,
            ),
            winRtSizeSlot = descriptor.winRtSizeSlot,
            extraFunctions = bindableCollectionExtraFunctions(),
        )
    }

    private fun bindableCollectionExtraFunctions(): List<FunSpec> = listOf(
        FunSpec.builder("contains")
            .addParameter("element", PoetSymbols.inspectableClass)
            .returns(Boolean::class)
            .addStatement("return indexOf(element) >= 0")
            .build(),
        FunSpec.builder("containsAll")
            .addParameter("elements", Collection::class.asTypeName().parameterizedBy(PoetSymbols.inspectableClass))
            .returns(Boolean::class)
            .addStatement("return elements.all { contains(it) }")
            .build(),
    )

    private fun closedGenericArguments(
        qualifiedName: String,
        rawQualifiedName: String,
        expectedCount: Int,
    ): List<String>? {
        if (!qualifiedName.startsWith("$rawQualifiedName<") || !qualifiedName.endsWith(">")) {
            return null
        }
        return splitGenericArguments(
            qualifiedName.substringAfter('<').substringBeforeLast('>'),
        ).takeIf { it.size == expectedCount }
    }

    private fun closedGenericInterfaceFactory(
        rawInterfaceClass: ClassName,
        signatures: List<String>,
        projectionTypeKeys: List<String>,
    ): CodeBlock {
        return CodeBlock.builder()
            .add("%T.from(%T(pointer)", rawInterfaceClass, PoetSymbols.inspectableClass)
            .apply {
                signatures.forEach { signature ->
                    add(", %S", signature)
                }
                projectionTypeKeys.forEach { projectionTypeKey ->
                    add(", %S", projectionTypeKey)
                }
            }
            .add(")")
            .build()
    }

    private fun closedGenericIterableProjection(
        qualifiedName: String,
        typeNameMapper: TypeNameMapper,
        winRtSignatureMapper: WinRtSignatureMapper,
        winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    ): RuntimeIterableProjection? {
        closedGenericRuntimeIterableElement(
            qualifiedName = qualifiedName,
            rawQualifiedName = "Windows.Foundation.Collections.IIterable",
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { element ->
            val rawIterableClass = typeNameMapper.mapTypeName(
                "Windows.Foundation.Collections.IIterable",
                "Windows.Foundation.Collections",
            ) as ClassName
            val rawIteratorClass = typeNameMapper.mapTypeName(
                "Windows.Foundation.Collections.IIterator",
                "Windows.Foundation.Collections",
            ) as ClassName
            return RuntimeIterableProjection(
                superinterface = PoetSymbols.iterableClass.parameterizedBy(element.elementTypeName),
                delegateFactory = CodeBlock.of(
                    "object : %T {\n" +
                        "  override fun iterator(): %T = %L\n" +
                        "}",
                    PoetSymbols.iterableClass.parameterizedBy(element.elementTypeName),
                    PoetSymbols.iteratorClass.parameterizedBy(element.elementTypeName),
                    closedGenericIteratorProjectionAdapter(
                        element = element,
                        iteratorProjectionInitializer = CodeBlock.of(
                            "%T.from(%T(%T.invokeObjectMethod(%T.from(%T(pointer), %S, %S).pointer, 6).getOrThrow()), %S, %S)",
                            rawIteratorClass,
                            PoetSymbols.inspectableClass,
                            PoetSymbols.platformComInteropClass,
                            rawIterableClass,
                            PoetSymbols.inspectableClass,
                            element.elementSignature,
                            element.elementProjectionTypeKey,
                            element.elementSignature,
                            element.elementProjectionTypeKey,
                        ),
                    ),
                ),
            )
        }
        closedGenericRuntimeIterableElement(
            qualifiedName = qualifiedName,
            rawQualifiedName = "Windows.Foundation.Collections.IIterator",
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { element ->
            val rawIteratorClass = typeNameMapper.mapTypeName(
                "Windows.Foundation.Collections.IIterator",
                "Windows.Foundation.Collections",
            ) as ClassName
            return RuntimeIterableProjection(
                superinterface = PoetSymbols.iteratorClass.parameterizedBy(element.elementTypeName),
                delegateFactory = closedGenericIteratorProjectionAdapter(
                    element = element,
                    iteratorProjectionInitializer = CodeBlock.of(
                        "%T.from(%T(pointer), %S, %S)",
                        rawIteratorClass,
                        PoetSymbols.inspectableClass,
                        element.elementSignature,
                        element.elementProjectionTypeKey,
                    ),
                ),
            )
        }
        return null
    }

    private fun xamlBindableDescriptor(namespace: String, name: String): XamlBindableDescriptor? =
        xamlBindableDescriptors.firstOrNull { isXamlBindableInteropNamespace(namespace) && it.simpleName == name }

    private fun xamlBindableRuntimeIterableSuperinterface(qualifiedName: String): TypeName? =
        xamlBindableDescriptor(qualifiedName.substringBeforeLast('.', ""), qualifiedName.substringAfterLast('.'))
            ?.takeIf { it.collectionDelegateProjection == null }
            ?.collectionSuperinterface

    private fun closedGenericRuntimeIterableElement(
        qualifiedName: String,
        rawQualifiedName: String,
        typeNameMapper: TypeNameMapper,
        winRtSignatureMapper: WinRtSignatureMapper,
        winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    ): ClosedGenericRuntimeIterableElement? {
        val elementType = closedGenericArguments(qualifiedName, rawQualifiedName, 1)?.singleOrNull() ?: return null
        if (!supportsClosedGenericCollectionElement(elementType)) {
            return null
        }
        val elementTypeName = collectionElementTypeName(elementType, typeNameMapper)
        return ClosedGenericRuntimeIterableElement(
            elementTypeName = elementTypeName,
            elementSignature = winRtSignatureMapper.signatureFor(elementType, "Windows.Foundation.Collections"),
            elementProjectionTypeKey = winRtProjectionTypeMapper.projectionTypeKeyFor(elementType, "Windows.Foundation.Collections"),
            currentExpression = if (isKeyValuePairCollectionElement(elementType)) {
                keyValuePairEntryExpression(elementType, "iteratorProjection.pointer")
            } else {
                elementReadExpression(elementTypeName, "iteratorProjection.pointer", 6)
            },
        )
    }

    private fun closedGenericIteratorProjectionAdapter(
        element: ClosedGenericRuntimeIterableElement,
        iteratorProjectionInitializer: CodeBlock,
    ): CodeBlock {
        return CodeBlock.of(
            "object : %T {\n" +
                "  private val iteratorProjection = %L\n" +
                "  override fun hasNext(): Boolean = %T(%L).value\n" +
                "  override fun next(): %T {\n" +
                "    if (!hasNext()) throw %T()\n" +
                "    val current = %L\n" +
                "    %T.invokeBooleanGetter(iteratorProjection.pointer, 8).getOrThrow()\n" +
                "    return current\n" +
                "  }\n" +
                "}",
            PoetSymbols.iteratorClass.parameterizedBy(element.elementTypeName),
            iteratorProjectionInitializer,
            PoetSymbols.winRtBooleanClass,
            AbiCallCatalog.booleanGetter(7, "iteratorProjection.pointer"),
            element.elementTypeName,
            NoSuchElementException::class,
            element.currentExpression,
            PoetSymbols.platformComInteropClass,
        )
    }

    private fun isXamlBindableInteropNamespace(namespace: String): Boolean =
        namespace == "Microsoft.UI.Xaml.Interop" || namespace == "Windows.UI.Xaml.Interop"

    private fun elementReadExpression(
        elementTypeName: TypeName,
        pointerExpression: String,
        slot: Int,
    ): CodeBlock {
        return if (elementTypeName == String::class.asTypeName()) {
            HStringSupport.toKotlinString(pointerExpression, slot)
        } else if (elementTypeName == Boolean::class.asTypeName()) {
            CodeBlock.of("%T(%L).value", PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanGetter(slot, pointerExpression))
        } else if (elementTypeName == Int::class.asTypeName()) {
            CodeBlock.of(
                "%T.invokeInt32Method(%L, %L).getOrThrow()",
                PoetSymbols.platformComInteropClass,
                pointerExpression,
                slot,
            )
        } else if (elementTypeName == UInt::class.asTypeName()) {
            CodeBlock.of(
                "%T.invokeUInt32Method(%L, %L).getOrThrow()",
                PoetSymbols.platformComInteropClass,
                pointerExpression,
                slot,
            )
        } else if (elementTypeName == Long::class.asTypeName()) {
            CodeBlock.of(
                "%T.invokeInt64Getter(%L, %L).getOrThrow()",
                PoetSymbols.platformComInteropClass,
                pointerExpression,
                slot,
            )
        } else if (elementTypeName == ULong::class.asTypeName()) {
            CodeBlock.of(
                "%T(%T.invokeInt64Getter(%L, %L).getOrThrow().toULong())",
                ULong::class,
                PoetSymbols.platformComInteropClass,
                pointerExpression,
                slot,
            )
        } else if (elementTypeName == Float::class.asTypeName()) {
            CodeBlock.of(
                "%T.invokeFloat32Method(%L, %L).getOrThrow()",
                PoetSymbols.platformComInteropClass,
                pointerExpression,
                slot,
            )
        } else if (elementTypeName == Double::class.asTypeName()) {
            CodeBlock.of(
                "%T.invokeFloat64Method(%L, %L).getOrThrow()",
                PoetSymbols.platformComInteropClass,
                pointerExpression,
                slot,
            )
        } else if (elementTypeName == PoetSymbols.dateTimeClass) {
            CodeBlock.of(
                "%T.fromEpochSeconds((%T.invokeInt64Getter(%L, %L).getOrThrow() - %L) / 10000000L, ((%T.invokeInt64Getter(%L, %L).getOrThrow() - %L) %% 10000000L * 100).toInt())",
                PoetSymbols.dateTimeClass,
                PoetSymbols.platformComInteropClass,
                pointerExpression,
                slot,
                WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
                PoetSymbols.platformComInteropClass,
                pointerExpression,
                slot,
                WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
            )
        } else if (elementTypeName == PoetSymbols.timeSpanClass) {
            CodeBlock.of(
                "%T(%T.invokeInt64Getter(%L, %L).getOrThrow())",
                PoetSymbols.timeSpanClass,
                PoetSymbols.platformComInteropClass,
                pointerExpression,
                slot,
            )
        } else {
            CodeBlock.of(
                "%T(%T.invokeObjectMethod(%L, %L).getOrThrow())",
                elementTypeName,
                PoetSymbols.platformComInteropClass,
                pointerExpression,
                slot,
            )
        }
    }

    private fun keyValuePairEntryExpression(elementType: String, pointerExpression: String): CodeBlock {
        val argumentSource = elementType.substringAfter('<').substringBeforeLast('>')
        val arguments = splitGenericArguments(argumentSource)
        require(arguments.size == 2) { "Invalid IKeyValuePair type: $elementType" }
        val keyTypeName = collectionElementTypeName(arguments[0], TypeNameMapper())
        val valueTypeName = collectionElementTypeName(arguments[1], TypeNameMapper())
        return CodeBlock.of(
            "object : %T {\n" +
                "  override val key: %T\n" +
                "    get() = %L\n" +
                "  override val value: %T\n" +
                "    get() = %L\n" +
                "}",
            PoetSymbols.mapEntryClass.parameterizedBy(keyTypeName, valueTypeName),
            keyTypeName,
            elementReadExpression(keyTypeName, pointerExpression, 6),
            valueTypeName,
            elementReadExpression(valueTypeName, pointerExpression, 7),
        )
    }

    private fun collectionElementTypeName(typeName: String, typeNameMapper: TypeNameMapper): TypeName {
        return when (typeName) {
            "Boolean" -> Boolean::class.asTypeName()
            "Int32" -> Int::class.asTypeName()
            "UInt32" -> UInt::class.asTypeName()
            "Int64" -> Long::class.asTypeName()
            "UInt64" -> ULong::class.asTypeName()
            "Float32" -> Float::class.asTypeName()
            "Float64" -> Double::class.asTypeName()
            "DateTime" -> PoetSymbols.dateTimeClass
            "TimeSpan" -> PoetSymbols.timeSpanClass
            else -> typeNameMapper.mapTypeName(typeName, "Windows.Foundation.Collections")
        }
    }
}

internal data class RuntimeCollectionProjection(
    val superinterface: TypeName,
    val delegateFactory: CodeBlock? = null,
    val winRtSizeSlot: Int? = null,
    val extraProperties: List<PropertySpec> = emptyList(),
    val extraFunctions: List<FunSpec> = emptyList(),
)

internal data class InterfaceCollectionProjection(
    val superinterface: TypeName,
    val delegateFactory: CodeBlock? = null,
    val winRtSizeSlot: Int? = null,
    val extraProperties: List<PropertySpec> = emptyList(),
    val extraFunctions: List<FunSpec> = emptyList(),
)

internal data class RuntimeIterableProjection(
    val superinterface: TypeName,
    val delegateFactory: CodeBlock,
)

internal data class CollectionInterfaceMetadata(
    val collectionSuperinterface: TypeName,
    val delegateFactory: CodeBlock? = null,
    val winRtSizeSlot: Int? = null,
    val extraProperties: List<PropertySpec> = emptyList(),
    val extraFunctions: List<FunSpec> = emptyList(),
)

private data class ClosedGenericCollectionMetadataDescriptor(
    val rawQualifiedName: String,
    val argumentCount: Int,
    val collectionSuperinterface: (List<TypeName>) -> TypeName,
    val supportsTypeArguments: (List<String>) -> Boolean = { true },
    val winRtSizeSlot: Int = 7,
)

private data class XamlBindableDescriptor(
    val simpleName: String,
    val collectionSuperinterface: TypeName,
    val collectionDelegateProjection: TypeName? = null,
    val collectionDelegateArguments: List<Pair<String, CodeBlock>> = emptyList(),
    val extraProperties: List<PropertySpec> = emptyList(),
    val extraFunctions: List<FunSpec> = emptyList(),
    val winRtSizeSlot: Int? = null,
)

private val closedGenericCollectionMetadataDescriptors = listOf(
    mapCollectionMetadataDescriptor("Windows.Foundation.Collections.IMapView", PoetSymbols.mapClass),
    mapCollectionMetadataDescriptor("Windows.Foundation.Collections.IObservableMap", PoetSymbols.mutableMapClass),
    mapCollectionMetadataDescriptor("Windows.Foundation.Collections.IMap", PoetSymbols.mutableMapClass),
    vectorCollectionMetadataDescriptor("Windows.Foundation.Collections.IObservableVector", PoetSymbols.mutableListClass),
    vectorCollectionMetadataDescriptor("Windows.Foundation.Collections.IVectorView", PoetSymbols.listClass),
    vectorCollectionMetadataDescriptor("Windows.Foundation.Collections.IVector", PoetSymbols.mutableListClass),
)

private val xamlBindableDescriptors = listOf(
    xamlBindableVectorDescriptor(
        simpleName = "IBindableVector",
        collectionSuperinterface = PoetSymbols.mutableListClass.parameterizedBy(PoetSymbols.inspectableClass),
        collectionDelegateProjection = PoetSymbols.winRtMutableListProjectionClass.parameterizedBy(PoetSymbols.inspectableClass),
        mutable = true,
    ),
    xamlBindableVectorDescriptor(
        simpleName = "IBindableVectorView",
        collectionSuperinterface = PoetSymbols.listClass.parameterizedBy(PoetSymbols.inspectableClass),
        collectionDelegateProjection = PoetSymbols.winRtListProjectionClass.parameterizedBy(PoetSymbols.inspectableClass),
        mutable = false,
    ),
    xamlBindableIterableDescriptor(
        simpleName = "IBindableIterable",
        superinterface = PoetSymbols.iterableClass.parameterizedBy(PoetSymbols.inspectableClass),
        extraFunctions = listOf(iterableIteratorOverrideFunction(PoetSymbols.inspectableClass)),
    ),
    xamlBindableIterableDescriptor(
        simpleName = "IBindableIterator",
        superinterface = PoetSymbols.iteratorClass.parameterizedBy(PoetSymbols.inspectableClass),
        extraProperties = listOf(
            PropertySpec.builder("winRtCurrent", PoetSymbols.inspectableClass)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement(
                            "return %T(%T.invokeObjectMethod(pointer, 6).getOrThrow())",
                            PoetSymbols.inspectableClass,
                            PoetSymbols.platformComInteropClass,
                        )
                        .build(),
                )
                .build(),
            winRtHasCurrentProperty(
                CodeBlock.of("return %M(%L)\n", PoetSymbols.winRtBooleanMember, AbiCallCatalog.booleanGetter(7)),
            ),
        ),
        extraFunctions = iteratorTraversalFunctions(PoetSymbols.inspectableClass),
    ),
)

private fun mapCollectionMetadataDescriptor(
    rawQualifiedName: String,
    collectionSuperinterface: ClassName,
) = ClosedGenericCollectionMetadataDescriptor(
    rawQualifiedName = rawQualifiedName,
    argumentCount = 2,
    collectionSuperinterface = { arguments ->
        collectionSuperinterface.parameterizedBy(arguments[0], arguments[1])
    },
)

private fun bindableVectorDelegateArguments(mutable: Boolean): List<Pair<String, CodeBlock>> = buildList {
    add(
        "sizeProvider" to CodeBlock.of(
            "{ %T(%L).value.toInt() }",
            PoetSymbols.uint32Class,
            AbiCallCatalog.uint32Method(8),
        ),
    )
    add(
        "getter" to CodeBlock.of(
            "{ index -> %T(%L) }",
            PoetSymbols.inspectableClass,
            AbiCallCatalog.objectMethodWithUInt32(7, "index.toUInt()"),
        ),
    )
    if (mutable) {
        add("append" to CodeBlock.of("{ value -> %L }", AbiCallCatalog.objectSetter(14, "value")))
        add("clearer" to CodeBlock.of("{ %L }", AbiCallCatalog.unitMethod(16)))
    }
}

private fun iterableIteratorOverrideFunction(elementTypeName: TypeName): FunSpec =
    FunSpec.builder("iterator")
        .addModifiers(KModifier.OVERRIDE)
        .returns(PoetSymbols.iteratorClass.parameterizedBy(elementTypeName))
        .addStatement("return first()")
        .build()

private fun winRtHasCurrentProperty(getterCode: CodeBlock): PropertySpec =
    PropertySpec.builder("winRtHasCurrent", PoetSymbols.winRtBooleanClass)
        .getter(FunSpec.getterBuilder().addCode(getterCode).build())
        .build()

private fun iteratorTraversalFunctions(elementTypeName: TypeName): List<FunSpec> = listOf(
    FunSpec.builder("hasNext")
        .addModifiers(KModifier.OVERRIDE)
        .returns(Boolean::class)
        .addStatement("return winRtHasCurrent.value")
        .build(),
    FunSpec.builder("next")
        .addModifiers(KModifier.OVERRIDE)
        .returns(elementTypeName)
        .beginControlFlow("if (!hasNext())")
        .addStatement("throw %T()", NoSuchElementException::class)
        .endControlFlow()
        .addStatement("val current = winRtCurrent")
        .addStatement("moveNext()")
        .addStatement("return current")
        .build(),
)

private fun xamlBindableVectorDescriptor(simpleName: String, collectionSuperinterface: TypeName, collectionDelegateProjection: TypeName, mutable: Boolean) = XamlBindableDescriptor(
    simpleName = simpleName,
    collectionSuperinterface = collectionSuperinterface,
    collectionDelegateProjection = collectionDelegateProjection,
    collectionDelegateArguments = bindableVectorDelegateArguments(mutable),
    winRtSizeSlot = 8,
)

private fun xamlBindableIterableDescriptor(
    simpleName: String,
    superinterface: TypeName,
    extraProperties: List<PropertySpec> = emptyList(),
    extraFunctions: List<FunSpec> = emptyList(),
) = XamlBindableDescriptor(
    simpleName = simpleName,
    collectionSuperinterface = superinterface,
    extraProperties = extraProperties,
    extraFunctions = extraFunctions,
)

private fun vectorCollectionMetadataDescriptor(
    rawQualifiedName: String,
    collectionSuperinterface: ClassName,
) = ClosedGenericCollectionMetadataDescriptor(
    rawQualifiedName = rawQualifiedName,
    argumentCount = 1,
    collectionSuperinterface = { arguments ->
        collectionSuperinterface.parameterizedBy(arguments.single())
    },
    supportsTypeArguments = { arguments -> supportsClosedGenericCollectionElement(arguments.single()) },
)

private fun supportsClosedGenericCollectionElement(typeName: String): Boolean =
    typeName == "String" ||
        typeName == "Boolean" ||
        typeName == "Int32" ||
        typeName == "UInt32" ||
        typeName == "Int64" ||
        typeName == "UInt64" ||
        typeName == "Float32" ||
        typeName == "Float64" ||
        typeName == "DateTime" ||
        typeName == "TimeSpan" ||
        (
            (typeName == "Object" || typeName.contains('.')) &&
                !typeName.endsWith("[]") &&
                (typeName.indexOf('<') < 0 || isKeyValuePairCollectionElement(typeName))
        )

private fun isKeyValuePairCollectionElement(typeName: String): Boolean =
    typeName.startsWith("Windows.Foundation.Collections.IKeyValuePair<") && typeName.endsWith(">")
