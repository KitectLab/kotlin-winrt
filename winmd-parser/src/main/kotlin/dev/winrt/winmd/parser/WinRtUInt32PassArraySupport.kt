package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isUInt32PassArrayParameter(): Boolean =
    type == "UInt32[]" && arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isUInt32PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    arrayReturnCategory() == null &&
        parameters.count { parameter -> parameter.isUInt32PassArrayParameter() } == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isUInt32PassArrayParameter() } &&
        (returnType == "Unit" || supportsObjectReturn(returnType))

internal fun uint32PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerNonArrayArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? {
    return buildList {
        parameters.forEach { parameter ->
            val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
            if (parameter.isUInt32PassArrayParameter()) {
                add(CodeBlock.of("%N.size", parameterName))
                add(CodeBlock.of("IntArray(%N.size) { index -> %N[index].value.toInt() }", parameterName, parameterName))
            } else {
                add(lowerNonArrayArgument(parameter) ?: return null)
            }
        }
    }
}
