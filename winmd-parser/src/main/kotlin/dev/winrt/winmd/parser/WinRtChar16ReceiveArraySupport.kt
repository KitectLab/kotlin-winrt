package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isChar16PassArrayParameter(): Boolean =
    isExactPassArrayParameter("Char16")

internal fun WinMdMethod.isChar16PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isChar16PassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isChar16ReceiveArrayReturnMethod(): Boolean =
    isStandardReceiveArrayReturnMethod("Char16", WinMdParameter::isChar16PassArrayParameter)

internal fun char16ReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isChar16PassArrayParameter,
) { parameterName ->
    CodeBlock.of("CharArray(%N.size) { index -> %N[index] }", parameterName, parameterName)
}

internal fun char16PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = char16ReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun char16ReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeChar16ReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().toTypedArray()",
)
