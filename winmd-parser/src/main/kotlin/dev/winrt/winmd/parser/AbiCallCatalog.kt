package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock

internal object AbiCallCatalog {
    private data class NormalizedAbiArgument(
        val descriptor: MethodParameterAbiDescriptor,
        val expression: Any,
    )

    private val unaryReturnsUsingUInt32ForInt32 = setOf("Object", "UInt64", "Boolean", "Float32", "Float64")

    private fun normalizedUnaryArgument(
        returnDescriptor: MethodReturnAbiDescriptor,
        parameterCategory: MethodParameterCategory,
        argumentExpression: Any,
    ): NormalizedAbiArgument =
        when (parameterCategory) {
            MethodParameterCategory.STRING -> NormalizedAbiArgument(MethodParameterAbiToken.STRING, argumentExpression)
            MethodParameterCategory.OBJECT -> NormalizedAbiArgument(MethodParameterAbiToken.OBJECT, argumentExpression)
            MethodParameterCategory.INT64,
            MethodParameterCategory.EVENT_REGISTRATION_TOKEN,
            -> NormalizedAbiArgument(MethodParameterAbiToken.INT64, argumentExpression)
            MethodParameterCategory.INT32 ->
                if (returnDescriptor.methodNamePart in unaryReturnsUsingUInt32ForInt32) {
                    NormalizedAbiArgument(MethodParameterAbiToken.UINT32, CodeBlock.of("%L.toUInt()", argumentExpression))
                } else {
                    NormalizedAbiArgument(MethodParameterAbiToken.INT32, argumentExpression)
                }
            MethodParameterCategory.UINT32 ->
                if (returnDescriptor.methodNamePart == MethodAbiToken.GUID.methodNamePart) {
                    NormalizedAbiArgument(MethodParameterAbiToken.INT32, CodeBlock.of("%L.toInt()", argumentExpression))
                } else {
                    NormalizedAbiArgument(MethodParameterAbiToken.UINT32, argumentExpression)
                }
            MethodParameterCategory.BOOLEAN ->
                if (returnDescriptor.methodNamePart == MethodAbiToken.GUID.methodNamePart) {
                    NormalizedAbiArgument(MethodParameterAbiToken.INT32, CodeBlock.of("if (%L) 1 else 0", argumentExpression))
                } else {
                    NormalizedAbiArgument(MethodParameterAbiToken.UINT32, CodeBlock.of("if (%L) 1u else 0u", argumentExpression))
                }
        }

    private fun normalizedTwoArgument(
        parameterCategory: MethodParameterCategory,
        argumentExpression: Any,
    ): NormalizedAbiArgument =
        when (parameterCategory) {
            MethodParameterCategory.STRING -> NormalizedAbiArgument(MethodParameterAbiToken.STRING, argumentExpression)
            MethodParameterCategory.OBJECT -> NormalizedAbiArgument(MethodParameterAbiToken.OBJECT, argumentExpression)
            MethodParameterCategory.INT32 -> NormalizedAbiArgument(MethodParameterAbiToken.INT32, argumentExpression)
            MethodParameterCategory.UINT32 -> NormalizedAbiArgument(MethodParameterAbiToken.INT32, CodeBlock.of("%L.toInt()", argumentExpression))
            MethodParameterCategory.BOOLEAN -> NormalizedAbiArgument(MethodParameterAbiToken.INT32, CodeBlock.of("if (%L) 1 else 0", argumentExpression))
            MethodParameterCategory.INT64,
            MethodParameterCategory.EVENT_REGISTRATION_TOKEN,
            -> NormalizedAbiArgument(MethodParameterAbiToken.INT64, argumentExpression)
        }

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
        parameterCategory: MethodParameterCategory,
        vtableIndex: Int,
        argumentExpression: Any,
        pointerExpression: String = "pointer",
    ): CodeBlock {
        val normalized = normalizedUnaryArgument(returnDescriptor, parameterCategory, argumentExpression)
        val methodName = buildString {
            append(returnDescriptor.methodNamePart)
            append("MethodWith")
            append(normalized.descriptor.methodNamePart)
            append("Arg")
        }
        return singleArgumentCall(
            methodName = methodName,
            vtableIndex = vtableIndex,
            argumentExpression = normalized.expression,
            pointerExpression = pointerExpression,
            placeholder = normalized.descriptor.argumentPlaceholder,
        )
    }

    private fun twoArgumentMethodName(prefix: String, parameterCategories: List<MethodParameterCategory>): String =
        buildString {
            val abiDescriptors = parameterCategories.map { normalizedTwoArgument(it, CodeBlock.of("0")).descriptor }
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
        val normalized = listOf(
            normalizedTwoArgument(parameterCategories[0], firstArgumentExpression),
            normalizedTwoArgument(parameterCategories[1], secondArgumentExpression),
        )
        return CodeBlock.of(
            buildString {
                append("%T.")
                append(methodName)
                append("(pointer, %L, ")
                append(normalized[0].descriptor.argumentPlaceholder)
                append(", ")
                append(normalized[1].descriptor.argumentPlaceholder)
                append(").getOrThrow()")
            },
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            normalized[0].expression,
            normalized[1].expression,
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
        val normalized = listOf(
            normalizedTwoArgument(parameterCategories[0], firstArgumentExpression),
            normalizedTwoArgument(parameterCategories[1], secondArgumentExpression),
        )
        return CodeBlock.of(
            buildString {
                append("%T.")
                append(methodName)
                append("(")
                append(pointerExpression)
                append(", %L, %T.%L, ")
                append(normalized[0].descriptor.argumentPlaceholder)
                append(", ")
                append(normalized[1].descriptor.argumentPlaceholder)
                append(").getOrThrow().%M()")
            },
            PoetSymbols.platformComInteropClass,
            vtableIndex,
            PoetSymbols.comMethodResultKindClass,
            resultKindName,
            normalized[0].expression,
            normalized[1].expression,
            extractor,
        )
    }

    fun hstringMethod(vtableIndex: Int, pointerExpression: String = "pointer"): CodeBlock =
        CodeBlock.of("%T.invokeHStringMethod($pointerExpression, $vtableIndex).getOrThrow()", PoetSymbols.platformComInteropClass)

    fun hstringMethodWithUInt32(vtableIndex: Int, argumentExpression: Any, pointerExpression: String = "pointer"): CodeBlock =
        unaryCall(MethodAbiToken.HSTRING, MethodParameterCategory.UINT32, vtableIndex, argumentExpression, pointerExpression)

    fun unitMethod(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeUnitMethod(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun unitMethodWithInt32Expression(vtableIndex: Int, argumentExpression: String): CodeBlock =
        unaryCall(MethodAbiToken.UNIT, MethodParameterCategory.INT32, vtableIndex, argumentExpression)

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
        unaryCall(MethodAbiToken.INT64, MethodParameterCategory.OBJECT, vtableIndex, argumentExpression, pointerExpression)

    fun objectMethod(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeObjectMethod(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun objectMethodWithString(vtableIndex: Int, argumentName: String): CodeBlock =
        unaryCall(MethodAbiToken.OBJECT, MethodParameterCategory.STRING, vtableIndex, argumentName)

    fun objectMethodWithUInt32(vtableIndex: Int, argumentExpression: Any): CodeBlock =
        unaryCall(MethodAbiToken.OBJECT, MethodParameterCategory.UINT32, vtableIndex, argumentExpression)

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
    ): CodeBlock = unaryCall(returnDescriptor, parameterCategory, vtableIndex, argumentExpression, pointerExpression)

    fun booleanMethod(vtableIndex: Int): CodeBlock =
        CodeBlock.of("%T.invokeBooleanGetter(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)

    fun booleanMethodWithObject(vtableIndex: Int, argumentExpression: Any): CodeBlock =
        unaryCall(MethodAbiToken.BOOLEAN, MethodParameterCategory.OBJECT, vtableIndex, argumentExpression)

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
