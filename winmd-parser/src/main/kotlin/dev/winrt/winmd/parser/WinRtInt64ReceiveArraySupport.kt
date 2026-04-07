package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isInt64PassArrayParameter(): Boolean =
    isExactPassArrayParameter("Int64")

internal fun WinMdMethod.isInt64PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isInt64PassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isInt64ReceiveArrayReturnMethod(): Boolean =
    isStandardReceiveArrayReturnMethod("Int64", WinMdParameter::isInt64PassArrayParameter)

internal fun int64ReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isInt64PassArrayParameter,
) { parameterName ->
    CodeBlock.of("LongArray(%N.size) { index -> %N[index].value }", parameterName, parameterName)
}

internal fun int64PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = int64ReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun int64ReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeInt64ReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().map { %T(it) }.toTypedArray()",
    PoetSymbols.int64Class,
)
