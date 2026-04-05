package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isInt32FillArrayParameter(): Boolean =
    type == "Int32[]" && arrayParameterCategory() == WinRtArrayParameterCategory.FILL_ARRAY

internal fun WinMdMethod.isInt32FillArrayMethod(): Boolean =
    arrayReturnCategory() == null &&
        parameters.count { parameter -> parameter.isInt32FillArrayParameter() } == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isInt32FillArrayParameter() }

internal fun int32FillArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerNonArrayArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? {
    return buildList {
        parameters.forEach { parameter ->
            val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
            if (parameter.isInt32FillArrayParameter()) {
                add(CodeBlock.of("%N.size", parameterName))
                add(CodeBlock.of("IntArray(%N.size) { index -> %N[index].value }", parameterName, parameterName))
            } else {
                add(lowerNonArrayArgument(parameter) ?: return null)
            }
        }
    }
}
