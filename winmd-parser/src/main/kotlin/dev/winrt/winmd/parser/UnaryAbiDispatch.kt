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
        returnDescriptor = returnKind.unaryMethodAbiDescriptor(),
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
        returnDescriptor = MethodReturnAbiDescriptor("Unit"),
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

internal fun MethodReturnKind.unaryMethodAbiDescriptor(): MethodReturnAbiDescriptor = when (this) {
    MethodReturnKind.STRING -> MethodReturnAbiDescriptor("HString")
    MethodReturnKind.FLOAT32 -> MethodReturnAbiDescriptor("Float32")
    MethodReturnKind.FLOAT64 -> MethodReturnAbiDescriptor("Float64")
    MethodReturnKind.DATE_TIME,
    MethodReturnKind.TIME_SPAN,
    MethodReturnKind.INT64,
    MethodReturnKind.EVENT_REGISTRATION_TOKEN -> MethodReturnAbiDescriptor("Int64")
    MethodReturnKind.BOOLEAN -> MethodReturnAbiDescriptor("Boolean")
    MethodReturnKind.INT32 -> MethodReturnAbiDescriptor("Int32")
    MethodReturnKind.UINT32 -> MethodReturnAbiDescriptor("UInt32")
    MethodReturnKind.UINT64 -> MethodReturnAbiDescriptor("UInt64")
    MethodReturnKind.GUID -> MethodReturnAbiDescriptor("Guid")
    MethodReturnKind.OBJECT -> MethodReturnAbiDescriptor("Object")
    MethodReturnKind.UNIT -> MethodReturnAbiDescriptor("Unit")
}
