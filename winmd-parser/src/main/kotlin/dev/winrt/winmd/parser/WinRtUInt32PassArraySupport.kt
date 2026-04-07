package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isUInt32PassArrayParameter(): Boolean =
    isExactPassArrayParameter("UInt32")

internal fun WinMdMethod.isUInt32PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isUInt32PassArrayParameter, supportsObjectReturn)

internal fun uint32PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerNonArrayArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? {
    return passArrayAbiArguments(
        parameters = parameters,
        lowerArgument = lowerNonArrayArgument,
        isPassArrayParameter = WinMdParameter::isUInt32PassArrayParameter,
    ) { parameterName ->
        CodeBlock.of("IntArray(%N.size) { index -> %N[index].value.toInt() }", parameterName, parameterName)
    }
}
