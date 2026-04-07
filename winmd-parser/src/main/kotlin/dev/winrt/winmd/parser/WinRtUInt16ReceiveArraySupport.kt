package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isUInt16PassArrayParameter(): Boolean =
    isExactPassArrayParameter("UInt16")

internal fun WinMdMethod.isUInt16PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isUInt16PassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isUInt16ReceiveArrayReturnMethod(): Boolean =
    isStandardReceiveArrayReturnMethod("UInt16", WinMdParameter::isUInt16PassArrayParameter)

internal fun uint16ReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isUInt16PassArrayParameter,
) { parameterName ->
    CodeBlock.of("ShortArray(%N.size) { index -> %N[index].toShort() }", parameterName, parameterName)
}

internal fun uint16PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = uint16ReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun uint16ReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeUInt16ReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().map { it.toUShort() }.toTypedArray()",
)
