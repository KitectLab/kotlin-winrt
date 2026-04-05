package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isBooleanPassArrayParameter(): Boolean =
    type == "Boolean[]" && arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isBooleanPassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    arrayReturnCategory() == null &&
        parameters.count { parameter -> parameter.isBooleanPassArrayParameter() } == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isBooleanPassArrayParameter() } &&
        (returnType == "Unit" || supportsObjectReturn(returnType))

internal fun WinMdMethod.isBooleanReceiveArrayReturnMethod(): Boolean =
    returnType == "Boolean[]" &&
        arrayReturnCategory() == WinRtArrayParameterCategory.RECEIVE_ARRAY &&
        parameters.count { parameter -> parameter.isBooleanPassArrayParameter() } <= 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isBooleanPassArrayParameter() }

internal fun booleanReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = buildList {
    parameters.forEach { parameter ->
        val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
        if (parameter.isBooleanPassArrayParameter()) {
            add(CodeBlock.of("%N.size", parameterName))
            add(CodeBlock.of("ByteArray(%N.size) { index -> if (%N[index].value) 1.toByte() else 0.toByte() }", parameterName, parameterName))
        } else {
            add(lowerArgument(parameter) ?: return null)
        }
    }
}

internal fun booleanPassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = booleanReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun booleanReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = CodeBlock.builder()
    .add("%M(pointer, %L", PoetSymbols.invokeBooleanReceiveArrayMethodMember, vtableIndex)
    .apply {
        abiArguments.forEach { argument ->
            add(", %L", argument)
        }
    }
    .add(").getOrThrow().map { %T(it) }.toTypedArray()", PoetSymbols.winRtBooleanClass)
    .build()
