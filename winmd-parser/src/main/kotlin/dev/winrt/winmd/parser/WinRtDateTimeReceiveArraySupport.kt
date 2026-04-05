package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isDateTimePassArrayParameter(): Boolean =
    type == "DateTime[]" && arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isDateTimePassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    arrayReturnCategory() == null &&
        parameters.count { parameter -> parameter.isDateTimePassArrayParameter() } == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isDateTimePassArrayParameter() } &&
        (returnType == "Unit" || supportsObjectReturn(returnType))

internal fun WinMdMethod.isDateTimeReceiveArrayReturnMethod(): Boolean =
    returnType == "DateTime[]" &&
        arrayReturnCategory() == WinRtArrayParameterCategory.RECEIVE_ARRAY &&
        parameters.count { parameter -> parameter.isDateTimePassArrayParameter() } <= 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isDateTimePassArrayParameter() }

internal fun dateTimeReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = buildList {
    parameters.forEach { parameter ->
        val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
        if (parameter.isDateTimePassArrayParameter()) {
            add(CodeBlock.of("%N.size", parameterName))
            add(
                CodeBlock.of(
                    "LongArray(%N.size) { index -> (((%N[index].epochSeconds * 10000000L) + (%N[index].nanosecondsOfSecond / 100)) + %L) }",
                    parameterName,
                    parameterName,
                    parameterName,
                    WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
                ),
            )
        } else {
            add(lowerArgument(parameter) ?: return null)
        }
    }
}

internal fun dateTimePassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = dateTimeReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun dateTimeReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = CodeBlock.builder()
    .add("%M(pointer, %L", PoetSymbols.invokeDateTimeReceiveArrayMethodMember, vtableIndex)
    .apply {
        abiArguments.forEach { argument ->
            add(", %L", argument)
        }
    }
    .add(").getOrThrow().map { %T.fromEpochSeconds((it - %L) / 10000000L, ((it - %L) %% 10000000L * 100).toInt()) }.toTypedArray()", PoetSymbols.dateTimeClass, WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET, WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET)
    .build()
