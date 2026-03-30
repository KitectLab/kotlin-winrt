package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock

internal object AbiCallCatalog {
    fun hstringMethod(vtableIndex: Int, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeHStringMethod($pointerExpression, $vtableIndex).getOrThrow()", PoetSymbols.platformComInteropClass)

    fun hstringMethodWithString(vtableIndex: Int, argumentName: String, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeHStringMethodWithStringArg($pointerExpression, $vtableIndex, %N).getOrThrow()", PoetSymbols.platformComInteropClass, argumentName)

    fun hstringMethodWithInt32(vtableIndex: Int, argumentName: String, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeHStringMethodWithInt32Arg($pointerExpression, $vtableIndex, %L).getOrThrow()", PoetSymbols.platformComInteropClass, argumentName)

    fun hstringMethodWithUInt32(vtableIndex: Int, argumentName: String, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeHStringMethodWithUInt32Arg($pointerExpression, $vtableIndex, %L).getOrThrow()", PoetSymbols.platformComInteropClass, argumentName)

    fun unitMethod(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeUnitMethod(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun unitMethodWithInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUnitMethodWithInt32Arg(pointer, %L, %N.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun unitMethodWithInt32Expression(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeUnitMethodWithInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun unitMethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUnitMethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun unitMethodWithInt64(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUnitMethodWithInt64Arg(pointer, %L, %N.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun unitMethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUnitMethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun objectSetter(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeObjectSetter(pointer, %L, (%N as %T).pointer).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            argumentName,
            PoetSymbols.inspectableClass,
        )

    fun int64MethodWithObject(vtableIndex: Int, argumentExpression: String, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of(
            "%T.invokeInt64MethodWithObjectArg($pointerExpression, $vtableIndex, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            argumentExpression,
        )

    fun int64Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeInt64Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun int64MethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeInt64MethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun int64MethodWithInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeInt64MethodWithInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun int64MethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeInt64MethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun int64MethodWithBoolean(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeInt64MethodWithBooleanArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun uint64Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeUInt64Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun uint64MethodWithObject(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt64MethodWithObjectArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun uint64MethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt64MethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun uint64MethodWithInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt64MethodWithInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun uint64MethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt64MethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun uint64MethodWithBoolean(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt64MethodWithBooleanArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun objectMethod(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeObjectMethod(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun objectMethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeObjectMethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun objectMethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeObjectMethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun booleanMethod(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeBooleanGetter(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun booleanMethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeBooleanMethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun booleanMethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeBooleanMethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun float64Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeFloat64Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun float32Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeFloat32Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun float32MethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat32MethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun float32MethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat32MethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun float64MethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat64MethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun float64MethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat64MethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun uint32Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeUInt32Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun uint32MethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt32MethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun uint32MethodWithInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt32MethodWithInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun uint32MethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt32MethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun uint32MethodWithObject(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt32MethodWithObjectArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun int32Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeInt32Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun int32MethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeInt32MethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun int32MethodWithInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeInt32MethodWithInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun int32MethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeInt32MethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun int32MethodWithObject(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeInt32MethodWithObjectArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun int64Getter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeInt64Getter(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun guidGetter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeGuidGetter(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun stringSetter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeStringSetter(pointer, %L, value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun int32Setter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeInt32Setter(pointer, %L, value.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun uint32Setter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeUInt32Setter(pointer, %L, value.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun float32Setter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeFloat32Setter(pointer, %L, value.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun booleanSetter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeBooleanSetter(pointer, %L, value.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun float64Setter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeFloat64Setter(pointer, %L, value.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun int64Setter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeInt64Setter(pointer, %L, value.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun uint64Setter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeUInt64Setter(pointer, %L, value.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)
}
