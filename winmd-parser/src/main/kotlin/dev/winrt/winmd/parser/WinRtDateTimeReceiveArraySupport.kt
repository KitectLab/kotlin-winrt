package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isDateTimePassArrayParameter(): Boolean =
    isExactPassArrayParameter("DateTime")

internal fun WinMdMethod.isDateTimePassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isDateTimePassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isDateTimeReceiveArrayReturnMethod(): Boolean =
    isStandardReceiveArrayReturnMethod("DateTime", WinMdParameter::isDateTimePassArrayParameter)

internal fun dateTimeReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isDateTimePassArrayParameter,
) { parameterName ->
    CodeBlock.of(
        "LongArray(%N.size) { index -> (((%N[index].epochSeconds * 10000000L) + (%N[index].nanosecondsOfSecond / 100)) + %L) }",
        parameterName,
        parameterName,
        parameterName,
        WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
    )
}

internal fun dateTimePassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = dateTimeReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun dateTimeReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeDateTimeReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().map { %T.fromEpochSeconds((it - %L) / 10000000L, ((it - %L) %% 10000000L * 100).toInt()) }.toTypedArray()",
    PoetSymbols.dateTimeClass,
    WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
    WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
)
