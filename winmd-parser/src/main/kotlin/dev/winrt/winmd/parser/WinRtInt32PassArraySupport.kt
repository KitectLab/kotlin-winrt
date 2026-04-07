package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isInt32PassArrayParameter(): Boolean =
    isExactPassArrayParameter("Int32")

internal fun WinMdMethod.isInt32PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isInt32PassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isSingleInt32PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isInt32PassArrayMethod(supportsObjectReturn) && parameters.size == 1

internal fun int32PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerNonArrayArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? {
    return passArrayAbiArguments(
        parameters = parameters,
        lowerArgument = lowerNonArrayArgument,
        isPassArrayParameter = WinMdParameter::isInt32PassArrayParameter,
    ) { parameterName ->
        CodeBlock.of("IntArray(%N.size) { index -> %N[index].value }", parameterName, parameterName)
    }
}
