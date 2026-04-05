package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isUInt16PassArrayParameter(): Boolean =
    type == "UInt16[]" && arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isUInt16PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    arrayReturnCategory() == null &&
        parameters.count { parameter -> parameter.isUInt16PassArrayParameter() } == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isUInt16PassArrayParameter() } &&
        (returnType == "Unit" || supportsObjectReturn(returnType))

internal fun WinMdMethod.isUInt16ReceiveArrayReturnMethod(): Boolean =
    returnType == "UInt16[]" &&
        arrayReturnCategory() == WinRtArrayParameterCategory.RECEIVE_ARRAY &&
        parameters.count { parameter -> parameter.isUInt16PassArrayParameter() } <= 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isUInt16PassArrayParameter() }

internal fun uint16ReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = buildList {
    parameters.forEach { parameter ->
        val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
        if (parameter.isUInt16PassArrayParameter()) {
            add(CodeBlock.of("%N.size", parameterName))
            add(CodeBlock.of("ShortArray(%N.size) { index -> %N[index].toShort() }", parameterName, parameterName))
        } else {
            add(lowerArgument(parameter) ?: return null)
        }
    }
}

internal fun uint16PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = uint16ReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun uint16ReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = CodeBlock.builder()
    .add("%M(pointer, %L", PoetSymbols.invokeUInt16ReceiveArrayMethodMember, vtableIndex)
    .apply {
        abiArguments.forEach { argument ->
            add(", %L", argument)
        }
    }
    .add(").getOrThrow().map { it.toUShort() }.toTypedArray()")
    .build()
