package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock

internal object AbiCallCatalog {
    private fun MethodParameterAbiToken.methodNamePart(): String = when (this) {
        MethodParameterAbiToken.STRING -> "String"
        MethodParameterAbiToken.OBJECT -> "Object"
        MethodParameterAbiToken.INT32 -> "Int32"
        MethodParameterAbiToken.INT64 -> "Int64"
    }

    private fun MethodParameterAbiToken.argumentPlaceholder(): String = when (this) {
        MethodParameterAbiToken.STRING -> "%N"
        MethodParameterAbiToken.OBJECT,
        MethodParameterAbiToken.INT32,
        MethodParameterAbiToken.INT64 -> "%L"
    }

    private fun twoArgumentMethodName(prefix: String, parameterCategories: List<MethodParameterCategory>): String =
        buildString {
            val abiTokens = parameterCategories.map(MethodParameterCategory::toAbiToken)
            append(prefix)
            append(
                if (abiTokens[0] == abiTokens[1]) {
                    "Two${abiTokens[0].methodNamePart()}"
                } else {
                    abiTokens.joinToString("And") { it.methodNamePart() }
                },
            )
            append("Args")
        }

    private fun unitTwoArgumentCall(
        methodName: String,
        vtableIndex: Int,
        parameterCategories: List<MethodParameterCategory>,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
    ): CodeBlock {
        val abiTokens = parameterCategories.map(MethodParameterCategory::toAbiToken)
        return CodeBlock.of(
            buildString {
                append("%T.")
                append(methodName)
                append("(pointer, %L, ")
                append(abiTokens[0].argumentPlaceholder())
                append(", ")
                append(abiTokens[1].argumentPlaceholder())
                append(").getOrThrow()")
            },
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            firstArgumentExpression,
            secondArgumentExpression,
        )
    }

    private fun resultTwoArgumentCall(
        methodName: String,
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        parameterCategories: List<MethodParameterCategory>,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String,
    ): CodeBlock {
        val abiTokens = parameterCategories.map(MethodParameterCategory::toAbiToken)
        return CodeBlock.of(
            buildString {
                append("%T.")
                append(methodName)
                append("(")
                append(pointerExpression)
                append(", %L, %T.%L, ")
                append(abiTokens[0].argumentPlaceholder())
                append(", ")
                append(abiTokens[1].argumentPlaceholder())
                append(").getOrThrow().%M()")
            },
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            PoetSymbols.comMethodResultKindClass,
            resultKindName,
            firstArgumentExpression,
            secondArgumentExpression,
            extractor,
        )
    }

    fun hstringMethod(vtableIndex: Int, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeHStringMethod($pointerExpression, $vtableIndex).getOrThrow()", PoetSymbols.platformComInteropClass)

    fun hstringMethodWithString(vtableIndex: Int, argumentName: String, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeHStringMethodWithStringArg($pointerExpression, $vtableIndex, %N).getOrThrow()", PoetSymbols.platformComInteropClass, argumentName)

    fun hstringMethodWithInt32(vtableIndex: Int, argumentName: String, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeHStringMethodWithInt32Arg($pointerExpression, $vtableIndex, %L).getOrThrow()", PoetSymbols.platformComInteropClass, argumentName)

    fun hstringMethodWithUInt32(vtableIndex: Int, argumentName: String, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeHStringMethodWithUInt32Arg($pointerExpression, $vtableIndex, %L).getOrThrow()", PoetSymbols.platformComInteropClass, argumentName)

    fun hstringMethodWithBoolean(vtableIndex: Int, argumentExpression: String, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeHStringMethodWithBooleanArg($pointerExpression, $vtableIndex, %L).getOrThrow()", PoetSymbols.platformComInteropClass, argumentExpression)

    fun hstringMethodWithObject(vtableIndex: Int, argumentExpression: String, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeHStringMethodWithObjectArg($pointerExpression, $vtableIndex, %L).getOrThrow()", PoetSymbols.platformComInteropClass, argumentExpression)

    fun hstringMethodWithInt64(vtableIndex: Int, argumentExpression: String, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeHStringMethodWithInt64Arg($pointerExpression, $vtableIndex, %L).getOrThrow()", PoetSymbols.platformComInteropClass, argumentExpression)

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
        parameterCategories: List<MethodParameterCategory>,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
    ): CodeBlock = unitTwoArgumentCall(
        methodName = twoArgumentMethodName("invokeUnitMethodWith", parameterCategories),
        vtableIndex = vtableIndex,
        parameterCategories = parameterCategories,
        firstArgumentExpression = firstArgumentExpression,
        secondArgumentExpression = secondArgumentExpression,
    )

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

    fun objectMethodWithInt32(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeObjectMethodWithInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun objectMethodWithBoolean(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeObjectMethodWithBooleanArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun objectMethodWithInt64(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeObjectMethodWithInt64Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

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

    fun resultMethodWithObjectAndInt32(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithObjectAndInt32Args($pointerExpression, $vtableIndex, %T.%L, %L, %L).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithInt32AndObject(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithInt32AndObjectArgs($pointerExpression, $vtableIndex, %T.%L, %L, %L).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithObjectAndInt64(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithObjectAndInt64Args($pointerExpression, $vtableIndex, %T.%L, %L, %L).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithInt64AndObject(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithInt64AndObjectArgs($pointerExpression, $vtableIndex, %T.%L, %L, %L).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithStringAndInt32(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithStringAndInt32Args($pointerExpression, $vtableIndex, %T.%L, %N, %L).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithInt32AndString(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithInt32AndStringArgs($pointerExpression, $vtableIndex, %T.%L, %L, %N).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithStringAndInt64(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithStringAndInt64Args($pointerExpression, $vtableIndex, %T.%L, %N, %L).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithInt64AndString(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithInt64AndStringArgs($pointerExpression, $vtableIndex, %T.%L, %L, %N).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithTwoInt32s(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithTwoInt32Args($pointerExpression, $vtableIndex, %T.%L, %L, %L).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithInt32AndInt64(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithInt32AndInt64Args($pointerExpression, $vtableIndex, %T.%L, %L, %L).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithInt64AndInt32(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithInt64AndInt32Args($pointerExpression, $vtableIndex, %T.%L, %L, %L).getOrThrow().%M()",
        PoetSymbols.platformComInteropClass,
        PoetSymbols.comMethodResultKindClass,
        resultKindName,
        firstArgumentExpression,
        secondArgumentExpression,
        extractor,
    )

    fun resultMethodWithTwoInt64s(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = CodeBlock.of(
        "%T.invokeMethodWithTwoInt64Args($pointerExpression, $vtableIndex, %T.%L, %L, %L).getOrThrow().%M()",
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
        parameterCategories: List<MethodParameterCategory>,
        firstArgumentExpression: String,
        secondArgumentExpression: String,
        pointerExpression: String = "pointer",
    ): CodeBlock = resultTwoArgumentCall(
        methodName = twoArgumentMethodName("invokeMethodWith", parameterCategories),
        vtableIndex = vtableIndex,
        resultKindName = resultKindName,
        extractor = extractor,
        parameterCategories = parameterCategories,
        firstArgumentExpression = firstArgumentExpression,
        secondArgumentExpression = secondArgumentExpression,
        pointerExpression = pointerExpression,
    )

    fun booleanMethod(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeBooleanGetter(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun booleanMethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeBooleanMethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun booleanMethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeBooleanMethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun booleanMethodWithInt32(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeBooleanMethodWithInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun booleanMethodWithBoolean(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeBooleanMethodWithBooleanArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun booleanMethodWithInt64(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeBooleanMethodWithInt64Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun float64Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeFloat64Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun float32Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeFloat32Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun float32MethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat32MethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun float32MethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat32MethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun float32MethodWithInt32(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat32MethodWithInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun float32MethodWithBoolean(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat32MethodWithBooleanArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun float32MethodWithObject(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat32MethodWithObjectArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun float32MethodWithInt64(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat32MethodWithInt64Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun float64MethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat64MethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun float64MethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat64MethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun float64MethodWithInt32(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat64MethodWithInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun float64MethodWithBoolean(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat64MethodWithBooleanArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun float64MethodWithObject(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat64MethodWithObjectArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun float64MethodWithInt64(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeFloat64MethodWithInt64Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun uint32Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeUInt32Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun uint32MethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt32MethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun uint32MethodWithInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt32MethodWithInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun uint32MethodWithUInt32(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt32MethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun uint32MethodWithBoolean(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt32MethodWithBooleanArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun uint32MethodWithInt64(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeUInt32MethodWithInt64Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

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

    fun int32MethodWithBoolean(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeInt32MethodWithBooleanArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun int32MethodWithInt64(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeInt32MethodWithInt64Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun int32MethodWithObject(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeInt32MethodWithObjectArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun int64Getter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeInt64Getter(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun guidGetter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeGuidGetter(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun guidMethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        CodeBlock.of("%T.invokeGuidMethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)

    fun guidMethodWithInt32(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeGuidMethodWithInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun guidMethodWithUInt32(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeGuidMethodWithUInt32Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun guidMethodWithBoolean(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeGuidMethodWithBooleanArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun guidMethodWithObject(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeGuidMethodWithObjectArg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

    fun guidMethodWithInt64(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeGuidMethodWithInt64Arg(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

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
