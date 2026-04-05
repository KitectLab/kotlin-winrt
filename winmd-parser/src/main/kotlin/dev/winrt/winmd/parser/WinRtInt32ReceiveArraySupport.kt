package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod

internal fun WinMdMethod.isInt32ReceiveArrayReturnMethod(): Boolean =
    returnType == "Int32[]" &&
        arrayReturnCategory() == WinRtArrayParameterCategory.RECEIVE_ARRAY &&
        parameters.isEmpty()

internal fun int32ReceiveArrayReturnExpression(vtableIndex: Int): CodeBlock = CodeBlock.of(
    "%M(pointer, %L).getOrThrow().map { %T(it) }.toTypedArray()",
    PoetSymbols.invokeInt32ReceiveArrayMethodMember,
    vtableIndex,
    PoetSymbols.int32Class,
)
