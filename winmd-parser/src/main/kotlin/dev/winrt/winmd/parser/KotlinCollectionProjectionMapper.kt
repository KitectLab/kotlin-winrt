package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdType

internal class KotlinCollectionProjectionMapper {
    fun runtimeClassProjection(type: WinMdType): RuntimeCollectionProjection? {
        if (type.namespace == "Windows.Foundation.Collections" && type.name == "StringVectorView") {
            return RuntimeCollectionProjection(
                superinterface = PoetSymbols.listClass.parameterizedBy(String::class.asTypeName()),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { %T(%T.invokeUInt32Method(pointer, 7).getOrThrow()).value.toInt() }, getter = { index -> val value = %T.invokeHStringMethodWithUInt32Arg(pointer, 6, index.toUInt()).getOrThrow(); try { %T.toKotlin(value) } finally { %T.release(value) } })",
                    PoetSymbols.winRtListProjectionClass.parameterizedBy(String::class.asTypeName()),
                    PoetSymbols.uint32Class,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.winRtStringsClass,
                    PoetSymbols.winRtStringsClass,
                ),
                winRtSizeSlot = 7,
                extraFunctions = listOf(
                    FunSpec.builder("getAt")
                        .addParameter("index", PoetSymbols.uint32Class)
                        .returns(String::class)
                        .addStatement(
                            "val value = %T.invokeHStringMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow()",
                            PoetSymbols.platformComInteropClass,
                        )
                        .beginControlFlow("return try")
                        .addStatement("%T.toKotlin(value)", PoetSymbols.winRtStringsClass)
                        .nextControlFlow("finally")
                        .addStatement("%T.release(value)", PoetSymbols.winRtStringsClass)
                        .endControlFlow()
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
        val collectionInterface = sequenceOf(type.defaultInterface)
            .filterNotNull()
            .plus(type.implementedInterfaces.asSequence())
            .distinct()
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
        )
    }

    fun runtimeClassIterableProjection(
        type: WinMdType,
        typeNameMapper: TypeNameMapper,
        winRtSignatureMapper: WinRtSignatureMapper,
        winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    ): RuntimeIterableProjection? {
        val iterableInterface = sequenceOf(type.defaultInterface)
            .filterNotNull()
            .plus(type.implementedInterfaces.asSequence())
            .distinct()
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
                    "%T(sizeProvider = { %T(%T.invokeUInt32Method(pointer, 8).getOrThrow()).value.toInt() }, getter = { index -> %T(%T.invokeObjectMethodWithUInt32Arg(pointer, 7, index.toUInt()).getOrThrow()) }, append = { value -> %T.invokeObjectSetter(pointer, 14, value.pointer).getOrThrow() }, clearer = { %T.invokeUnitMethod(pointer, 16).getOrThrow() })",
                    PoetSymbols.winRtMutableListProjectionClass.parameterizedBy(PoetSymbols.inspectableClass),
                    PoetSymbols.uint32Class,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.inspectableClass,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.platformComInteropClass,
                ),
                winRtSizeSlot = 8,
            )
        }
        if (type.namespace == "Microsoft.UI.Xaml.Interop" && type.name == "IBindableVectorView") {
            return InterfaceCollectionProjection(
                superinterface = PoetSymbols.listClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { %T(%T.invokeUInt32Method(pointer, 8).getOrThrow()).value.toInt() }, getter = { index -> %T(%T.invokeObjectMethodWithUInt32Arg(pointer, 7, index.toUInt()).getOrThrow()) })",
                    PoetSymbols.winRtListProjectionClass.parameterizedBy(PoetSymbols.inspectableClass),
                    PoetSymbols.uint32Class,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.inspectableClass,
                    PoetSymbols.platformComInteropClass,
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
                        "return %T(%T.invokeUInt32Method(pointer, %L).getOrThrow())",
                        PoetSymbols.uint32Class,
                        PoetSymbols.platformComInteropClass,
                        slot,
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
                    typeNameMapper.mapTypeName(qualifiedName, qualifiedName.substringBeforeLast(".")) as ClassName,
                    PoetSymbols.inspectableClass,
                ),
                winRtSizeSlot = 8,
            )
            "Microsoft.UI.Xaml.Interop.IBindableVectorView" -> CollectionInterfaceMetadata(
                collectionSuperinterface = PoetSymbols.listClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T.from(%T(pointer))",
                    typeNameMapper.mapTypeName(qualifiedName, qualifiedName.substringBeforeLast(".")) as ClassName,
                    PoetSymbols.inspectableClass,
                ),
                winRtSizeSlot = 8,
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
                        "    override fun hasNext(): Boolean = %T(%T.invokeBooleanGetter(iteratorProjection.pointer, 7).getOrThrow()).value\n" +
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
                    PoetSymbols.platformComInteropClass,
                    elementTypeName,
                    NoSuchElementException::class,
                    elementReadExpression(elementTypeName, "iteratorProjection.pointer", 6),
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
                        "  override fun hasNext(): Boolean = %T(%T.invokeBooleanGetter(iteratorProjection.pointer, 7).getOrThrow()).value\n" +
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
                    PoetSymbols.platformComInteropClass,
                    elementTypeName,
                    NoSuchElementException::class,
                    elementReadExpression(elementTypeName, "iteratorProjection.pointer", 6),
                    PoetSymbols.platformComInteropClass,
                ),
            )
        }
        return null
    }

    private fun supportsClosedGenericIterableElement(typeName: String): Boolean {
        return typeName == "String" || typeName == "Boolean" || typeName == "Int32" || typeName == "UInt32" || typeName == "Int64" || typeName == "UInt64" || typeName == "Float64" || (
            (typeName == "Object" || typeName.contains('.')) &&
                !typeName.contains('<') &&
                !typeName.endsWith("[]")
            )
    }

    private fun supportsClosedGenericVectorElement(typeName: String): Boolean {
        return supportsClosedGenericIterableElement(typeName)
    }

    private fun elementReadExpression(
        elementTypeName: TypeName,
        pointerExpression: String,
        slot: Int,
    ): CodeBlock {
        return if (elementTypeName == String::class.asTypeName()) {
            CodeBlock.of(
                "run {\n" +
                    "        val value = %T.invokeHStringMethod(%L, %L).getOrThrow()\n" +
                    "        try {\n" +
                    "          %T.toKotlin(value)\n" +
                    "        } finally {\n" +
                    "          %T.release(value)\n" +
                    "        }\n" +
                    "      }",
                PoetSymbols.platformComInteropClass,
                pointerExpression,
                slot,
                PoetSymbols.winRtStringsClass,
                PoetSymbols.winRtStringsClass,
            )
        } else if (elementTypeName == Boolean::class.asTypeName()) {
            CodeBlock.of(
                "%T(%T.invokeBooleanGetter(%L, %L).getOrThrow()).value",
                PoetSymbols.winRtBooleanClass,
                PoetSymbols.platformComInteropClass,
                pointerExpression,
                slot,
            )
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

    private fun collectionElementTypeName(typeName: String, typeNameMapper: TypeNameMapper): TypeName {
        return when (typeName) {
            "Boolean" -> Boolean::class.asTypeName()
            "Int32" -> Int::class.asTypeName()
            "UInt32" -> UInt::class.asTypeName()
            "Int64" -> Long::class.asTypeName()
            "UInt64" -> ULong::class.asTypeName()
            "Float64" -> Double::class.asTypeName()
            else -> typeNameMapper.mapTypeName(typeName, "Windows.Foundation.Collections")
        }
    }
}

internal data class RuntimeCollectionProjection(
    val superinterface: TypeName,
    val delegateFactory: CodeBlock,
    val winRtSizeSlot: Int,
    val extraFunctions: List<FunSpec> = emptyList(),
)

internal data class InterfaceCollectionProjection(
    val superinterface: TypeName,
    val delegateFactory: CodeBlock,
    val winRtSizeSlot: Int,
)

internal data class RuntimeIterableProjection(
    val superinterface: TypeName,
    val delegateFactory: CodeBlock,
)

internal data class CollectionInterfaceMetadata(
    val collectionSuperinterface: TypeName,
    val delegateFactory: CodeBlock,
    val winRtSizeSlot: Int,
)
