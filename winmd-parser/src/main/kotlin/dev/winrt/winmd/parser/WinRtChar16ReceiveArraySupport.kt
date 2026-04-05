package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isChar16PassArrayParameter(): Boolean =
    type == "Char16[]" && arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isChar16PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    arrayReturnCategory() == null &&
        parameters.count { parameter -> parameter.isChar16PassArrayParameter() } == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isChar16PassArrayParameter() } &&
        (returnType == "Unit" || supportsObjectReturn(returnType))

internal fun WinMdMethod.isChar16ReceiveArrayReturnMethod(): Boolean =
    returnType == "Char16[]" &&
        arrayReturnCategory() == WinRtArrayParameterCategory.RECEIVE_ARRAY &&
        parameters.count { parameter -> parameter.isChar16PassArrayParameter() } <= 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isChar16PassArrayParameter() }

internal fun char16ReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = buildList {
    parameters.forEach { parameter ->
        val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
        if (parameter.isChar16PassArrayParameter()) {
            add(CodeBlock.of("%N.size", parameterName))
            add(CodeBlock.of("CharArray(%N.size) { index -> %N[index] }", parameterName, parameterName))
        } else {
            add(lowerArgument(parameter) ?: return null)
        }
    }
}

internal fun char16PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = char16ReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun char16ReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = CodeBlock.builder()
    .add("%M(pointer, %L", PoetSymbols.invokeChar16ReceiveArrayMethodMember, vtableIndex)
    .apply {
        abiArguments.forEach { argument ->
            add(", %L", argument)
        }
    }
    .add(").getOrThrow().toTypedArray()")
    .build()
