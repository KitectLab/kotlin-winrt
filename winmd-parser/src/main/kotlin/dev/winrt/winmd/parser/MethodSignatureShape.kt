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
    DATE_TIME,
    TIME_SPAN,
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

internal enum class MethodParameterAbiToken {
    BOOLEAN,
    STRING,
    OBJECT,
    INT32,
    UINT32,
    INT64,
}

internal fun MethodParameterAbiToken.callNamePart(): String =
    when (this) {
        MethodParameterAbiToken.BOOLEAN -> "Boolean"
        MethodParameterAbiToken.STRING -> "String"
        MethodParameterAbiToken.OBJECT -> "Object"
        MethodParameterAbiToken.INT32 -> "Int32"
        MethodParameterAbiToken.UINT32 -> "UInt32"
        MethodParameterAbiToken.INT64 -> "Int64"
    }

internal enum class MethodAbiToken {
    HSTRING,
    UNIT,
    BOOLEAN,
    STRING,
    OBJECT,
    INT32,
    UINT32,
    INT64,
    FLOAT32,
    FLOAT64,
    GUID,
}

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

private fun twoArgumentShape(
    first: MethodParameterCategory,
    second: MethodParameterCategory,
): MethodSignatureShape =
    if (first == MethodParameterCategory.OBJECT && second == MethodParameterCategory.OBJECT) {
        MethodSignatureShape.TWO_OBJECT
    } else {
        MethodSignatureShape.valueOf("${first.name}_${second.name}")
    }

private val twoArgumentShapes = buildMap {
    for (first in MethodParameterCategory.entries) {
        for (second in MethodParameterCategory.entries) {
            put(
                MethodParameterPair(first, second),
                twoArgumentShape(first, second),
            )
        }
    }
}

private val parameterCategoriesByShape = buildMap {
    put(MethodSignatureShape.EMPTY, emptyList())
    unaryShapes.forEach { (category, shape) -> put(shape, listOf(category)) }
    twoArgumentShapes.forEach { (pair, shape) -> put(shape, listOf(pair.first, pair.second)) }
}

internal fun MethodSignatureShape.toParameterCategories(): List<MethodParameterCategory>? =
    parameterCategoriesByShape[this]

internal fun List<MethodParameterCategory>.isSupportedTwoArgumentUnitCategories(): Boolean =
    size == 2 && supportedTwoArgumentUnitCategories(this[0], this[1])

internal fun List<MethodParameterCategory>.isSupportedTwoArgumentUnifiedReturnCategories(): Boolean =
    size == 2 && supportedTwoArgumentUnifiedReturnCategories(this[0], this[1])

internal fun MethodReturnKind.twoArgumentSharedRuleFamily(
    parameterCategories: List<MethodParameterCategory>,
): SharedMethodRuleFamily? = when (this) {
    MethodReturnKind.UNIT ->
        if (parameterCategories.isSupportedTwoArgumentUnitCategories()) SharedMethodRuleFamily.UNIT else null
    MethodReturnKind.STRING ->
        if (parameterCategories.isSupportedTwoArgumentUnifiedReturnCategories()) SharedMethodRuleFamily.STRING else null
    MethodReturnKind.FLOAT32 ->
        if (parameterCategories.isSupportedTwoArgumentUnifiedReturnCategories()) SharedMethodRuleFamily.FLOAT32 else null
    MethodReturnKind.FLOAT64 ->
        if (parameterCategories.isSupportedTwoArgumentUnifiedReturnCategories()) SharedMethodRuleFamily.FLOAT64 else null
    MethodReturnKind.DATE_TIME ->
        if (parameterCategories.isSupportedTwoArgumentUnifiedReturnCategories()) SharedMethodRuleFamily.DATE_TIME else null
    MethodReturnKind.TIME_SPAN ->
        if (parameterCategories.isSupportedTwoArgumentUnifiedReturnCategories()) SharedMethodRuleFamily.TIME_SPAN else null
    MethodReturnKind.BOOLEAN ->
        if (parameterCategories.isSupportedTwoArgumentUnifiedReturnCategories()) SharedMethodRuleFamily.BOOLEAN else null
    MethodReturnKind.INT32,
    MethodReturnKind.UINT32,
    MethodReturnKind.INT64,
    MethodReturnKind.UINT64,
    MethodReturnKind.GUID,
    MethodReturnKind.OBJECT ->
        if (parameterCategories.isSupportedTwoArgumentUnifiedReturnCategories()) SharedMethodRuleFamily.OBJECT else null
    MethodReturnKind.EVENT_REGISTRATION_TOKEN -> null
}

private fun supportedTwoArgumentUnitCategories(
    first: MethodParameterCategory,
    second: MethodParameterCategory,
): Boolean =
    first == MethodParameterCategory.STRING ||
        second == MethodParameterCategory.STRING ||
        first == MethodParameterCategory.OBJECT ||
        second == MethodParameterCategory.OBJECT ||
        (first in int32LikeCategories && second in int32LikeCategories) ||
        (first in int32LikeCategories && second in int64LikeCategories) ||
        (first in int64LikeCategories && second in int32LikeCategories) ||
        (first in int64LikeCategories && second in int64LikeCategories)

private fun supportedTwoArgumentUnifiedReturnCategories(
    first: MethodParameterCategory,
    second: MethodParameterCategory,
): Boolean =
    supportedTwoArgumentUnitCategories(first, second) &&
        !(first == MethodParameterCategory.STRING && second == MethodParameterCategory.STRING)

internal fun MethodParameterCategory.toAbiToken(): MethodParameterAbiToken =
    when (this) {
        MethodParameterCategory.STRING -> MethodParameterAbiToken.STRING
        MethodParameterCategory.OBJECT -> MethodParameterAbiToken.OBJECT
        MethodParameterCategory.INT32 -> MethodParameterAbiToken.INT32
        MethodParameterCategory.UINT32 -> MethodParameterAbiToken.UINT32
        MethodParameterCategory.BOOLEAN -> MethodParameterAbiToken.BOOLEAN
        MethodParameterCategory.INT64,
        MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> MethodParameterAbiToken.INT64
    }

internal fun methodParameterCategories(
    parameterTypes: List<String>,
    supportsObjectType: (String) -> Boolean,
): List<MethodParameterCategory>? =
    parameterTypes.map { type -> methodParameterCategory(type, supportsObjectType) ?: return null }

internal fun MethodSignatureKey.isTwoArgumentUnifiedReturnShape(): Boolean =
    shape.toParameterCategories()?.let(returnKind::twoArgumentSharedRuleFamily) != null &&
        returnKind != MethodReturnKind.UNIT

internal fun methodParameterCategory(
    type: String,
    supportsObjectType: (String) -> Boolean,
): MethodParameterCategory? {
    val canonicalType = canonicalWinRtSpecialType(type)
    return when {
        canonicalType == "String" -> MethodParameterCategory.STRING
        canonicalType == "Int32" -> MethodParameterCategory.INT32
        canonicalType == "Boolean" -> MethodParameterCategory.BOOLEAN
        canonicalType == "Int64" -> MethodParameterCategory.INT64
        canonicalType == "UInt32" -> MethodParameterCategory.UINT32
        canonicalType == "EventRegistrationToken" -> MethodParameterCategory.EVENT_REGISTRATION_TOKEN
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
    val returnKind = when (canonicalWinRtSpecialType(returnType)) {
        "Unit" -> MethodReturnKind.UNIT
        "String" -> MethodReturnKind.STRING
        "Float32" -> MethodReturnKind.FLOAT32
        "Float64" -> MethodReturnKind.FLOAT64
        "DateTime" -> MethodReturnKind.DATE_TIME
        "TimeSpan" -> MethodReturnKind.TIME_SPAN
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

internal fun isEventRegistrationTokenType(type: String): Boolean =
    canonicalWinRtSpecialType(type) == "EventRegistrationToken"

internal fun supportsProjectedObjectTypeName(type: String): Boolean =
    (type == "Object" || (type.contains('.') && canonicalWinRtSpecialType(type) == type)) &&
        !type.contains('`') &&
        !type.contains('<') &&
        !type.endsWith("[]")

internal fun canonicalWinRtSpecialType(type: String): String =
    when (type) {
        "Windows.Foundation.WinRtBoolean" -> "Boolean"
        "Windows.Foundation.Int32" -> "Int32"
        "Windows.Foundation.UInt32" -> "UInt32"
        "Windows.Foundation.Int64" -> "Int64"
        "Windows.Foundation.UInt64" -> "UInt64"
        "Windows.Foundation.Float32" -> "Float32"
        "Windows.Foundation.Float64" -> "Float64"
        "Windows.Foundation.Guid" -> "Guid"
        "Windows.Foundation.DateTime" -> "DateTime"
        "Windows.Foundation.TimeSpan" -> "TimeSpan"
        "Windows.Foundation.EventRegistrationToken" -> "EventRegistrationToken"
        else -> type
    }
