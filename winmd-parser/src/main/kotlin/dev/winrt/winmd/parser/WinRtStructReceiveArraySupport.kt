package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter
import dev.winrt.winmd.plugin.stripValueTypeNameMarker

private val supportedStructReceiveArrayNames = setOf("Point", "Size", "Rect")

internal fun WinMdMethod.supportedStructReceiveArrayElementType(
    currentNamespace: String,
    typeRegistry: TypeRegistry,
): String? {
    if (arrayReturnCategory() != WinRtArrayParameterCategory.RECEIVE_ARRAY) {
        return null
    }
    if (parameters.any { parameter -> parameter.type.isWinRtArrayType() }) {
        return null
    }
    val elementType = stripValueTypeNameMarker(returnType).takeIf { it.endsWith("[]") }?.removeSuffix("[]") ?: return null
    if (!typeRegistry.isStructType(elementType, currentNamespace)) {
        return null
    }
    val simpleName = elementType.substringAfterLast('.').substringBefore('`')
    return elementType.takeIf { simpleName in supportedStructReceiveArrayNames }
}

internal fun structReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = parameters.map { parameter -> lowerArgument(parameter) ?: return null }

internal fun structReceiveArrayReturnExpression(
    vtableIndex: Int,
    structType: TypeName,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = CodeBlock.builder()
    .add("%M(pointer, %L, %T.ABI_LAYOUT", PoetSymbols.invokeStructReceiveArrayMethodMember, vtableIndex, structType)
    .apply {
        abiArguments.forEach { argument ->
            add(", %L", argument)
        }
    }
    .add(").getOrThrow().map { %T.fromAbi(it) }.toTypedArray()", structType)
    .build()
