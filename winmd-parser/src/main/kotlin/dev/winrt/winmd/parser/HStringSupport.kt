package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock

internal object HStringSupport {
    fun toKotlinString(pointerExpression: String, vtableIndex: Int): CodeBlock {
        return CodeBlock.of(
            "%T.invokeHStringMethod($pointerExpression, $vtableIndex).getOrThrow().use { it.toKotlinString() }",
            PoetSymbols.platformComInteropClass,
        )
    }

    fun toKotlinStringWithStringArg(pointerExpression: String, vtableIndex: Int, argumentName: String): CodeBlock {
        return CodeBlock.of(
            "%T.invokeHStringMethodWithStringArg($pointerExpression, $vtableIndex, $argumentName).getOrThrow().use { it.toKotlinString() }",
            PoetSymbols.platformComInteropClass,
        )
    }

    fun toKotlinStringWithInt32Arg(pointerExpression: String, vtableIndex: Int, argumentName: String): CodeBlock {
        return CodeBlock.of(
            "%T.invokeHStringMethodWithInt32Arg($pointerExpression, $vtableIndex, $argumentName).getOrThrow().use { it.toKotlinString() }",
            PoetSymbols.platformComInteropClass,
        )
    }

    fun toKotlinStringWithUInt32Arg(pointerExpression: String, vtableIndex: Int, argumentName: String): CodeBlock {
        return CodeBlock.of(
            "%T.invokeHStringMethodWithUInt32Arg($pointerExpression, $vtableIndex, $argumentName).getOrThrow().use { it.toKotlinString() }",
            PoetSymbols.platformComInteropClass,
        )
    }
}
