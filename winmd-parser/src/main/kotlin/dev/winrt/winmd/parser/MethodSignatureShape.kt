package dev.winrt.winmd.parser

internal enum class MethodSignatureShape {
    EMPTY,
    STRING,
    INT32,
    BOOLEAN,
    INT64,
    UINT32,
    EVENT_REGISTRATION_TOKEN,
    OBJECT,
    OBJECT_STRING,
    STRING_OBJECT,
    TWO_OBJECT,
}

internal enum class MethodParameterCategory {
    STRING,
    INT32,
    BOOLEAN,
    INT64,
    UINT32,
    EVENT_REGISTRATION_TOKEN,
    OBJECT,
}

internal enum class MethodReturnKind {
    UNIT,
    STRING,
    FLOAT32,
    FLOAT64,
    BOOLEAN,
    INT32,
    UINT32,
    INT64,
    UINT64,
    GUID,
    EVENT_REGISTRATION_TOKEN,
    OBJECT,
}

internal data class MethodSignatureKey(
    val returnKind: MethodReturnKind,
    val shape: MethodSignatureShape,
)

internal fun MethodSignatureShape.isTwoArgumentObjectShape(): Boolean =
    this == MethodSignatureShape.OBJECT_STRING ||
        this == MethodSignatureShape.STRING_OBJECT ||
        this == MethodSignatureShape.TWO_OBJECT

internal fun MethodSignatureKey.isTwoArgumentUnifiedReturnShape(): Boolean =
    returnKind in setOf(
        MethodReturnKind.STRING,
        MethodReturnKind.FLOAT32,
        MethodReturnKind.FLOAT64,
        MethodReturnKind.BOOLEAN,
        MethodReturnKind.INT32,
        MethodReturnKind.UINT32,
        MethodReturnKind.INT64,
        MethodReturnKind.UINT64,
        MethodReturnKind.GUID,
    ) && shape.isTwoArgumentObjectShape()

internal fun methodParameterCategory(
    type: String,
    supportsObjectType: (String) -> Boolean,
): MethodParameterCategory? {
    return when {
        type == "String" -> MethodParameterCategory.STRING
        type == "Int32" -> MethodParameterCategory.INT32
        type == "Boolean" -> MethodParameterCategory.BOOLEAN
        type == "Int64" -> MethodParameterCategory.INT64
        type == "UInt32" -> MethodParameterCategory.UINT32
        type == "EventRegistrationToken" -> MethodParameterCategory.EVENT_REGISTRATION_TOKEN
        supportsObjectType(type) -> MethodParameterCategory.OBJECT
        else -> null
    }
}

internal fun methodSignatureShape(
    parameterTypes: List<String>,
    supportsObjectType: (String) -> Boolean,
): MethodSignatureShape? {
    val parameterCategories = parameterTypes.map { type ->
        methodParameterCategory(type, supportsObjectType) ?: return null
    }
    return when {
        parameterCategories.isEmpty() -> MethodSignatureShape.EMPTY
        parameterCategories == listOf(MethodParameterCategory.STRING) -> MethodSignatureShape.STRING
        parameterCategories == listOf(MethodParameterCategory.INT32) -> MethodSignatureShape.INT32
        parameterCategories == listOf(MethodParameterCategory.BOOLEAN) -> MethodSignatureShape.BOOLEAN
        parameterCategories == listOf(MethodParameterCategory.INT64) -> MethodSignatureShape.INT64
        parameterCategories == listOf(MethodParameterCategory.UINT32) -> MethodSignatureShape.UINT32
        parameterCategories == listOf(MethodParameterCategory.EVENT_REGISTRATION_TOKEN) -> MethodSignatureShape.EVENT_REGISTRATION_TOKEN
        parameterCategories == listOf(MethodParameterCategory.OBJECT) -> MethodSignatureShape.OBJECT
        parameterCategories == listOf(MethodParameterCategory.OBJECT, MethodParameterCategory.STRING) ->
            MethodSignatureShape.OBJECT_STRING
        parameterCategories == listOf(MethodParameterCategory.STRING, MethodParameterCategory.OBJECT) ->
            MethodSignatureShape.STRING_OBJECT
        parameterCategories == listOf(MethodParameterCategory.OBJECT, MethodParameterCategory.OBJECT) ->
            MethodSignatureShape.TWO_OBJECT
        else -> null
    }
}

internal fun methodSignatureKey(
    returnType: String,
    parameterTypes: List<String>,
    supportsObjectType: (String) -> Boolean,
): MethodSignatureKey? {
    val shape = methodSignatureShape(parameterTypes, supportsObjectType) ?: return null
    val returnKind = when (returnType) {
        "Unit" -> MethodReturnKind.UNIT
        "String" -> MethodReturnKind.STRING
        "Float32" -> MethodReturnKind.FLOAT32
        "Float64" -> MethodReturnKind.FLOAT64
        "Boolean" -> MethodReturnKind.BOOLEAN
        "Int32" -> MethodReturnKind.INT32
        "UInt32" -> MethodReturnKind.UINT32
        "Int64" -> MethodReturnKind.INT64
        "UInt64" -> MethodReturnKind.UINT64
        "Guid" -> MethodReturnKind.GUID
        "EventRegistrationToken" -> MethodReturnKind.EVENT_REGISTRATION_TOKEN
        else -> if (supportsObjectType(returnType)) MethodReturnKind.OBJECT else null
    } ?: return null
    return MethodSignatureKey(returnKind = returnKind, shape = shape)
}
