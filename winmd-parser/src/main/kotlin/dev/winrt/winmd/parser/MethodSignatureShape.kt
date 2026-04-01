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

internal enum class MethodParameterAbiFamily {
    STRING,
    OBJECT,
    INT32_LIKE,
    INT64_LIKE,
}

internal data class MethodParameterFamilyPair(
    val first: MethodParameterAbiFamily,
    val second: MethodParameterAbiFamily,
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

private val unaryShapes = mapOf(
    MethodParameterCategory.STRING to MethodSignatureShape.STRING,
    MethodParameterCategory.INT32 to MethodSignatureShape.INT32,
    MethodParameterCategory.BOOLEAN to MethodSignatureShape.BOOLEAN,
    MethodParameterCategory.INT64 to MethodSignatureShape.INT64,
    MethodParameterCategory.UINT32 to MethodSignatureShape.UINT32,
    MethodParameterCategory.EVENT_REGISTRATION_TOKEN to MethodSignatureShape.EVENT_REGISTRATION_TOKEN,
    MethodParameterCategory.OBJECT to MethodSignatureShape.OBJECT,
)

private val twoArgumentShapes = mapOf(
    MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.INT32) to MethodSignatureShape.STRING_INT32,
    MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.STRING) to MethodSignatureShape.INT32_STRING,
    MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.UINT32) to MethodSignatureShape.STRING_UINT32,
    MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.STRING) to MethodSignatureShape.UINT32_STRING,
    MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.BOOLEAN) to MethodSignatureShape.STRING_BOOLEAN,
    MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.STRING) to MethodSignatureShape.BOOLEAN_STRING,
    MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.INT64) to MethodSignatureShape.STRING_INT64,
    MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.STRING) to MethodSignatureShape.INT64_STRING,
    MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) to MethodSignatureShape.STRING_EVENT_REGISTRATION_TOKEN,
    MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.STRING) to MethodSignatureShape.EVENT_REGISTRATION_TOKEN_STRING,
    MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.STRING) to MethodSignatureShape.STRING_STRING,
    MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.INT32) to MethodSignatureShape.INT32_INT32,
    MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.UINT32) to MethodSignatureShape.INT32_UINT32,
    MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.BOOLEAN) to MethodSignatureShape.INT32_BOOLEAN,
    MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.INT64) to MethodSignatureShape.INT32_INT64,
    MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) to MethodSignatureShape.INT32_EVENT_REGISTRATION_TOKEN,
    MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.INT32) to MethodSignatureShape.UINT32_INT32,
    MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.UINT32) to MethodSignatureShape.UINT32_UINT32,
    MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.BOOLEAN) to MethodSignatureShape.UINT32_BOOLEAN,
    MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.INT64) to MethodSignatureShape.UINT32_INT64,
    MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) to MethodSignatureShape.UINT32_EVENT_REGISTRATION_TOKEN,
    MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.INT32) to MethodSignatureShape.BOOLEAN_INT32,
    MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.UINT32) to MethodSignatureShape.BOOLEAN_UINT32,
    MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.BOOLEAN) to MethodSignatureShape.BOOLEAN_BOOLEAN,
    MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.INT64) to MethodSignatureShape.BOOLEAN_INT64,
    MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) to MethodSignatureShape.BOOLEAN_EVENT_REGISTRATION_TOKEN,
    MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.INT32) to MethodSignatureShape.INT64_INT32,
    MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.UINT32) to MethodSignatureShape.INT64_UINT32,
    MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.BOOLEAN) to MethodSignatureShape.INT64_BOOLEAN,
    MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.INT64) to MethodSignatureShape.INT64_INT64,
    MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) to MethodSignatureShape.INT64_EVENT_REGISTRATION_TOKEN,
    MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.INT32) to MethodSignatureShape.EVENT_REGISTRATION_TOKEN_INT32,
    MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.UINT32) to MethodSignatureShape.EVENT_REGISTRATION_TOKEN_UINT32,
    MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.BOOLEAN) to MethodSignatureShape.EVENT_REGISTRATION_TOKEN_BOOLEAN,
    MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.INT64) to MethodSignatureShape.EVENT_REGISTRATION_TOKEN_INT64,
    MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) to MethodSignatureShape.EVENT_REGISTRATION_TOKEN_EVENT_REGISTRATION_TOKEN,
    MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.INT32) to MethodSignatureShape.OBJECT_INT32,
    MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.UINT32) to MethodSignatureShape.OBJECT_UINT32,
    MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.BOOLEAN) to MethodSignatureShape.OBJECT_BOOLEAN,
    MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.INT64) to MethodSignatureShape.OBJECT_INT64,
    MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.EVENT_REGISTRATION_TOKEN) to MethodSignatureShape.OBJECT_EVENT_REGISTRATION_TOKEN,
    MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.OBJECT) to MethodSignatureShape.INT32_OBJECT,
    MethodParameterPair(MethodParameterCategory.UINT32, MethodParameterCategory.OBJECT) to MethodSignatureShape.UINT32_OBJECT,
    MethodParameterPair(MethodParameterCategory.BOOLEAN, MethodParameterCategory.OBJECT) to MethodSignatureShape.BOOLEAN_OBJECT,
    MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.OBJECT) to MethodSignatureShape.INT64_OBJECT,
    MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.OBJECT) to MethodSignatureShape.EVENT_REGISTRATION_TOKEN_OBJECT,
    MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.STRING) to MethodSignatureShape.OBJECT_STRING,
    MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.OBJECT) to MethodSignatureShape.STRING_OBJECT,
    MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.OBJECT) to MethodSignatureShape.TWO_OBJECT,
)

private val twoArgumentPairsByShape = twoArgumentShapes.entries.associate { (pair, shape) -> shape to pair }

internal fun MethodSignatureShape.toTwoArgumentParameterPair(): MethodParameterPair? =
    twoArgumentPairsByShape[this]

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

internal fun MethodParameterCategory.toAbiFamily(): MethodParameterAbiFamily =
    when (this) {
        MethodParameterCategory.STRING -> MethodParameterAbiFamily.STRING
        MethodParameterCategory.OBJECT -> MethodParameterAbiFamily.OBJECT
        MethodParameterCategory.INT32,
        MethodParameterCategory.UINT32,
        MethodParameterCategory.BOOLEAN -> MethodParameterAbiFamily.INT32_LIKE
        MethodParameterCategory.INT64,
        MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> MethodParameterAbiFamily.INT64_LIKE
    }

internal fun MethodParameterPair.toAbiFamilyPair(): MethodParameterFamilyPair =
    MethodParameterFamilyPair(first.toAbiFamily(), second.toAbiFamily())

internal fun methodParameterCategories(
    parameterTypes: List<String>,
    supportsObjectType: (String) -> Boolean,
): List<MethodParameterCategory>? =
    parameterTypes.map { type -> methodParameterCategory(type, supportsObjectType) ?: return null }

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
    val parameterCategories = methodParameterCategories(parameterTypes, supportsObjectType) ?: return null
    return when {
        parameterCategories.isEmpty() -> MethodSignatureShape.EMPTY
        parameterCategories.size == 1 -> unaryShapes[parameterCategories.single()]
        parameterCategories.size == 2 -> twoArgumentShapes[
            MethodParameterPair(
                parameterCategories[0],
                parameterCategories[1],
            ),
        ]
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
