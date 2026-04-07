package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock

internal fun zeroArgumentUnaryAbiCall(
    vtableIndex: Int,
    returnKind: MethodReturnKind,
): CodeBlock = when (returnKind) {
    MethodReturnKind.STRING -> AbiCallCatalog.hstringMethod(vtableIndex)
    MethodReturnKind.FLOAT32 -> AbiCallCatalog.float32Method(vtableIndex)
    MethodReturnKind.FLOAT64 -> AbiCallCatalog.float64Method(vtableIndex)
    MethodReturnKind.DATE_TIME,
    MethodReturnKind.TIME_SPAN,
    MethodReturnKind.INT64,
    MethodReturnKind.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.int64Getter(vtableIndex)
    MethodReturnKind.BOOLEAN -> AbiCallCatalog.booleanMethod(vtableIndex)
    MethodReturnKind.INT32 -> AbiCallCatalog.int32Method(vtableIndex)
    MethodReturnKind.UINT32 -> AbiCallCatalog.uint32Method(vtableIndex)
    MethodReturnKind.UINT64 -> CodeBlock.of(
        "%T.invokeInt64Getter(pointer, %L).getOrThrow().toULong()",
        PoetSymbols.platformComInteropClass,
        vtableIndex,
    )
    MethodReturnKind.GUID -> AbiCallCatalog.guidGetter(vtableIndex)
    MethodReturnKind.OBJECT -> AbiCallCatalog.objectMethod(vtableIndex)
    MethodReturnKind.UNIT -> AbiCallCatalog.unitMethod(vtableIndex)
}

internal fun defaultUnaryAbiCall(
    vtableIndex: Int,
    returnKind: MethodReturnKind,
    parameterCategory: MethodParameterCategory,
    argumentName: String,
    loweredArgument: Any,
    unsupportedMessage: String,
): CodeBlock = when {
    returnKind == MethodReturnKind.UNIT ->
        unitUnaryAbiCall(vtableIndex, parameterCategory, argumentName, loweredArgument)
    returnKind == MethodReturnKind.UINT64 &&
        (parameterCategory == MethodParameterCategory.INT64 ||
            parameterCategory == MethodParameterCategory.EVENT_REGISTRATION_TOKEN) ->
        error(unsupportedMessage)
    else -> AbiCallCatalog.unaryMethod(
        returnToken = returnKind.unaryMethodAbiToken(),
        parameterCategory = parameterCategory,
        vtableIndex = vtableIndex,
        argumentExpression = unaryAbiArgument(parameterCategory, argumentName, loweredArgument),
    )
}

private fun unitUnaryAbiCall(
    vtableIndex: Int,
    parameterCategory: MethodParameterCategory,
    argumentName: String,
    loweredArgument: Any,
): CodeBlock = when (parameterCategory) {
    MethodParameterCategory.BOOLEAN ->
        AbiCallCatalog.unitMethodWithInt32Expression(vtableIndex, "if ($loweredArgument) 1 else 0")
    MethodParameterCategory.OBJECT -> AbiCallCatalog.objectSetterExpression(vtableIndex, loweredArgument)
    else -> AbiCallCatalog.unaryMethod(
        returnToken = MethodAbiToken.UNIT,
        parameterCategory = parameterCategory,
        vtableIndex = vtableIndex,
        argumentExpression = unaryAbiArgument(parameterCategory, argumentName, loweredArgument),
    )
}

private fun unaryAbiArgument(
    parameterCategory: MethodParameterCategory,
    argumentName: String,
    loweredArgument: Any,
): Any = if (parameterCategory == MethodParameterCategory.STRING) argumentName else loweredArgument

internal fun MethodReturnKind.unaryMethodAbiToken(): MethodAbiToken = when (this) {
    MethodReturnKind.STRING -> MethodAbiToken.HSTRING
    MethodReturnKind.FLOAT32 -> MethodAbiToken.FLOAT32
    MethodReturnKind.FLOAT64 -> MethodAbiToken.FLOAT64
    MethodReturnKind.DATE_TIME,
    MethodReturnKind.TIME_SPAN,
    MethodReturnKind.INT64,
    MethodReturnKind.EVENT_REGISTRATION_TOKEN -> MethodAbiToken.INT64
    MethodReturnKind.BOOLEAN -> MethodAbiToken.BOOLEAN
    MethodReturnKind.INT32 -> MethodAbiToken.INT32
    MethodReturnKind.UINT32 -> MethodAbiToken.UINT32
    MethodReturnKind.UINT64 -> MethodAbiToken.UINT64
    MethodReturnKind.GUID -> MethodAbiToken.GUID
    MethodReturnKind.OBJECT -> MethodAbiToken.OBJECT
    MethodReturnKind.UNIT -> MethodAbiToken.UNIT
}
