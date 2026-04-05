package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isStringPassArrayParameter(): Boolean =
    type == "String[]" && arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isStringPassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    arrayReturnCategory() == null &&
        parameters.count { parameter -> parameter.isStringPassArrayParameter() } == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isStringPassArrayParameter() } &&
        (returnType == "Unit" || supportsObjectReturn(returnType))

internal fun WinMdMethod.isSingleStringPassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStringPassArrayMethod(supportsObjectReturn) && parameters.size == 1

internal fun stringPassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerNonArrayArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? {
    return buildList {
        parameters.forEach { parameter ->
            val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
            if (parameter.isStringPassArrayParameter()) {
                add(CodeBlock.of("%N.size", parameterName))
                add(CodeBlock.of("%N", parameterName))
            } else {
                add(lowerNonArrayArgument(parameter) ?: return null)
            }
        }
    }
}
