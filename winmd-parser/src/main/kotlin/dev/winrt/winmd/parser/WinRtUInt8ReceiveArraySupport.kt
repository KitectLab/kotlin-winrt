package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isUInt8PassArrayParameter(): Boolean =
    type == "UInt8[]" && arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isUInt8PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    arrayReturnCategory() == null &&
        parameters.count { parameter -> parameter.isUInt8PassArrayParameter() } == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isUInt8PassArrayParameter() } &&
        (returnType == "Unit" || supportsObjectReturn(returnType))

internal fun WinMdMethod.isUInt8ReceiveArrayReturnMethod(): Boolean =
    returnType == "UInt8[]" &&
        arrayReturnCategory() == WinRtArrayParameterCategory.RECEIVE_ARRAY &&
        parameters.count { parameter -> parameter.isUInt8PassArrayParameter() } <= 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isUInt8PassArrayParameter() }

internal fun uint8ReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = buildList {
    parameters.forEach { parameter ->
        val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
        if (parameter.isUInt8PassArrayParameter()) {
            add(CodeBlock.of("%N.size", parameterName))
            add(CodeBlock.of("ByteArray(%N.size) { index -> %N[index].toByte() }", parameterName, parameterName))
        } else {
            add(lowerArgument(parameter) ?: return null)
        }
    }
}

internal fun uint8PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = uint8ReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun uint8ReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = CodeBlock.builder()
    .add("%M(pointer, %L", PoetSymbols.invokeUInt8ReceiveArrayMethodMember, vtableIndex)
    .apply {
        abiArguments.forEach { argument ->
            add(", %L", argument)
        }
    }
    .add(").getOrThrow().map { it.toUByte() }.toTypedArray()")
    .build()
