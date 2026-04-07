package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isUInt64PassArrayParameter(): Boolean =
    isExactPassArrayParameter("UInt64")

internal fun WinMdMethod.isUInt64PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isUInt64PassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isUInt64ReceiveArrayReturnMethod(): Boolean =
    isStandardReceiveArrayReturnMethod("UInt64", WinMdParameter::isUInt64PassArrayParameter)

internal fun uint64ReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isUInt64PassArrayParameter,
) { parameterName ->
    CodeBlock.of("LongArray(%N.size) { index -> %N[index].value.toLong() }", parameterName, parameterName)
}

internal fun uint64PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = uint64ReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun uint64ReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeUInt64ReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().map { %T(it.toULong()) }.toTypedArray()",
    PoetSymbols.uint64Class,
)
