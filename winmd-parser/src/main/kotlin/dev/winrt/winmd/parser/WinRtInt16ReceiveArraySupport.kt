package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isInt16PassArrayParameter(): Boolean =
    isExactPassArrayParameter("Int16")

internal fun WinMdMethod.isInt16PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isInt16PassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isInt16ReceiveArrayReturnMethod(): Boolean =
    isStandardReceiveArrayReturnMethod("Int16", WinMdParameter::isInt16PassArrayParameter)

internal fun int16ReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isInt16PassArrayParameter,
) { parameterName ->
    CodeBlock.of("ShortArray(%N.size) { index -> %N[index] }", parameterName, parameterName)
}

internal fun int16PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = int16ReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun int16ReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeInt16ReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().toTypedArray()",
)
