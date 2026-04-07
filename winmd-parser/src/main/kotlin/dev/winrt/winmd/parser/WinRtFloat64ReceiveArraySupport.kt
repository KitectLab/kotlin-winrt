package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isFloat64PassArrayParameter(): Boolean =
    isExactPassArrayParameter("Float64")

internal fun WinMdMethod.isFloat64PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isFloat64PassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isFloat64ReceiveArrayReturnMethod(): Boolean =
    isStandardReceiveArrayReturnMethod("Float64", WinMdParameter::isFloat64PassArrayParameter)

internal fun float64ReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isFloat64PassArrayParameter,
) { parameterName ->
    CodeBlock.of("DoubleArray(%N.size) { index -> %N[index].value }", parameterName, parameterName)
}

internal fun float64PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = float64ReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun float64ReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeFloat64ReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().map { %T(it) }.toTypedArray()",
    PoetSymbols.float64Class,
)
