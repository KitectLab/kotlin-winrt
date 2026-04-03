package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
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
                    it.startsWith("Windows.Foundation.Collections.IIterable<") ||
                    it.startsWith("Windows.Foundation.Collections.IIterator<")
            }
            ?: return null
        return when (iterableInterface) {
            "Microsoft.UI.Xaml.Interop.IBindableIterable" -> RuntimeIterableProjection(
                superinterface = PoetSymbols.iterableClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer))",
                    typeNameMapper.mapTypeName(iterableInterface, "Microsoft.UI.Xaml.Interop") as ClassName,
                    PoetSymbols.inspectableClass,
                ),
            )
            "Microsoft.UI.Xaml.Interop.IBindableIterator" -> RuntimeIterableProjection(
                superinterface = PoetSymbols.iteratorClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer))",
                    typeNameMapper.mapTypeName(iterableInterface, "Microsoft.UI.Xaml.Interop") as ClassName,
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
        if (type.namespace == "Microsoft.UI.Xaml.Interop" && type.name == "IBindableVector") {
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
        if (type.namespace == "Windows.Foundation.Collections" && type.name == "IMap`2") {
            val keyTypeName = type.genericParameters.firstOrNull()?.let { TypeVariableName(it) } ?: return null
            val valueTypeName = type.genericParameters.getOrNull(1)?.let { TypeVariableName(it) } ?: return null
            return InterfaceCollectionProjection(
                superinterface = PoetSymbols.mutableMapClass.parameterizedBy(keyTypeName, valueTypeName),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { winRtSize.value.toInt() }, lookupFn = { key -> lookup(key) }, containsKeyFn = { key -> hasKey(key) }, putValueFn = { key, value -> insert(key, value) }, removeKeyFn = { key -> remove(key) }, clearerFn = { clear() }, entriesProvider = { first().asSequence().toList() })",
                    PoetSymbols.winRtMutableMapProjectionClass.parameterizedBy(keyTypeName, valueTypeName),
                ),
                winRtSizeSlot = 7,
            )
        }
        if (type.namespace == "Windows.Foundation.Collections" && type.name == "IObservableMap`2") {
            val keyTypeName = type.genericParameters.firstOrNull()?.let { TypeVariableName(it) } ?: return null
            val valueTypeName = type.genericParameters.getOrNull(1)?.let { TypeVariableName(it) } ?: return null
            return InterfaceCollectionProjection(
                superinterface = PoetSymbols.mutableMapClass.parameterizedBy(keyTypeName, valueTypeName),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { winRtSize.value.toInt() }, lookupFn = { key -> lookup(key) }, containsKeyFn = { key -> hasKey(key) }, putValueFn = { key, value -> insert(key, value) }, removeKeyFn = { key -> remove(key) }, clearerFn = { clear() }, entriesProvider = { first().asSequence().toList() })",
                    PoetSymbols.winRtMutableMapProjectionClass.parameterizedBy(keyTypeName, valueTypeName),
                ),
                winRtSizeSlot = 7,
            )
        }
        if (type.namespace == "Windows.Foundation.Collections" && type.name == "IMapView`2") {
            val keyTypeName = type.genericParameters.firstOrNull()?.let { TypeVariableName(it) } ?: return null
            val valueTypeName = type.genericParameters.getOrNull(1)?.let { TypeVariableName(it) } ?: return null
            return InterfaceCollectionProjection(
                superinterface = PoetSymbols.mapClass.parameterizedBy(keyTypeName, valueTypeName),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { winRtSize.value.toInt() }, lookupFn = { key -> lookup(key) }, containsKeyFn = { key -> hasKey(key) }, entriesProvider = { first().asSequence().toList() })",
                    PoetSymbols.winRtMapProjectionClass.parameterizedBy(keyTypeName, valueTypeName),
                ),
                winRtSizeSlot = 7,
            )
        }
        if (type.namespace == "Windows.Foundation.Collections" && type.name == "IObservableVector`1") {
            val elementTypeName = type.genericParameters.firstOrNull()?.let { TypeVariableName(it) } ?: return null
            return InterfaceCollectionProjection(
                superinterface = PoetSymbols.mutableListClass.parameterizedBy(elementTypeName),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { winRtSize.value.toInt() }, getter = { index -> getAt(index) }, append = { value -> append(value) }, clearer = { clear() })",
                    PoetSymbols.winRtMutableListProjectionClass.parameterizedBy(elementTypeName),
                ),
                winRtSizeSlot = 7,
            )
        }
        if (type.namespace == "Microsoft.UI.Xaml.Interop" && type.name == "IBindableVectorView") {
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

    private fun interfaceProjectionMetadata(
        qualifiedName: String,
        typeNameMapper: TypeNameMapper,
        winRtSignatureMapper: WinRtSignatureMapper,
        winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    ): CollectionInterfaceMetadata? {
        if (qualifiedName.startsWith("Windows.Foundation.Collections.IMapView<") && qualifiedName.endsWith(">")) {
            val argumentSource = qualifiedName.substringAfter('<').substringBeforeLast('>')
            val arguments = splitGenericArguments(argumentSource)
            if (arguments.size != 2) {
                return null
            }
            val keyType = collectionElementTypeName(arguments[0], typeNameMapper)
            val valueType = collectionElementTypeName(arguments[1], typeNameMapper)
            val rawInterfaceClass = typeNameMapper.mapTypeName(
                "Windows.Foundation.Collections.IMapView",
                "Windows.Foundation.Collections",
            ) as ClassName
            return CollectionInterfaceMetadata(
                collectionSuperinterface = PoetSymbols.mapClass.parameterizedBy(keyType, valueType),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { winRtSize.value.toInt() }, lookupFn = { key -> lookup(key) }, containsKeyFn = { key -> hasKey(key) }, entriesProvider = { first().asSequence().toList() })",
                    PoetSymbols.winRtMapProjectionClass.parameterizedBy(keyType, valueType),
                ),
                winRtSizeSlot = 7,
            )
        }
        if (qualifiedName.startsWith("Windows.Foundation.Collections.IObservableMap<") && qualifiedName.endsWith(">")) {
            val argumentSource = qualifiedName.substringAfter('<').substringBeforeLast('>')
            val arguments = splitGenericArguments(argumentSource)
            if (arguments.size != 2) {
                return null
            }
            val keyType = collectionElementTypeName(arguments[0], typeNameMapper)
            val valueType = collectionElementTypeName(arguments[1], typeNameMapper)
            val rawInterfaceClass = typeNameMapper.mapTypeName(
                "Windows.Foundation.Collections.IObservableMap",
                "Windows.Foundation.Collections",
            ) as ClassName
            return CollectionInterfaceMetadata(
                collectionSuperinterface = PoetSymbols.mutableMapClass.parameterizedBy(keyType, valueType),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer), %S, %S)",
                    rawInterfaceClass,
                    PoetSymbols.inspectableClass,
                    winRtSignatureMapper.signatureFor(qualifiedName, "Windows.Foundation.Collections"),
                    winRtProjectionTypeMapper.projectionTypeKeyFor(qualifiedName, "Windows.Foundation.Collections"),
                ),
                winRtSizeSlot = 7,
            )
        }
        if (qualifiedName.startsWith("Windows.Foundation.Collections.IMap<") && qualifiedName.endsWith(">")) {
            val argumentSource = qualifiedName.substringAfter('<').substringBeforeLast('>')
            val arguments = splitGenericArguments(argumentSource)
            if (arguments.size != 2) {
                return null
            }
            val keyType = collectionElementTypeName(arguments[0], typeNameMapper)
            val valueType = collectionElementTypeName(arguments[1], typeNameMapper)
            val rawInterfaceClass = typeNameMapper.mapTypeName(
                "Windows.Foundation.Collections.IMap",
                "Windows.Foundation.Collections",
            ) as ClassName
            return CollectionInterfaceMetadata(
                collectionSuperinterface = PoetSymbols.mutableMapClass.parameterizedBy(keyType, valueType),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { winRtSize.value.toInt() }, lookupFn = { key -> lookup(key) }, containsKeyFn = { key -> hasKey(key) }, putValueFn = { key, value -> insert(key, value) }, removeKeyFn = { key -> remove(key) }, clearerFn = { clear() }, entriesProvider = { first().asSequence().toList() })",
                    PoetSymbols.winRtMutableMapProjectionClass.parameterizedBy(keyType, valueType),
                ),
                winRtSizeSlot = 7,
            )
        }
        if (qualifiedName.startsWith("Windows.Foundation.Collections.IObservableVector<") && qualifiedName.endsWith(">")) {
            val elementType = qualifiedName.substringAfter('<').substringBeforeLast('>')
            if (!supportsClosedGenericVectorElement(elementType)) {
                return null
            }
            val rawInterfaceClass = typeNameMapper.mapTypeName(
                "Windows.Foundation.Collections.IObservableVector",
                "Windows.Foundation.Collections",
            ) as ClassName
            val elementTypeName = collectionElementTypeName(elementType, typeNameMapper)
            return CollectionInterfaceMetadata(
                collectionSuperinterface = PoetSymbols.mutableListClass.parameterizedBy(elementTypeName),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer), %S, %S)",
                    rawInterfaceClass,
                    PoetSymbols.inspectableClass,
                    winRtSignatureMapper.signatureFor(elementType, "Windows.Foundation.Collections"),
                    winRtProjectionTypeMapper.projectionTypeKeyFor(elementType, "Windows.Foundation.Collections"),
                ),
                winRtSizeSlot = 8,
            )
        }
        if (qualifiedName.startsWith("Windows.Foundation.Collections.IVectorView<") && qualifiedName.endsWith(">")) {
            val elementType = qualifiedName.substringAfter('<').substringBeforeLast('>')
            if (!supportsClosedGenericVectorElement(elementType)) {
                return null
            }
            val rawInterfaceClass = typeNameMapper.mapTypeName(
                "Windows.Foundation.Collections.IVectorView",
                "Windows.Foundation.Collections",
            ) as ClassName
            val elementTypeName = collectionElementTypeName(elementType, typeNameMapper)
            return CollectionInterfaceMetadata(
                collectionSuperinterface = PoetSymbols.listClass.parameterizedBy(elementTypeName),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer), %S, %S)",
                    rawInterfaceClass,
                    PoetSymbols.inspectableClass,
                    winRtSignatureMapper.signatureFor(elementType, "Windows.Foundation.Collections"),
                    winRtProjectionTypeMapper.projectionTypeKeyFor(elementType, "Windows.Foundation.Collections"),
                ),
                winRtSizeSlot = 8,
            )
        }
        if (qualifiedName.startsWith("Windows.Foundation.Collections.IVector<") && qualifiedName.endsWith(">")) {
            val elementType = qualifiedName.substringAfter('<').substringBeforeLast('>')
            if (!supportsClosedGenericVectorElement(elementType)) {
                return null
            }
            val rawInterfaceClass = typeNameMapper.mapTypeName(
                "Windows.Foundation.Collections.IVector",
                "Windows.Foundation.Collections",
            ) as ClassName
            val elementTypeName = collectionElementTypeName(elementType, typeNameMapper)
            return CollectionInterfaceMetadata(
                collectionSuperinterface = PoetSymbols.mutableListClass.parameterizedBy(elementTypeName),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer), %S, %S)",
                    rawInterfaceClass,
                    PoetSymbols.inspectableClass,
                    winRtSignatureMapper.signatureFor(elementType, "Windows.Foundation.Collections"),
                    winRtProjectionTypeMapper.projectionTypeKeyFor(elementType, "Windows.Foundation.Collections"),
                ),
                winRtSizeSlot = 8,
            )
        }
        return when (qualifiedName) {
            "Microsoft.UI.Xaml.Interop.IBindableVector" -> CollectionInterfaceMetadata(
                collectionSuperinterface = PoetSymbols.mutableListClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer))",
                    typeNameMapper.mapTypeName(qualifiedName, "Microsoft.UI.Xaml.Interop") as ClassName,
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
            "Microsoft.UI.Xaml.Interop.IBindableVectorView" -> CollectionInterfaceMetadata(
                collectionSuperinterface = PoetSymbols.listClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer))",
                    typeNameMapper.mapTypeName(qualifiedName, "Microsoft.UI.Xaml.Interop") as ClassName,
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
        return typeName == "String" || typeName == "Boolean" || typeName == "Int32" || typeName == "UInt32" || typeName == "Int64" || typeName == "UInt64" || typeName == "Float64" || typeName == "DateTime" || typeName == "TimeSpan" || (
            (typeName == "Object" || typeName.contains('.')) &&
                !typeName.endsWith("[]") &&
                (typeName.indexOf('<') < 0 || isKeyValuePairElement(typeName))
            )
    }

    private fun supportsClosedGenericVectorElement(typeName: String): Boolean {
        return supportsClosedGenericIterableElement(typeName)
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
    val winRtSizeSlot: Int,
    val extraProperties: List<PropertySpec> = emptyList(),
    val extraFunctions: List<FunSpec> = emptyList(),
)

internal data class InterfaceCollectionProjection(
    val superinterface: TypeName,
    val delegateFactory: CodeBlock? = null,
    val winRtSizeSlot: Int,
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
    val winRtSizeSlot: Int,
    val extraProperties: List<PropertySpec> = emptyList(),
    val extraFunctions: List<FunSpec> = emptyList(),
)
