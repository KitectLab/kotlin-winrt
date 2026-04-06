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
            .firstOrNull {
                it == "Microsoft.UI.Xaml.Interop.IBindableIterable" ||
                    it == "Microsoft.UI.Xaml.Interop.IBindableIterator" ||
                    it == "Windows.UI.Xaml.Interop.IBindableIterable" ||
                    it == "Windows.UI.Xaml.Interop.IBindableIterator" ||
                    it.startsWith("Windows.Foundation.Collections.IIterable<") ||
                    it.startsWith("Windows.Foundation.Collections.IIterator<")
            }
            ?: return null
        return when (iterableInterface) {
            "Microsoft.UI.Xaml.Interop.IBindableIterable",
            "Windows.UI.Xaml.Interop.IBindableIterable",
            -> RuntimeIterableProjection(
                superinterface = PoetSymbols.iterableClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer))",
                    typeNameMapper.mapTypeName(iterableInterface, iterableInterface.substringBeforeLast('.')) as ClassName,
                    PoetSymbols.inspectableClass,
                ),
            )
            "Microsoft.UI.Xaml.Interop.IBindableIterator",
            "Windows.UI.Xaml.Interop.IBindableIterator",
            -> RuntimeIterableProjection(
                superinterface = PoetSymbols.iteratorClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer))",
                    typeNameMapper.mapTypeName(iterableInterface, iterableInterface.substringBeforeLast('.')) as ClassName,
                    PoetSymbols.inspectableClass,
                ),
            )
            else -> closedGenericIterableProjection(
                qualifiedName = iterableInterface,
                typeNameMapper = typeNameMapper,
                winRtSignatureMapper = winRtSignatureMapper,
                winRtProjectionTypeMapper = winRtProjectionTypeMapper,
            )
        }
    }

    fun interfaceProjection(type: WinMdType): InterfaceCollectionProjection? {
        windowsFoundationCollectionProjection(type)?.let { return it }
        if (isXamlBindableInteropNamespace(type.namespace) && type.name == "IBindableVector") {
            return InterfaceCollectionProjection(
                superinterface = PoetSymbols.mutableListClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { %T(%L).value.toInt() }, getter = { index -> %T(%L) }, append = { value -> %L }, clearer = { %L })",
                    PoetSymbols.winRtMutableListProjectionClass.parameterizedBy(PoetSymbols.inspectableClass),
                    PoetSymbols.uint32Class,
                    AbiCallCatalog.uint32Method(8),
                    PoetSymbols.inspectableClass,
                    AbiCallCatalog.objectMethodWithUInt32(7, "index.toUInt()"),
                    AbiCallCatalog.objectSetter(14, "value"),
                    AbiCallCatalog.unitMethod(16),
                ),
                winRtSizeSlot = 8,
            )
        }
        if (isXamlBindableInteropNamespace(type.namespace) && type.name == "IBindableVectorView") {
            return InterfaceCollectionProjection(
                superinterface = PoetSymbols.listClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { %T(%L).value.toInt() }, getter = { index -> %T(%L) })",
                    PoetSymbols.winRtListProjectionClass.parameterizedBy(PoetSymbols.inspectableClass),
                    PoetSymbols.uint32Class,
                    AbiCallCatalog.uint32Method(8),
                    PoetSymbols.inspectableClass,
                    AbiCallCatalog.objectMethodWithUInt32(7, "index.toUInt()"),
                ),
                winRtSizeSlot = 8,
            )
        }
        return null
    }

    private fun windowsFoundationCollectionProjection(type: WinMdType): InterfaceCollectionProjection? {
        if (type.namespace != "Windows.Foundation.Collections") {
            return null
        }
        return when (type.name) {
            "IIterable`1" -> iterableInterfaceProjection(type)
            "IIterator`1" -> iteratorInterfaceProjection(type)
            "IKeyValuePair`2" -> keyValuePairInterfaceProjection(type)
            "IMapView`2" -> mapViewInterfaceProjection(type)
            "IMap`2",
            "IObservableMap`2",
            -> mutableMapInterfaceProjection(type)
            "IVectorView`1" -> vectorViewInterfaceProjection(type)
            "IVector`1",
            "IObservableVector`1",
            -> mutableVectorInterfaceProjection(type)
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
                FunSpec.builder("iterator")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(PoetSymbols.iteratorClass.parameterizedBy(elementTypeName))
                    .addStatement("return first()")
                    .build(),
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
                PropertySpec.builder("winRtHasCurrent", PoetSymbols.winRtBooleanClass)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement(
                                "return %M(%T.invokeBooleanGetter(pointer, %L).getOrThrow())",
                                PoetSymbols.winRtBooleanMember,
                                PoetSymbols.platformComInteropClass,
                                getterSlot(type, "HasCurrent", 7),
                            )
                            .build(),
                    )
                    .build(),
            ),
            extraFunctions = listOf(
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
            ),
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

    private fun mapViewInterfaceProjection(type: WinMdType): InterfaceCollectionProjection? {
        val (keyTypeName, valueTypeName) = genericTypeVariables(type, 2)?.let { it[0] to it[1] } ?: return null
        return genericCollectionProjection(
            type = type,
            superinterface = PoetSymbols.mapClass.parameterizedBy(keyTypeName, valueTypeName),
            delegateFactory = readOnlyMapDelegateFactory(type, keyTypeName, valueTypeName),
            winRtSizeSlot = 7,
            extraFunctions = listOf(
                FunSpec.builder("lookup")
                    .addParameter("key", keyTypeName)
                    .returns(valueTypeName)
                    .addStatement(
                        "return projectedGenericMethodResult(%L, genericArg1Signature, genericArg1ProjectionTypeKey, projectedGenericArgument(key, genericArg0Signature, genericArg0ProjectionTypeKey)) as %T",
                        methodSlot(type, "Lookup", 6),
                        valueTypeName,
                    )
                    .build(),
                FunSpec.builder("hasKey")
                    .addParameter("key", keyTypeName)
                    .returns(PoetSymbols.winRtBooleanClass)
                    .addStatement(
                        "return %M(projectedGenericMethodResult(%L, %S, %S, projectedGenericArgument(key, genericArg0Signature, genericArg0ProjectionTypeKey)) as Boolean)",
                        PoetSymbols.winRtBooleanMember,
                        methodSlot(type, "HasKey", 8),
                        "b1",
                        "Boolean",
                    )
                    .build(),
            ),
        )
    }

    private fun mutableMapInterfaceProjection(type: WinMdType): InterfaceCollectionProjection? {
        val (keyTypeName, valueTypeName) = genericTypeVariables(type, 2)?.let { it[0] to it[1] } ?: return null
        return genericCollectionProjection(
            type = type,
            superinterface = PoetSymbols.mutableMapClass.parameterizedBy(keyTypeName, valueTypeName),
            delegateFactory = mutableMapDelegateFactory(type, keyTypeName, valueTypeName),
            winRtSizeSlot = 7,
            extraFunctions = listOf(
                FunSpec.builder("lookup")
                    .addParameter("key", keyTypeName)
                    .returns(valueTypeName)
                    .addStatement(
                        "return projectedGenericMethodResult(%L, genericArg1Signature, genericArg1ProjectionTypeKey, projectedGenericArgument(key, genericArg0Signature, genericArg0ProjectionTypeKey)) as %T",
                        methodSlot(type, "Lookup", 6),
                        valueTypeName,
                    )
                    .build(),
                FunSpec.builder("hasKey")
                    .addParameter("key", keyTypeName)
                    .returns(PoetSymbols.winRtBooleanClass)
                    .addStatement(
                        "return %M(projectedGenericMethodResult(%L, %S, %S, projectedGenericArgument(key, genericArg0Signature, genericArg0ProjectionTypeKey)) as Boolean)",
                        PoetSymbols.winRtBooleanMember,
                        methodSlot(type, "HasKey", 8),
                        "b1",
                        "Boolean",
                    )
                    .build(),
                FunSpec.builder("getView")
                    .returns(projectedDeclarationClassName(type.namespace, "IMapView").parameterizedBy(keyTypeName, valueTypeName))
                    .addStatement(
                        "return %T.from(%T(%T.invokeObjectMethod(pointer, %L).getOrThrow()), genericArg0Signature, genericArg1Signature, genericArg0ProjectionTypeKey, genericArg1ProjectionTypeKey)",
                        projectedDeclarationClassName(type.namespace, "IMapView"),
                        PoetSymbols.inspectableClass,
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "GetView", 9),
                    )
                    .build(),
                FunSpec.builder("insert")
                    .addParameter("key", keyTypeName)
                    .addParameter("value", valueTypeName)
                    .returns(PoetSymbols.winRtBooleanClass)
                    .addStatement(
                        "return %M(projectedGenericMethodResult(%L, %S, %S, projectedGenericArgument(key, genericArg0Signature, genericArg0ProjectionTypeKey), projectedGenericArgument(value, genericArg1Signature, genericArg1ProjectionTypeKey)) as Boolean)",
                        PoetSymbols.winRtBooleanMember,
                        methodSlot(type, "Insert", 10),
                        "b1",
                        "Boolean",
                    )
                    .build(),
                FunSpec.builder("winRtRemoveKey")
                    .addModifiers(KModifier.PROTECTED)
                    .addParameter("key", keyTypeName)
                    .addStatement(
                        "%T.invokeUnitMethodWithArgs(pointer, %L, projectedGenericArgument(key, genericArg0Signature, genericArg0ProjectionTypeKey)).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "Remove", 11),
                    )
                    .build(),
                FunSpec.builder("clear")
                    .addStatement(
                        "%T.invokeUnitMethod(pointer, %L).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "Clear", 12),
                    )
                    .build(),
            ),
        )
    }

    private fun vectorViewInterfaceProjection(type: WinMdType): InterfaceCollectionProjection? {
        val elementTypeName = genericTypeVariables(type, 1)?.singleOrNull() ?: return null
        return genericCollectionProjection(
            type = type,
            superinterface = PoetSymbols.listClass.parameterizedBy(elementTypeName),
            delegateFactory = readOnlyVectorDelegateFactory(elementTypeName),
            winRtSizeSlot = 7,
            extraFunctions = listOf(
                FunSpec.builder("getAt")
                    .addParameter("index", PoetSymbols.uint32Class)
                    .returns(elementTypeName)
                    .addStatement(
                        "return projectedGenericMethodResult(%L, genericArg0Signature, genericArg0ProjectionTypeKey, index.value) as %T",
                        methodSlot(type, "GetAt", 6),
                        elementTypeName,
                    )
                    .build(),
                FunSpec.builder("winRtIndexOf")
                    .addParameter("value", elementTypeName)
                    .returns(PoetSymbols.uint32Class.copy(nullable = true))
                    .addStatement(
                        "val (found, index) = %T.invokeIndexOfMethod(pointer, %L, projectedGenericArgument(value, genericArg0Signature, genericArg0ProjectionTypeKey)).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "IndexOf", 8),
                    )
                    .addStatement("return if (found) %T(index) else null", PoetSymbols.uint32Class)
                    .build(),
            ),
        )
    }

    private fun mutableVectorInterfaceProjection(type: WinMdType): InterfaceCollectionProjection? {
        val elementTypeName = genericTypeVariables(type, 1)?.singleOrNull() ?: return null
        return genericCollectionProjection(
            type = type,
            superinterface = PoetSymbols.mutableListClass.parameterizedBy(elementTypeName),
            delegateFactory = mutableVectorDelegateFactory(elementTypeName),
            winRtSizeSlot = 7,
            extraFunctions = listOf(
                FunSpec.builder("getAt")
                    .addParameter("index", PoetSymbols.uint32Class)
                    .returns(elementTypeName)
                    .addStatement(
                        "return projectedGenericMethodResult(%L, genericArg0Signature, genericArg0ProjectionTypeKey, index.value) as %T",
                        methodSlot(type, "GetAt", 6),
                        elementTypeName,
                    )
                    .build(),
                FunSpec.builder("getView")
                    .returns(projectedDeclarationClassName(type.namespace, "IVectorView").parameterizedBy(elementTypeName))
                    .addStatement(
                        "return %T.from(%T(%T.invokeObjectMethod(pointer, %L).getOrThrow()), genericArg0Signature, genericArg0ProjectionTypeKey)",
                        projectedDeclarationClassName(type.namespace, "IVectorView"),
                        PoetSymbols.inspectableClass,
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "GetView", 8),
                    )
                    .build(),
                FunSpec.builder("winRtIndexOf")
                    .addParameter("value", elementTypeName)
                    .returns(PoetSymbols.uint32Class.copy(nullable = true))
                    .addStatement(
                        "val (found, index) = %T.invokeIndexOfMethod(pointer, %L, projectedGenericArgument(value, genericArg0Signature, genericArg0ProjectionTypeKey)).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "IndexOf", 9),
                    )
                    .addStatement("return if (found) %T(index) else null", PoetSymbols.uint32Class)
                    .build(),
                FunSpec.builder("setAt")
                    .addParameter("index", PoetSymbols.uint32Class)
                    .addParameter("value", elementTypeName)
                    .addStatement(
                        "%T.invokeUnitMethodWithArgs(pointer, %L, index.value, projectedGenericArgument(value, genericArg0Signature, genericArg0ProjectionTypeKey)).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "SetAt", 10),
                    )
                    .build(),
                FunSpec.builder("insertAt")
                    .addParameter("index", PoetSymbols.uint32Class)
                    .addParameter("value", elementTypeName)
                    .addStatement(
                        "%T.invokeUnitMethodWithArgs(pointer, %L, index.value, projectedGenericArgument(value, genericArg0Signature, genericArg0ProjectionTypeKey)).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "InsertAt", 11),
                    )
                    .build(),
                FunSpec.builder("removeAt")
                    .addParameter("index", PoetSymbols.uint32Class)
                    .addStatement(
                        "%T.invokeUnitMethodWithUInt32Arg(pointer, %L, index.value).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "RemoveAt", 12),
                    )
                    .build(),
                FunSpec.builder("append")
                    .addParameter("value", elementTypeName)
                    .addStatement(
                        "%T.invokeUnitMethodWithArgs(pointer, %L, projectedGenericArgument(value, genericArg0Signature, genericArg0ProjectionTypeKey)).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "Append", 13),
                    )
                    .build(),
                FunSpec.builder("removeAtEnd")
                    .addStatement(
                        "%T.invokeUnitMethod(pointer, %L).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "RemoveAtEnd", 14),
                    )
                    .build(),
                FunSpec.builder("clear")
                    .addStatement(
                        "%T.invokeUnitMethod(pointer, %L).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        methodSlot(type, "Clear", 15),
                    )
                    .build(),
            ),
        )
    }

    private fun mutableMapDelegateFactory(
        type: WinMdType,
        keyTypeName: TypeName,
        valueTypeName: TypeName,
    ): CodeBlock {
        return CodeBlock.of(
            "%T(sizeProvider = { winRtSize.value.toInt() }, lookupFn = { key -> lookup(key) }, containsKeyFn = { key -> hasKey(key).value }, putValueFn = { key, value -> insert(key, value).value }, removeKeyFn = { key -> winRtRemoveKey(key); true }, clearerFn = { clear() }, entriesProvider = { %L.asSequence().toList() })",
            PoetSymbols.winRtMutableMapProjectionClass.parameterizedBy(keyTypeName, valueTypeName),
            mapEntryIterableExpression(
                rawIterableClass = projectedDeclarationClassName(type.namespace, "IIterable"),
                rawEntryClass = projectedDeclarationClassName(type.namespace, "IKeyValuePair"),
                keySignature = genericArgSignature(0),
                valueSignature = genericArgSignature(1),
                keyProjectionTypeKey = genericArgProjectionTypeKey(0),
                valueProjectionTypeKey = genericArgProjectionTypeKey(1),
            ),
        )
    }

    private fun readOnlyMapDelegateFactory(
        type: WinMdType,
        keyTypeName: TypeName,
        valueTypeName: TypeName,
    ): CodeBlock {
        return CodeBlock.of(
            "%T(sizeProvider = { winRtSize.value.toInt() }, lookupFn = { key -> lookup(key) }, containsKeyFn = { key -> hasKey(key).value }, entriesProvider = { %L.asSequence().toList() })",
            PoetSymbols.winRtMapProjectionClass.parameterizedBy(keyTypeName, valueTypeName),
            mapEntryIterableExpression(
                rawIterableClass = projectedDeclarationClassName(type.namespace, "IIterable"),
                rawEntryClass = projectedDeclarationClassName(type.namespace, "IKeyValuePair"),
                keySignature = genericArgSignature(0),
                valueSignature = genericArgSignature(1),
                keyProjectionTypeKey = genericArgProjectionTypeKey(0),
                valueProjectionTypeKey = genericArgProjectionTypeKey(1),
            ),
        )
    }

    private fun readOnlyVectorDelegateFactory(elementTypeName: TypeName): CodeBlock {
        return CodeBlock.of(
            "%T(sizeProvider = { winRtSize.value.toInt() }, getter = { index -> getAt(%T(index.toUInt())) })",
            PoetSymbols.winRtListProjectionClass.parameterizedBy(elementTypeName),
            PoetSymbols.uint32Class,
        )
    }

    private fun mutableVectorDelegateFactory(elementTypeName: TypeName): CodeBlock {
        return CodeBlock.of(
            "%T(sizeProvider = { winRtSize.value.toInt() }, getter = { index -> getAt(%T(index.toUInt())) }, append = { value -> append(value) }, clearer = { clear() })",
            PoetSymbols.winRtMutableListProjectionClass.parameterizedBy(elementTypeName),
            PoetSymbols.uint32Class,
        )
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
    ): CollectionInterfaceMetadata? {
        closedGenericMapMetadata(
            qualifiedName = qualifiedName,
            rawQualifiedName = "Windows.Foundation.Collections.IMapView",
            collectionSuperinterfaceFactory = { keyType, valueType ->
                PoetSymbols.mapClass.parameterizedBy(keyType, valueType)
            },
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { return it }
        closedGenericMapMetadata(
            qualifiedName = qualifiedName,
            rawQualifiedName = "Windows.Foundation.Collections.IObservableMap",
            collectionSuperinterfaceFactory = { keyType, valueType ->
                PoetSymbols.mutableMapClass.parameterizedBy(keyType, valueType)
            },
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { return it }
        closedGenericMapMetadata(
            qualifiedName = qualifiedName,
            rawQualifiedName = "Windows.Foundation.Collections.IMap",
            collectionSuperinterfaceFactory = { keyType, valueType ->
                PoetSymbols.mutableMapClass.parameterizedBy(keyType, valueType)
            },
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { return it }
        closedGenericVectorMetadata(
            qualifiedName = qualifiedName,
            rawQualifiedName = "Windows.Foundation.Collections.IObservableVector",
            collectionSuperinterfaceFactory = { elementType ->
                PoetSymbols.mutableListClass.parameterizedBy(elementType)
            },
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { return it }
        closedGenericVectorMetadata(
            qualifiedName = qualifiedName,
            rawQualifiedName = "Windows.Foundation.Collections.IVectorView",
            collectionSuperinterfaceFactory = { elementType ->
                PoetSymbols.listClass.parameterizedBy(elementType)
            },
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { return it }
        closedGenericVectorMetadata(
            qualifiedName = qualifiedName,
            rawQualifiedName = "Windows.Foundation.Collections.IVector",
            collectionSuperinterfaceFactory = { elementType ->
                PoetSymbols.mutableListClass.parameterizedBy(elementType)
            },
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { return it }
        return when (qualifiedName) {
            "Microsoft.UI.Xaml.Interop.IBindableVector",
            "Windows.UI.Xaml.Interop.IBindableVector",
            -> CollectionInterfaceMetadata(
                collectionSuperinterface = PoetSymbols.mutableListClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer))",
                    typeNameMapper.mapTypeName(qualifiedName, qualifiedName.substringBeforeLast('.')) as ClassName,
                    PoetSymbols.inspectableClass,
                ),
                winRtSizeSlot = 8,
                extraFunctions = listOf(
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
                ),
            )
            "Microsoft.UI.Xaml.Interop.IBindableVectorView",
            "Windows.UI.Xaml.Interop.IBindableVectorView",
            -> CollectionInterfaceMetadata(
                collectionSuperinterface = PoetSymbols.listClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer))",
                    typeNameMapper.mapTypeName(qualifiedName, qualifiedName.substringBeforeLast('.')) as ClassName,
                    PoetSymbols.inspectableClass,
                ),
                winRtSizeSlot = 8,
                extraFunctions = listOf(
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
                ),
            )
            else -> null
        }
    }

    private fun closedGenericMapMetadata(
        qualifiedName: String,
        rawQualifiedName: String,
        collectionSuperinterfaceFactory: (TypeName, TypeName) -> TypeName,
        typeNameMapper: TypeNameMapper,
        winRtSignatureMapper: WinRtSignatureMapper,
        winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    ): CollectionInterfaceMetadata? {
        val arguments = closedGenericArguments(qualifiedName, rawQualifiedName, 2) ?: return null
        val keyType = collectionElementTypeName(arguments[0], typeNameMapper)
        val valueType = collectionElementTypeName(arguments[1], typeNameMapper)
        val rawInterfaceClass = typeNameMapper.mapTypeName(
            rawQualifiedName,
            rawQualifiedName.substringBeforeLast('.'),
        ) as ClassName
        return CollectionInterfaceMetadata(
            collectionSuperinterface = collectionSuperinterfaceFactory(keyType, valueType),
            delegateFactory = closedGenericInterfaceFactory(
                rawInterfaceClass = rawInterfaceClass,
                signatures = arguments.map { argument ->
                    winRtSignatureMapper.signatureFor(argument, "Windows.Foundation.Collections")
                },
                projectionTypeKeys = arguments.map { argument ->
                    winRtProjectionTypeMapper.projectionTypeKeyFor(argument, "Windows.Foundation.Collections")
                },
            ),
            winRtSizeSlot = 7,
        )
    }

    private fun closedGenericVectorMetadata(
        qualifiedName: String,
        rawQualifiedName: String,
        collectionSuperinterfaceFactory: (TypeName) -> TypeName,
        typeNameMapper: TypeNameMapper,
        winRtSignatureMapper: WinRtSignatureMapper,
        winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    ): CollectionInterfaceMetadata? {
        val elementType = closedGenericArguments(qualifiedName, rawQualifiedName, 1)?.singleOrNull() ?: return null
        if (!supportsClosedGenericVectorElement(elementType)) {
            return null
        }
        val rawInterfaceClass = typeNameMapper.mapTypeName(
            rawQualifiedName,
            rawQualifiedName.substringBeforeLast('.'),
        ) as ClassName
        return CollectionInterfaceMetadata(
            collectionSuperinterface = collectionSuperinterfaceFactory(collectionElementTypeName(elementType, typeNameMapper)),
            delegateFactory = closedGenericInterfaceFactory(
                rawInterfaceClass = rawInterfaceClass,
                signatures = listOf(winRtSignatureMapper.signatureFor(elementType, "Windows.Foundation.Collections")),
                projectionTypeKeys = listOf(
                    winRtProjectionTypeMapper.projectionTypeKeyFor(elementType, "Windows.Foundation.Collections"),
                ),
            ),
            winRtSizeSlot = 7,
        )
    }

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
        if (qualifiedName.startsWith("Windows.Foundation.Collections.IIterable<") && qualifiedName.endsWith(">")) {
            val elementType = qualifiedName.substringAfter('<').substringBeforeLast('>')
            if (!supportsClosedGenericIterableElement(elementType)) {
                return null
            }
            val rawIterableClass = typeNameMapper.mapTypeName(
                "Windows.Foundation.Collections.IIterable",
                "Windows.Foundation.Collections",
            ) as ClassName
            val rawIteratorClass = typeNameMapper.mapTypeName(
                "Windows.Foundation.Collections.IIterator",
                "Windows.Foundation.Collections",
            ) as ClassName
            val elementTypeName = collectionElementTypeName(elementType, typeNameMapper)
            val elementSignature = winRtSignatureMapper.signatureFor(elementType, "Windows.Foundation.Collections")
            val elementProjectionTypeKey = winRtProjectionTypeMapper.projectionTypeKeyFor(elementType, "Windows.Foundation.Collections")
            return RuntimeIterableProjection(
                superinterface = PoetSymbols.iterableClass.parameterizedBy(elementTypeName),
                delegateFactory = CodeBlock.of(
                    "object : %T {\n" +
                        "  override fun iterator(): %T = object : %T {\n" +
                        "    private val iteratorProjection = %T.from(%T(%T.invokeObjectMethod(%T.from(%T(pointer), %S, %S).pointer, 6).getOrThrow()), %S, %S)\n" +
                        "    override fun hasNext(): Boolean = %T(%L).value\n" +
                        "    override fun next(): %T {\n" +
                        "      if (!hasNext()) throw %T()\n" +
                        "      val current = %L\n" +
                        "      %T.invokeBooleanGetter(iteratorProjection.pointer, 8).getOrThrow()\n" +
                        "      return current\n" +
                        "    }\n" +
                        "  }\n" +
                        "}",
                    PoetSymbols.iterableClass.parameterizedBy(elementTypeName),
                    PoetSymbols.iteratorClass.parameterizedBy(elementTypeName),
                    PoetSymbols.iteratorClass.parameterizedBy(elementTypeName),
                    rawIteratorClass,
                    PoetSymbols.inspectableClass,
                    PoetSymbols.platformComInteropClass,
                    rawIterableClass,
                    PoetSymbols.inspectableClass,
                    elementSignature,
                    elementProjectionTypeKey,
                    elementSignature,
                    elementProjectionTypeKey,
                    PoetSymbols.winRtBooleanClass,
                    AbiCallCatalog.booleanGetter(7, "iteratorProjection.pointer"),
                    elementTypeName,
                    NoSuchElementException::class,
                    if (isKeyValuePairElement(elementType)) {
                        keyValuePairEntryExpression(elementType, "iteratorProjection.pointer")
                    } else {
                        elementReadExpression(elementTypeName, "iteratorProjection.pointer", 6)
                    },
                    PoetSymbols.platformComInteropClass,
                ),
            )
        }
        if (qualifiedName.startsWith("Windows.Foundation.Collections.IIterator<") && qualifiedName.endsWith(">")) {
            val elementType = qualifiedName.substringAfter('<').substringBeforeLast('>')
            if (!supportsClosedGenericIterableElement(elementType)) {
                return null
            }
            val rawIteratorClass = typeNameMapper.mapTypeName(
                "Windows.Foundation.Collections.IIterator",
                "Windows.Foundation.Collections",
            ) as ClassName
            val elementTypeName = collectionElementTypeName(elementType, typeNameMapper)
            val elementSignature = winRtSignatureMapper.signatureFor(elementType, "Windows.Foundation.Collections")
            val elementProjectionTypeKey = winRtProjectionTypeMapper.projectionTypeKeyFor(elementType, "Windows.Foundation.Collections")
            return RuntimeIterableProjection(
                superinterface = PoetSymbols.iteratorClass.parameterizedBy(elementTypeName),
                delegateFactory = CodeBlock.of(
                    "object : %T {\n" +
                        "  private val iteratorProjection = %T.from(%T(pointer), %S, %S)\n" +
                        "  override fun hasNext(): Boolean = %T(%L).value\n" +
                        "  override fun next(): %T {\n" +
                        "    if (!hasNext()) throw %T()\n" +
                        "    val current = %L\n" +
                        "    %T.invokeBooleanGetter(iteratorProjection.pointer, 8).getOrThrow()\n" +
                        "    return current\n" +
                        "  }\n" +
                        "}",
                    PoetSymbols.iteratorClass.parameterizedBy(elementTypeName),
                    rawIteratorClass,
                    PoetSymbols.inspectableClass,
                    elementSignature,
                    elementProjectionTypeKey,
                    PoetSymbols.winRtBooleanClass,
                    AbiCallCatalog.booleanGetter(7, "iteratorProjection.pointer"),
                    elementTypeName,
                    NoSuchElementException::class,
                    if (isKeyValuePairElement(elementType)) {
                        keyValuePairEntryExpression(elementType, "iteratorProjection.pointer")
                    } else {
                        elementReadExpression(elementTypeName, "iteratorProjection.pointer", 6)
                    },
                    PoetSymbols.platformComInteropClass,
                ),
            )
        }
        return null
    }

    private fun supportsClosedGenericIterableElement(typeName: String): Boolean {
        return typeName == "String" || typeName == "Boolean" || typeName == "Int32" || typeName == "UInt32" || typeName == "Int64" || typeName == "UInt64" || typeName == "Float32" || typeName == "Float64" || typeName == "DateTime" || typeName == "TimeSpan" || (
            (typeName == "Object" || typeName.contains('.')) &&
                !typeName.endsWith("[]") &&
                (typeName.indexOf('<') < 0 || isKeyValuePairElement(typeName))
            )
    }

    private fun supportsClosedGenericVectorElement(typeName: String): Boolean {
        return supportsClosedGenericIterableElement(typeName)
    }

    private fun isXamlBindableInteropNamespace(namespace: String): Boolean {
        return namespace == "Microsoft.UI.Xaml.Interop" || namespace == "Windows.UI.Xaml.Interop"
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

    private fun isKeyValuePairElement(typeName: String): Boolean {
        return typeName.startsWith("Windows.Foundation.Collections.IKeyValuePair<") && typeName.endsWith(">")
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
