package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isInt32PassArrayParameter(): Boolean =
    type == "Int32[]" && arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isSingleInt32PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    arrayReturnCategory() == null &&
        parameters.size == 1 &&
        parameters.single().isInt32PassArrayParameter() &&
        (returnType == "Unit" || supportsObjectReturn(returnType))

internal fun int32PassArrayAbiArguments(argumentName: String): List<CodeBlock> = listOf(
    CodeBlock.of("%N.size", argumentName),
    CodeBlock.of("IntArray(%N.size) { index -> %N[index].value }", argumentName, argumentName),
)
