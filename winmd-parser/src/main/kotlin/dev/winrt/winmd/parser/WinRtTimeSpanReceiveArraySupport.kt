package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isTimeSpanPassArrayParameter(): Boolean =
    isExactPassArrayParameter("TimeSpan")

internal fun WinMdMethod.isTimeSpanPassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isTimeSpanPassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isTimeSpanReceiveArrayReturnMethod(): Boolean =
    isStandardReceiveArrayReturnMethod("TimeSpan", WinMdParameter::isTimeSpanPassArrayParameter)

internal fun timeSpanReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isTimeSpanPassArrayParameter,
) { parameterName ->
    CodeBlock.of(
        "LongArray(%N.size) { index -> (%N[index].inWholeNanoseconds / 100) }",
        parameterName,
        parameterName,
    )
}

internal fun timeSpanPassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = timeSpanReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun timeSpanReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeTimeSpanReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().map { %T(it) }.toTypedArray()",
    PoetSymbols.timeSpanClass,
)
