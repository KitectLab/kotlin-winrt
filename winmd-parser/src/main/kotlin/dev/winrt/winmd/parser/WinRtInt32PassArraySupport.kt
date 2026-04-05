package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isInt32PassArrayParameter(): Boolean =
    type == "Int32[]" && arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isInt32PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    arrayReturnCategory() == null &&
        parameters.count { parameter -> parameter.isInt32PassArrayParameter() } == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isInt32PassArrayParameter() } &&
        (returnType == "Unit" || supportsObjectReturn(returnType))

internal fun WinMdMethod.isSingleInt32PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isInt32PassArrayMethod(supportsObjectReturn) && parameters.size == 1

internal fun int32PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerNonArrayArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? {
    return buildList {
        parameters.forEach { parameter ->
            val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
            if (parameter.isInt32PassArrayParameter()) {
                add(CodeBlock.of("%N.size", parameterName))
                add(CodeBlock.of("IntArray(%N.size) { index -> %N[index].value }", parameterName, parameterName))
            } else {
                add(lowerNonArrayArgument(parameter) ?: return null)
            }
        }
    }
}
