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
    STRING_INT32,
    INT32_STRING,
    STRING_UINT32,
    UINT32_STRING,
    STRING_BOOLEAN,
    BOOLEAN_STRING,
    STRING_INT64,
    INT64_STRING,
    STRING_EVENT_REGISTRATION_TOKEN,
    EVENT_REGISTRATION_TOKEN_STRING,
    STRING_STRING,
    INT32_INT32,
    INT32_UINT32,
    INT32_BOOLEAN,
    INT32_INT64,
    INT32_EVENT_REGISTRATION_TOKEN,
    UINT32_INT32,
    UINT32_UINT32,
    UINT32_BOOLEAN,
    UINT32_INT64,
    UINT32_EVENT_REGISTRATION_TOKEN,
    BOOLEAN_INT32,
    BOOLEAN_UINT32,
    BOOLEAN_BOOLEAN,
    BOOLEAN_INT64,
    BOOLEAN_EVENT_REGISTRATION_TOKEN,
    INT64_INT32,
    INT64_UINT32,
    INT64_BOOLEAN,
    INT64_INT64,
    INT64_EVENT_REGISTRATION_TOKEN,
    EVENT_REGISTRATION_TOKEN_INT32,
    EVENT_REGISTRATION_TOKEN_UINT32,
    EVENT_REGISTRATION_TOKEN_BOOLEAN,
    EVENT_REGISTRATION_TOKEN_INT64,
    EVENT_REGISTRATION_TOKEN_EVENT_REGISTRATION_TOKEN,
    OBJECT_INT32,
    OBJECT_UINT32,
    OBJECT_BOOLEAN,
    OBJECT_INT64,
    OBJECT_EVENT_REGISTRATION_TOKEN,
    INT32_OBJECT,
    UINT32_OBJECT,
    BOOLEAN_OBJECT,
    INT64_OBJECT,
    EVENT_REGISTRATION_TOKEN_OBJECT,
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

internal data class MethodParameterPair(
    val first: MethodParameterCategory,
    val second: MethodParameterCategory,
)

private val int32LikeCategories = setOf(
    MethodParameterCategory.INT32,
    MethodParameterCategory.UINT32,
    MethodParameterCategory.BOOLEAN,
)

private val int64LikeCategories = setOf(
    MethodParameterCategory.INT64,
    MethodParameterCategory.EVENT_REGISTRATION_TOKEN,
)

internal fun MethodSignatureShape.isTwoArgumentObjectShape(): Boolean =
    toTwoArgumentParameterPair() != null

internal fun MethodSignatureShape.toTwoArgumentParameterPair(): MethodParameterPair? =
    when (this) {
        MethodSignatureShape.STRING_INT32 -> MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.INT32)
        MethodSignatureShape.INT32_STRING -> MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.STRING)
        MethodSignatureShape.STRING_UINT32 -> MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.UINT32)
        MethodSignatureShape.UINT32_STRING -> MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.STRING)
        MethodSignatureShape.STRING_BOOLEAN -> MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.BOOLEAN)
        MethodSignatureShape.BOOLEAN_STRING -> MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.STRING)
        MethodSignatureShape.STRING_INT64 -> MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.INT64)
        MethodSignatureShape.INT64_STRING -> MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.STRING)
        MethodSignatureShape.STRING_EVENT_REGISTRATION_TOKEN -> MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.EVENT_REGISTRATION_TOKEN)
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_STRING -> MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.STRING)
        MethodSignatureShape.STRING_STRING -> MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.STRING)
        MethodSignatureShape.INT32_INT32 -> MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.INT32)
        MethodSignatureShape.INT32_UINT32 -> MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.UINT32)
        MethodSignatureShape.INT32_BOOLEAN -> MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.BOOLEAN)
        MethodSignatureShape.INT32_INT64 -> MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.INT64)
        MethodSignatureShape.INT32_EVENT_REGISTRATION_TOKEN -> MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.EVENT_REGISTRATION_TOKEN)
        MethodSignatureShape.UINT32_INT32 -> MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.INT32)
        MethodSignatureShape.UINT32_UINT32 -> MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.UINT32)
        MethodSignatureShape.UINT32_BOOLEAN -> MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.BOOLEAN)
        MethodSignatureShape.UINT32_INT64 -> MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.INT64)
        MethodSignatureShape.UINT32_EVENT_REGISTRATION_TOKEN -> MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.EVENT_REGISTRATION_TOKEN)
        MethodSignatureShape.BOOLEAN_INT32 -> MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.INT32)
        MethodSignatureShape.BOOLEAN_UINT32 -> MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.UINT32)
        MethodSignatureShape.BOOLEAN_BOOLEAN -> MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.BOOLEAN)
        MethodSignatureShape.BOOLEAN_INT64 -> MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.INT64)
        MethodSignatureShape.BOOLEAN_EVENT_REGISTRATION_TOKEN -> MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.EVENT_REGISTRATION_TOKEN)
        MethodSignatureShape.INT64_INT32 -> MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.INT32)
        MethodSignatureShape.INT64_UINT32 -> MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.UINT32)
        MethodSignatureShape.INT64_BOOLEAN -> MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.BOOLEAN)
        MethodSignatureShape.INT64_INT64 -> MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.INT64)
        MethodSignatureShape.INT64_EVENT_REGISTRATION_TOKEN -> MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.EVENT_REGISTRATION_TOKEN)
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_INT32 -> MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.INT32)
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_UINT32 -> MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.UINT32)
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_BOOLEAN -> MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.BOOLEAN)
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_INT64 -> MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.INT64)
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_EVENT_REGISTRATION_TOKEN -> MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.EVENT_REGISTRATION_TOKEN)
        MethodSignatureShape.OBJECT_INT32 -> MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.INT32)
        MethodSignatureShape.OBJECT_UINT32 -> MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.UINT32)
        MethodSignatureShape.OBJECT_BOOLEAN -> MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.BOOLEAN)
        MethodSignatureShape.OBJECT_INT64 -> MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.INT64)
        MethodSignatureShape.OBJECT_EVENT_REGISTRATION_TOKEN -> MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.EVENT_REGISTRATION_TOKEN)
        MethodSignatureShape.INT32_OBJECT -> MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.OBJECT)
        MethodSignatureShape.UINT32_OBJECT -> MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.OBJECT)
        MethodSignatureShape.BOOLEAN_OBJECT -> MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.OBJECT)
        MethodSignatureShape.INT64_OBJECT -> MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.OBJECT)
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_OBJECT -> MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.OBJECT)
        MethodSignatureShape.OBJECT_STRING -> MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.STRING)
        MethodSignatureShape.STRING_OBJECT -> MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.OBJECT)
        MethodSignatureShape.TWO_OBJECT -> MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.OBJECT)
        else -> null
    }

internal fun MethodParameterPair.isSupportedTwoArgumentUnitPair(): Boolean =
    first == MethodParameterCategory.STRING ||
        second == MethodParameterCategory.STRING ||
        first == MethodParameterCategory.OBJECT ||
        second == MethodParameterCategory.OBJECT ||
        (first in int32LikeCategories && second in int32LikeCategories) ||
        (first in int32LikeCategories && second in int64LikeCategories) ||
        (first in int64LikeCategories && second in int32LikeCategories) ||
        (first in int64LikeCategories && second in int64LikeCategories)

internal fun MethodParameterPair.isSupportedTwoArgumentUnifiedReturnPair(): Boolean =
    isSupportedTwoArgumentUnitPair() && this != MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.STRING)

internal fun MethodSignatureKey.isTwoArgumentUnifiedReturnShape(): Boolean =
    returnKind in setOf(
        MethodReturnKind.OBJECT,
        MethodReturnKind.STRING,
        MethodReturnKind.FLOAT32,
        MethodReturnKind.FLOAT64,
        MethodReturnKind.BOOLEAN,
        MethodReturnKind.INT32,
        MethodReturnKind.UINT32,
        MethodReturnKind.INT64,
        MethodReturnKind.UINT64,
        MethodReturnKind.GUID,
    ) && shape.toTwoArgumentParameterPair()?.isSupportedTwoArgumentUnifiedReturnPair() == true

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
        parameterCategories == listOf(MethodParameterCategory.STRING, MethodParameterCategory.INT32) -> MethodSignatureShape.STRING_INT32
        parameterCategories == listOf(MethodParameterCategory.INT32, MethodParameterCategory.STRING) -> MethodSignatureShape.INT32_STRING
        parameterCategories == listOf(MethodParameterCategory.STRING, MethodParameterCategory.UINT32) -> MethodSignatureShape.STRING_UINT32
        parameterCategories == listOf(MethodParameterCategory.UINT32, MethodParameterCategory.STRING) -> MethodSignatureShape.UINT32_STRING
        parameterCategories == listOf(MethodParameterCategory.STRING, MethodParameterCategory.BOOLEAN) -> MethodSignatureShape.STRING_BOOLEAN
        parameterCategories == listOf(MethodParameterCategory.BOOLEAN, MethodParameterCategory.STRING) -> MethodSignatureShape.BOOLEAN_STRING
        parameterCategories == listOf(MethodParameterCategory.STRING, MethodParameterCategory.INT64) -> MethodSignatureShape.STRING_INT64
        parameterCategories == listOf(MethodParameterCategory.INT64, MethodParameterCategory.STRING) -> MethodSignatureShape.INT64_STRING
        parameterCategories == listOf(MethodParameterCategory.STRING, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) -> MethodSignatureShape.STRING_EVENT_REGISTRATION_TOKEN
        parameterCategories == listOf(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.STRING) -> MethodSignatureShape.EVENT_REGISTRATION_TOKEN_STRING
        parameterCategories == listOf(MethodParameterCategory.STRING, MethodParameterCategory.STRING) -> MethodSignatureShape.STRING_STRING
        parameterCategories == listOf(MethodParameterCategory.INT32, MethodParameterCategory.INT32) -> MethodSignatureShape.INT32_INT32
        parameterCategories == listOf(MethodParameterCategory.INT32, MethodParameterCategory.UINT32) -> MethodSignatureShape.INT32_UINT32
        parameterCategories == listOf(MethodParameterCategory.INT32, MethodParameterCategory.BOOLEAN) -> MethodSignatureShape.INT32_BOOLEAN
        parameterCategories == listOf(MethodParameterCategory.INT32, MethodParameterCategory.INT64) -> MethodSignatureShape.INT32_INT64
        parameterCategories == listOf(MethodParameterCategory.INT32, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) -> MethodSignatureShape.INT32_EVENT_REGISTRATION_TOKEN
        parameterCategories == listOf(MethodParameterCategory.UINT32, MethodParameterCategory.INT32) -> MethodSignatureShape.UINT32_INT32
        parameterCategories == listOf(MethodParameterCategory.UINT32, MethodParameterCategory.UINT32) -> MethodSignatureShape.UINT32_UINT32
        parameterCategories == listOf(MethodParameterCategory.UINT32, MethodParameterCategory.BOOLEAN) -> MethodSignatureShape.UINT32_BOOLEAN
        parameterCategories == listOf(MethodParameterCategory.UINT32, MethodParameterCategory.INT64) -> MethodSignatureShape.UINT32_INT64
        parameterCategories == listOf(MethodParameterCategory.UINT32, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) -> MethodSignatureShape.UINT32_EVENT_REGISTRATION_TOKEN
        parameterCategories == listOf(MethodParameterCategory.BOOLEAN, MethodParameterCategory.INT32) -> MethodSignatureShape.BOOLEAN_INT32
        parameterCategories == listOf(MethodParameterCategory.BOOLEAN, MethodParameterCategory.UINT32) -> MethodSignatureShape.BOOLEAN_UINT32
        parameterCategories == listOf(MethodParameterCategory.BOOLEAN, MethodParameterCategory.BOOLEAN) -> MethodSignatureShape.BOOLEAN_BOOLEAN
        parameterCategories == listOf(MethodParameterCategory.BOOLEAN, MethodParameterCategory.INT64) -> MethodSignatureShape.BOOLEAN_INT64
        parameterCategories == listOf(MethodParameterCategory.BOOLEAN, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) -> MethodSignatureShape.BOOLEAN_EVENT_REGISTRATION_TOKEN
        parameterCategories == listOf(MethodParameterCategory.INT64, MethodParameterCategory.INT32) -> MethodSignatureShape.INT64_INT32
        parameterCategories == listOf(MethodParameterCategory.INT64, MethodParameterCategory.UINT32) -> MethodSignatureShape.INT64_UINT32
        parameterCategories == listOf(MethodParameterCategory.INT64, MethodParameterCategory.BOOLEAN) -> MethodSignatureShape.INT64_BOOLEAN
        parameterCategories == listOf(MethodParameterCategory.INT64, MethodParameterCategory.INT64) -> MethodSignatureShape.INT64_INT64
        parameterCategories == listOf(MethodParameterCategory.INT64, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) -> MethodSignatureShape.INT64_EVENT_REGISTRATION_TOKEN
        parameterCategories == listOf(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.INT32) -> MethodSignatureShape.EVENT_REGISTRATION_TOKEN_INT32
        parameterCategories == listOf(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.UINT32) -> MethodSignatureShape.EVENT_REGISTRATION_TOKEN_UINT32
        parameterCategories == listOf(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.BOOLEAN) -> MethodSignatureShape.EVENT_REGISTRATION_TOKEN_BOOLEAN
        parameterCategories == listOf(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.INT64) -> MethodSignatureShape.EVENT_REGISTRATION_TOKEN_INT64
        parameterCategories == listOf(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) -> MethodSignatureShape.EVENT_REGISTRATION_TOKEN_EVENT_REGISTRATION_TOKEN
        parameterCategories == listOf(MethodParameterCategory.OBJECT, MethodParameterCategory.INT32) -> MethodSignatureShape.OBJECT_INT32
        parameterCategories == listOf(MethodParameterCategory.OBJECT, MethodParameterCategory.UINT32) -> MethodSignatureShape.OBJECT_UINT32
        parameterCategories == listOf(MethodParameterCategory.OBJECT, MethodParameterCategory.BOOLEAN) -> MethodSignatureShape.OBJECT_BOOLEAN
        parameterCategories == listOf(MethodParameterCategory.OBJECT, MethodParameterCategory.INT64) -> MethodSignatureShape.OBJECT_INT64
        parameterCategories == listOf(MethodParameterCategory.OBJECT, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) -> MethodSignatureShape.OBJECT_EVENT_REGISTRATION_TOKEN
        parameterCategories == listOf(MethodParameterCategory.INT32, MethodParameterCategory.OBJECT) -> MethodSignatureShape.INT32_OBJECT
        parameterCategories == listOf(MethodParameterCategory.UINT32, MethodParameterCategory.OBJECT) -> MethodSignatureShape.UINT32_OBJECT
        parameterCategories == listOf(MethodParameterCategory.BOOLEAN, MethodParameterCategory.OBJECT) -> MethodSignatureShape.BOOLEAN_OBJECT
        parameterCategories == listOf(MethodParameterCategory.INT64, MethodParameterCategory.OBJECT) -> MethodSignatureShape.INT64_OBJECT
        parameterCategories == listOf(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.OBJECT) -> MethodSignatureShape.EVENT_REGISTRATION_TOKEN_OBJECT
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
