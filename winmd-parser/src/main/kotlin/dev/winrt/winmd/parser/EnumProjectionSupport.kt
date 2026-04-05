package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName

internal fun enumUnderlyingTypeOrDefault(
    typeRegistry: TypeRegistry,
    typeName: String,
    currentNamespace: String,
): String = typeRegistry.enumUnderlyingType(typeName, currentNamespace) ?: "Int32"

internal fun enumSignatureType(
    typeRegistry: TypeRegistry,
    typeName: String,
    currentNamespace: String,
): String = normalizedEnumAbiType(enumUnderlyingTypeOrDefault(typeRegistry, typeName, currentNamespace))

internal fun enumValueTypeName(underlyingType: String): TypeName = when (normalizedEnumAbiType(underlyingType)) {
    "UInt32" -> UInt::class.asTypeName()
    "Int64" -> Long::class.asTypeName()
    "UInt64" -> ULong::class.asTypeName()
    else -> Int::class.asTypeName()
}

internal fun enumZeroLiteral(underlyingType: String): String = when (normalizedEnumAbiType(underlyingType)) {
    "UInt32" -> "0u"
    "Int64" -> "0L"
    "UInt64" -> "0uL"
    else -> "0"
}

internal fun enumMemberLiteral(value: Int, underlyingType: String): String = when (normalizedEnumAbiType(underlyingType)) {
    "UInt32" -> if (value < 0) "0x${value.toUInt().toString(16)}u" else "${value}u"
    "Int64" -> "${value}L"
    "UInt64" -> if (value < 0) "0x${value.toLong().toULong().toString(16)}uL" else "${value}uL"
    else -> value.toString()
}

internal fun enumGetterAbiCall(underlyingType: String, vtableIndex: Int): CodeBlock = when (normalizedEnumAbiType(underlyingType)) {
    "UInt32" -> CodeBlock.of("%T.invokeUInt32Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)
    "Int64" -> CodeBlock.of("%T.invokeInt64Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)
    "UInt64" -> CodeBlock.of("%T.invokeUInt64Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)
    else -> CodeBlock.of("%T.invokeInt32Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)
}

internal fun enumMethodWithInt32ArgAbiCall(underlyingType: String, vtableIndex: Int, argumentName: String): CodeBlock =
    when (normalizedEnumAbiType(underlyingType)) {
        "UInt32" -> CodeBlock.of(
            "%T.invokeUInt32MethodWithInt32Arg(pointer, %L, %N.value).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            argumentName,
        )
        "Int64" -> CodeBlock.of(
            "%T.invokeInt64MethodWithInt32Arg(pointer, %L, %N.value).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            argumentName,
        )
        "UInt64" -> CodeBlock.of(
            "%T.invokeUInt64MethodWithInt32Arg(pointer, %L, %N.value).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            argumentName,
        )
        else -> CodeBlock.of(
            "%T.invokeInt32MethodWithInt32Arg(pointer, %L, %N.value).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            argumentName,
        )
    }

internal fun enumSetterAbiCall(underlyingType: String, vtableIndex: Int): CodeBlock = when (normalizedEnumAbiType(underlyingType)) {
    "UInt32" -> AbiCallCatalog.uint32Setter(vtableIndex)
    "Int64" -> AbiCallCatalog.int64Setter(vtableIndex)
    "UInt64" -> AbiCallCatalog.uint64Setter(vtableIndex)
    else -> AbiCallCatalog.int32Setter(vtableIndex)
}

private fun normalizedEnumAbiType(underlyingType: String): String = when (underlyingType) {
    "Int8", "Int16", "Int32" -> "Int32"
    "UInt8", "UInt16", "Char16", "UInt32" -> "UInt32"
    "Int64" -> "Int64"
    "UInt64" -> "UInt64"
    else -> "Int32"
}
