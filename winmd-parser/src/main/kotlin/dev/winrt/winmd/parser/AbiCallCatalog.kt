package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock

internal object AbiCallCatalog {
    internal fun booleanGetter(vtableIndex: Int, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeBooleanGetter(%L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, pointerExpression, vtableIndex)

    internal fun booleanAsInt64Expression(argumentName: String): String = "if ($argumentName.value) 1L else 0L"
    internal fun booleanAsInt32Expression(argumentName: String): String = "if ($argumentName.value) 1 else 0"

    private fun singleArgumentCall(
        methodName: String,
        vtableIndex: Int,
        argumentExpression: Any,
        pointerExpression: String = "pointer",
        placeholder: String = "%L",
    ): CodeBlock =
        CodeBlock.of(
            "%T.${methodName.prependInvokePrefix()}($pointerExpression, %L, $placeholder).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            argumentExpression,
        )

    private fun unaryCall(
        returnDescriptor: MethodReturnAbiDescriptor,
        parameterDescriptor: MethodParameterAbiDescriptor,
        vtableIndex: Int,
        argumentExpression: Any,
        pointerExpression: String = "pointer",
    ): CodeBlock {
        val methodName = buildString {
            append(returnDescriptor.methodNamePart)
            append("MethodWith")
            append(parameterDescriptor.methodNamePart)
            append("Arg")
        }
        return singleArgumentCall(
            methodName = methodName,
            vtableIndex = vtableIndex,
            argumentExpression = argumentExpression,
            pointerExpression = pointerExpression,
            placeholder = parameterDescriptor.argumentPlaceholder,
        )
    }

    private fun twoArgumentMethodName(prefix: String, parameterCategories: List<MethodParameterCategory>): String =
        buildString {
            val abiDescriptors = parameterCategories.map(MethodParameterCategory::toAbiDescriptor)
            append(prefix)
            append(
                if (abiDescriptors[0].methodNamePart == abiDescriptors[1].methodNamePart) {
                    "Two${abiDescriptors[0].methodNamePart}"
                } else {
                    abiDescriptors.joinToString("And") { it.methodNamePart }
                },
            )
            append("Args")
        }

    private fun unitTwoArgumentCall(
        methodName: String,
        vtableIndex: Int,
        parameterCategories: List<MethodParameterCategory>,
        firstArgumentExpression: Any,
        secondArgumentExpression: Any,
    ): CodeBlock {
        val abiDescriptors = parameterCategories.map(MethodParameterCategory::toAbiDescriptor)
        return CodeBlock.of(
            buildString {
                append("%T.")
                append(methodName)
                append("(pointer, %L, ")
                append(abiDescriptors[0].argumentPlaceholder)
                append(", ")
                append(abiDescriptors[1].argumentPlaceholder)
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
        firstArgumentExpression: Any,
        secondArgumentExpression: Any,
        pointerExpression: String,
    ): CodeBlock {
        val abiDescriptors = parameterCategories.map(MethodParameterCategory::toAbiDescriptor)
        return CodeBlock.of(
            buildString {
                append("%T.")
                append(methodName)
                append("(")
                append(pointerExpression)
                append(", %L, %T.%L, ")
                append(abiDescriptors[0].argumentPlaceholder)
                append(", ")
                append(abiDescriptors[1].argumentPlaceholder)
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

    fun hstringMethodWithUInt32(vtableIndex: Int, argumentExpression: Any, pointerExpression: String = "pointer"): CodeBlock =
        unaryCall(MethodReturnAbiDescriptor("HString"), MethodParameterAbiDescriptor("UInt32"), vtableIndex, argumentExpression, pointerExpression)

    fun unitMethod(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeUnitMethod(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun unitMethodWithInt32Expression(vtableIndex: Int, argumentExpression: String): CodeBlock =
        unaryCall(MethodReturnAbiDescriptor("Unit"), MethodParameterAbiDescriptor("Int32"), vtableIndex, argumentExpression)

    fun unitMethodWithTwoArguments(
        vtableIndex: Int,
        parameterCategories: List<MethodParameterCategory>,
        firstArgumentExpression: Any,
        secondArgumentExpression: Any,
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

    fun objectSetterExpression(vtableIndex: Int, argumentExpression: Any): CodeBlock =
        CodeBlock.of(
            "%T.invokeObjectSetter(pointer, %L, %L).getOrThrow()",
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            argumentExpression,
        )

    fun int64MethodWithObject(vtableIndex: Int, argumentExpression: Any, pointerExpression: String = "pointer"): CodeBlock =
        unaryCall(MethodAbiToken.INT64, MethodParameterAbiToken.OBJECT, vtableIndex, argumentExpression, pointerExpression)

    fun objectMethod(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeObjectMethod(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun objectMethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        unaryCall(MethodAbiToken.OBJECT, MethodParameterAbiToken.STRING, vtableIndex, argumentName)

    fun objectMethodWithUInt32(vtableIndex: Int, argumentExpression: Any): CodeBlock =
        unaryCall(MethodAbiToken.OBJECT, MethodParameterAbiToken.UINT32, vtableIndex, argumentExpression)

    fun resultMethodWithTwoArguments(
        vtableIndex: Int,
        resultKindName: String,
        extractor: Any,
        parameterCategories: List<MethodParameterCategory>,
        firstArgumentExpression: Any,
        secondArgumentExpression: Any,
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

    fun unaryMethod(
        returnDescriptor: MethodReturnAbiDescriptor,
        parameterCategory: MethodParameterCategory,
        vtableIndex: Int,
        argumentExpression: Any,
        pointerExpression: String = "pointer",
    ): CodeBlock = unaryCall(returnDescriptor, parameterCategory.toAbiDescriptor(), vtableIndex, argumentExpression, pointerExpression)

    fun booleanMethod(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeBooleanGetter(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun booleanMethodWithObject(vtableIndex: Int, argumentExpression: Any): CodeBlock =
        unaryCall(MethodAbiToken.BOOLEAN, MethodParameterAbiToken.OBJECT, vtableIndex, argumentExpression)

    fun float64Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeFloat64Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun float32Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeFloat32Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun uint32Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeUInt32Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun int32Method(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeInt32Method(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun int64Getter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeInt64Getter(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun guidGetter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeGuidGetter(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    private fun String.prependInvokePrefix(): String =
        if (startsWith("invoke")) this else "invoke${replaceFirstChar(Char::uppercaseChar)}"

    fun stringSetter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeStringSetter(pointer, %L, value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun int32Setter(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeInt32Setter(pointer, %L, value.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun int32SetterExpression(vtableIndex: Int, argumentExpression: String): CodeBlock =
        CodeBlock.of("%T.invokeInt32Setter(pointer, %L, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentExpression)

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
