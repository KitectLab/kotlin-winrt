package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isUInt8PassArrayParameter(): Boolean =
    isExactPassArrayParameter("UInt8")

internal fun WinMdMethod.isUInt8PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isUInt8PassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isUInt8ReceiveArrayReturnMethod(): Boolean =
    isStandardReceiveArrayReturnMethod("UInt8", WinMdParameter::isUInt8PassArrayParameter)

internal fun uint8ReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isUInt8PassArrayParameter,
) { parameterName ->
    CodeBlock.of("ByteArray(%N.size) { index -> %N[index].toByte() }", parameterName, parameterName)
}

internal fun uint8PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = uint8ReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun uint8ReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeUInt8ReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().map { it.toUByte() }.toTypedArray()",
)
