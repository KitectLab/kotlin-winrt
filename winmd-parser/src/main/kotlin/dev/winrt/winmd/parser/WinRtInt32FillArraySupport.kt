package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isInt32FillArrayParameter(): Boolean =
    type == "Int32[]" && arrayParameterCategory() == WinRtArrayParameterCategory.FILL_ARRAY

internal fun WinMdMethod.isInt32FillArrayMethod(): Boolean =
    arrayReturnCategory() == null &&
        parameters.count { parameter -> parameter.isInt32FillArrayParameter() } == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isInt32FillArrayParameter() }

internal fun WinMdMethod.int32FillArrayParameter(): WinMdParameter? =
    parameters.singleOrNull { parameter -> parameter.isInt32FillArrayParameter() }

internal fun int32FillArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerNonArrayArgument: (WinMdParameter) -> CodeBlock?,
    lowerArrayArgument: (WinMdParameter) -> CodeBlock = { parameter ->
        val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
        CodeBlock.of("IntArray(%N.size) { index -> %N[index].value }", parameterName, parameterName)
    },
): List<CodeBlock>? {
    return buildList {
        parameters.forEach { parameter ->
            val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
            if (parameter.isInt32FillArrayParameter()) {
                add(CodeBlock.of("%N.size", parameterName))
                add(lowerArrayArgument(parameter))
            } else {
                add(lowerNonArrayArgument(parameter) ?: return null)
            }
        }
    }
}

internal fun int32FillArrayBufferName(parameter: WinMdParameter): String =
    "${parameter.name.replaceFirstChar(Char::lowercase)}Abi"

internal fun int32FillArrayWrappedCall(
    parameter: WinMdParameter,
    abiCall: CodeBlock,
    returnsValue: Boolean,
): CodeBlock {
    val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
    val bufferName = int32FillArrayBufferName(parameter)
    val bufferInitializer = CodeBlock.of("IntArray(%N.size) { index -> %N[index].value }", parameterName, parameterName)
    val copyBack = CodeBlock.of(
        "%N.forEachIndexed { index, value -> %N[index] = %T(value) }",
        bufferName,
        parameterName,
        PoetSymbols.int32Class,
    )
    return CodeBlock.builder()
        .add("run {\n")
        .indent()
        .add("val %N = %L\n", bufferName, bufferInitializer)
        .apply {
            if (returnsValue) {
                add("%L.also {\n", abiCall)
                indent()
                add("%L\n", copyBack)
                unindent()
                add("}\n")
            } else {
                add("%L\n", abiCall)
                add("%L\n", copyBack)
            }
        }
        .unindent()
        .add("}")
        .build()
}
