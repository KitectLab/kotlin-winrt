package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isGuidPassArrayParameter(): Boolean =
    type == "Guid[]" && arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isGuidPassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    arrayReturnCategory() == null &&
        parameters.count { parameter -> parameter.isGuidPassArrayParameter() } == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isGuidPassArrayParameter() } &&
        (returnType == "Unit" || supportsObjectReturn(returnType))

internal fun WinMdMethod.isGuidReceiveArrayReturnMethod(): Boolean =
    returnType == "Guid[]" &&
        arrayReturnCategory() == WinRtArrayParameterCategory.RECEIVE_ARRAY &&
        parameters.count { parameter -> parameter.isGuidPassArrayParameter() } <= 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isGuidPassArrayParameter() }

internal fun guidReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = buildList {
    parameters.forEach { parameter ->
        val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
        if (parameter.isGuidPassArrayParameter()) {
            add(CodeBlock.of("%N.size", parameterName))
            add(
                CodeBlock.of(
                    "ByteArray(%N.size * 16) { index -> val guid = guidOf(%N[index / 16].toString()); when (index %% 16) { 0 -> guid.data1.toByte(); 1 -> (guid.data1 shr 8).toByte(); 2 -> (guid.data1 shr 16).toByte(); 3 -> (guid.data1 shr 24).toByte(); 4 -> guid.data2.toByte(); 5 -> (guid.data2.toInt() shr 8).toByte(); 6 -> guid.data3.toByte(); 7 -> (guid.data3.toInt() shr 8).toByte(); else -> guid.data4[index %% 16 - 8] } }",
                    parameterName,
                    parameterName,
                ),
            )
        } else {
            add(lowerArgument(parameter) ?: return null)
        }
    }
}

internal fun guidPassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = guidReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun guidReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = CodeBlock.builder()
    .add("%M(pointer, %L", PoetSymbols.invokeGuidReceiveArrayMethodMember, vtableIndex)
    .apply {
        abiArguments.forEach { argument ->
            add(", %L", argument)
        }
    }
    .add(").getOrThrow().map { %T.parse(it.toString()) }.toTypedArray()", PoetSymbols.guidValueClass)
    .build()
