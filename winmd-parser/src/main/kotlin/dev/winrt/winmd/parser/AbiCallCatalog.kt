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

    fun unitMethodWithTwoStrings(vtableIndex: Int, firstArgumentName: String, secondArgumentName: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithTwoStringArgs(pointer, %L, %N, %N).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentName,
            secondArgumentName,
        )

    fun unitMethodWithStringAndInt32(vtableIndex: Int, firstArgumentName: String, secondArgumentExpression: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithStringAndInt32Args(pointer, %L, %N, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentName,
            secondArgumentExpression,
        )

    fun unitMethodWithInt32AndString(vtableIndex: Int, firstArgumentExpression: String, secondArgumentName: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithInt32AndStringArgs(pointer, %L, %L, %N).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentExpression,
            secondArgumentName,
        )

    fun unitMethodWithTwoInt32s(vtableIndex: Int, firstArgumentExpression: String, secondArgumentExpression: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithTwoInt32Args(pointer, %L, %L, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentExpression,
            secondArgumentExpression,
        )

    fun unitMethodWithInt32AndInt64(vtableIndex: Int, firstArgumentExpression: String, secondArgumentExpression: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithInt32AndInt64Args(pointer, %L, %L, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentExpression,
            secondArgumentExpression,
        )

    fun unitMethodWithInt64AndInt32(vtableIndex: Int, firstArgumentExpression: String, secondArgumentExpression: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithInt64AndInt32Args(pointer, %L, %L, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentExpression,
            secondArgumentExpression,
        )

    fun unitMethodWithTwoInt64s(vtableIndex: Int, firstArgumentExpression: String, secondArgumentExpression: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithTwoInt64Args(pointer, %L, %L, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentExpression,
            secondArgumentExpression,
        )

    fun unitMethodWithObjectAndString(vtableIndex: Int, firstArgumentExpression: String, secondArgumentName: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithObjectAndStringArgs(pointer, %L, %L, %N).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentExpression,
            secondArgumentName,
        )

    fun unitMethodWithStringAndObject(vtableIndex: Int, firstArgumentName: String, secondArgumentExpression: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithStringAndObjectArgs(pointer, %L, %N, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentName,
            secondArgumentExpression,
        )

    fun unitMethodWithObjectAndInt32(vtableIndex: Int, firstArgumentExpression: String, secondArgumentExpression: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithObjectAndInt32Args(pointer, %L, %L, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentExpression,
            secondArgumentExpression,
        )

    fun unitMethodWithInt32AndObject(vtableIndex: Int, firstArgumentExpression: String, secondArgumentExpression: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithInt32AndObjectArgs(pointer, %L, %L, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentExpression,
            secondArgumentExpression,
        )

    fun unitMethodWithObjectAndInt64(vtableIndex: Int, firstArgumentExpression: String, secondArgumentExpression: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithObjectAndInt64Args(pointer, %L, %L, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentExpression,
            secondArgumentExpression,
        )

    fun unitMethodWithInt64AndObject(vtableIndex: Int, firstArgumentExpression: String, secondArgumentExpression: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithInt64AndObjectArgs(pointer, %L, %L, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentExpression,
            secondArgumentExpression,
        )

    fun unitMethodWithTwoObject(vtableIndex: Int, firstArgumentExpression: String, secondArgumentExpression: String): CodeBlock =
        CodeBlock.of(
            "%T.invokeUnitMethodWithTwoObjectArgs(pointer, %L, %L, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentExpression,
            secondArgumentExpression,
        )

    fun unitMethodWithTwoArguments(
        vtableIndex: Int,
        parameterPair: MethodParameterPair,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
    ): CodeBlock = when (parameterPair) {
        MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.INT32) ->
            unitMethodWithStringAndInt32(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.STRING) ->
            unitMethodWithInt32AndString(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.UINT32) ->
            unitMethodWithStringAndInt32(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.STRING) ->
            unitMethodWithInt32AndString(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.BOOLEAN) ->
            unitMethodWithStringAndInt32(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.STRING) ->
            unitMethodWithInt32AndString(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.INT64) ->
            CodeBlock.of("%T.invokeUnitMethodWithStringAndInt64Args(pointer, %L, %N, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.STRING) ->
            CodeBlock.of("%T.invokeUnitMethodWithInt64AndStringArgs(pointer, %L, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) ->
            CodeBlock.of("%T.invokeUnitMethodWithStringAndInt64Args(pointer, %L, %N, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.STRING) ->
            CodeBlock.of("%T.invokeUnitMethodWithInt64AndStringArgs(pointer, %L, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.STRING) ->
            unitMethodWithTwoStrings(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.INT32),
        MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.UINT32),
        MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.BOOLEAN),
        MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.INT32),
        MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.UINT32),
        MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.BOOLEAN),
        MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.INT32),
        MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.UINT32),
        MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.BOOLEAN) ->
            unitMethodWithTwoInt32s(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.INT64),
        MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.EVENT_REGISTRATION_TOKEN),
        MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.INT64),
        MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.EVENT_REGISTRATION_TOKEN),
        MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.INT64),
        MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) ->
            unitMethodWithInt32AndInt64(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.INT32),
        MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.UINT32),
        MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.BOOLEAN),
        MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.INT32),
        MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.UINT32),
        MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.BOOLEAN) ->
            unitMethodWithInt64AndInt32(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.INT64),
        MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.EVENT_REGISTRATION_TOKEN),
        MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.INT64),
        MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) ->
            unitMethodWithTwoInt64s(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.INT32),
        MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.UINT32),
        MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.BOOLEAN) ->
            unitMethodWithObjectAndInt32(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.INT64),
        MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) ->
            unitMethodWithObjectAndInt64(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.OBJECT),
        MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.OBJECT),
        MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.OBJECT) ->
            unitMethodWithInt32AndObject(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.OBJECT),
        MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.OBJECT) ->
            unitMethodWithInt64AndObject(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.STRING) ->
            unitMethodWithObjectAndString(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.OBJECT) ->
            unitMethodWithStringAndObject(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.OBJECT) ->
            unitMethodWithTwoObject(vtableIndex, firstArgumentExpression, secondArgumentExpression)
        else -> error("Unsupported two-argument unit categories: $parameterPair")
    }

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

    fun objectMethodWithObject(vtableIndex: Int, argumentExpression: String, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of(
            "%T.invokeObjectMethodWithObjectArg($pointerExpression, $vtableIndex, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            argumentExpression,
        )

    fun objectMethodWithObjectAndString(
        vtableIndex: Int,
        firstArgumentExpression: String,
        secondArgumentName: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithObjectAndStringArgs($pointerExpression, $vtableIndex, %T.OBJECT, %L, %N).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        firstArgumentExpression,
        secondArgumentName,
        PoetSymbols.requireObjectMember,
    )

    fun objectMethodWithStringAndObject(
        vtableIndex: Int,
        firstArgumentName: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithStringAndObjectArgs($pointerExpression, $vtableIndex, %T.OBJECT, %N, %L).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        firstArgumentName,
        secondArgumentExpression,
        PoetSymbols.requireObjectMember,
    )

    fun resultMethodWithObjectAndString(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentName: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithObjectAndStringArgs($pointerExpression, $vtableIndex, %T.%L, %L, %N).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentName,
        extractor,
    )

    fun resultMethodWithStringAndObject(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentName: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithStringAndObjectArgs($pointerExpression, $vtableIndex, %T.%L, %N, %L).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentName,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithTwoObject(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithTwoObjectArgs($pointerExpression, $vtableIndex, %T.%L, %L, %L).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithTwoArguments(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        parameterPair: MethodParameterPair,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = when (parameterPair) {
        MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.STRING) ->
            resultMethodWithObjectAndString(
                vtableIndex,
                resultKindName,
                extractor,
                firstArgumentExpression,
                secondArgumentExpression,
                pointerExpression,
            )
        MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.OBJECT) ->
            resultMethodWithStringAndObject(
                vtableIndex,
                resultKindName,
                extractor,
                firstArgumentExpression,
                secondArgumentExpression,
                pointerExpression,
            )
        MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.OBJECT) ->
            resultMethodWithTwoObject(
                vtableIndex,
                resultKindName,
                extractor,
                firstArgumentExpression,
                secondArgumentExpression,
                pointerExpression,
            )
        else -> error("Unsupported two-argument result categories: $parameterPair")
    }

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
