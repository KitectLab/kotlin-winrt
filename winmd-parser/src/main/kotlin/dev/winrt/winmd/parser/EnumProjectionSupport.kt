package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.requireSupportedWinRtEnumUnderlyingType

internal fun enumUnderlyingTypeOrDefault(
    typeRegistry: TypeRegistry,
    typeName: String,
    currentNamespace: String,
): String = typeRegistry.enumUnderlyingType(typeName, currentNamespace)
    ?.let { requireSupportedWinRtEnumUnderlyingType(it, typeName) }
    ?: "Int32"

internal fun enumSignatureType(
    typeRegistry: TypeRegistry,
    typeName: String,
    currentNamespace: String,
): String = normalizedEnumAbiType(enumUnderlyingTypeOrDefault(typeRegistry, typeName, currentNamespace))

internal fun enumValueTypeName(underlyingType: String): TypeName = when (normalizedEnumAbiType(underlyingType)) {
    "UInt32" -> UInt::class.asTypeName()
    else -> Int::class.asTypeName()
}

internal fun enumZeroLiteral(underlyingType: String): String = when (normalizedEnumAbiType(underlyingType)) {
    "UInt32" -> "0u"
    else -> "0"
}

internal fun enumMemberLiteral(value: Int, underlyingType: String): String = when (normalizedEnumAbiType(underlyingType)) {
    "UInt32" -> if (value < 0) "0x${value.toUInt().toString(16)}u" else "${value}u"
    else -> value.toString()
}

internal fun enumGetterAbiCall(
    underlyingType: String,
    vtableIndex: Int,
    pointerExpression: String = "pointer",
): CodeBlock = when (normalizedEnumAbiType(underlyingType)) {
    "UInt32" -> CodeBlock.of("%T.invokeUInt32Method(%L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, pointerExpression, vtableIndex)
    else -> CodeBlock.of("%T.invokeInt32Method(%L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, pointerExpression, vtableIndex)
}

internal fun enumMethodWithInt32ArgAbiCall(underlyingType: String, vtableIndex: Int, argumentName: String): CodeBlock =
    when (normalizedEnumAbiType(underlyingType)) {
        "UInt32" -> CodeBlock.of(
            "%T.invokeUInt32MethodWithInt32Arg(pointer, %L, %N.value).getOrThrow()",
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
    else -> AbiCallCatalog.int32Setter(vtableIndex)
}

private fun normalizedEnumAbiType(underlyingType: String): String = when (underlyingType) {
    "Int32" -> "Int32"
    "UInt32" -> "UInt32"
    else -> requireSupportedWinRtEnumUnderlyingType(underlyingType)
}
