package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isStringPassArrayParameter(): Boolean =
    isExactPassArrayParameter("String")

internal fun WinMdMethod.isStringPassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isStringPassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isSingleStringPassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStringPassArrayMethod(supportsObjectReturn) && parameters.size == 1

internal fun stringPassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerNonArrayArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? {
    return passArrayAbiArguments(
        parameters = parameters,
        lowerArgument = lowerNonArrayArgument,
        isPassArrayParameter = WinMdParameter::isStringPassArrayParameter,
    ) { parameterName ->
        CodeBlock.of("%N", parameterName)
    }
}
