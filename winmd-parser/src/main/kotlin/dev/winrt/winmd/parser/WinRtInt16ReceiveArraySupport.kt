package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isInt16PassArrayParameter(): Boolean =
    type == "Int16[]" && arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isInt16PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    arrayReturnCategory() == null &&
        parameters.count { parameter -> parameter.isInt16PassArrayParameter() } == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isInt16PassArrayParameter() } &&
        (returnType == "Unit" || supportsObjectReturn(returnType))

internal fun WinMdMethod.isInt16ReceiveArrayReturnMethod(): Boolean =
    returnType == "Int16[]" &&
        arrayReturnCategory() == WinRtArrayParameterCategory.RECEIVE_ARRAY &&
        parameters.count { parameter -> parameter.isInt16PassArrayParameter() } <= 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isInt16PassArrayParameter() }

internal fun int16ReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = buildList {
    parameters.forEach { parameter ->
        val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
        if (parameter.isInt16PassArrayParameter()) {
            add(CodeBlock.of("%N.size", parameterName))
            add(CodeBlock.of("ShortArray(%N.size) { index -> %N[index] }", parameterName, parameterName))
        } else {
            add(lowerArgument(parameter) ?: return null)
        }
    }
}

internal fun int16PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = int16ReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun int16ReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = CodeBlock.builder()
    .add("%M(pointer, %L", PoetSymbols.invokeInt16ReceiveArrayMethodMember, vtableIndex)
    .apply {
        abiArguments.forEach { argument ->
            add(", %L", argument)
        }
    }
    .add(").getOrThrow().toTypedArray()")
    .build()
