package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.stripValueTypeNameMarker

internal data class MethodSignatureShape(
    private val parameterCategories: List<MethodParameterCategory>,
) {
    init {
        require(parameterCategories.size <= 2) { "Unsupported method signature arity: ${parameterCategories.size}" }
    }

    fun toParameterCategories(): List<MethodParameterCategory> = parameterCategories

    companion object {
        val EMPTY = MethodSignatureShape(emptyList())

        fun of(parameterCategories: List<MethodParameterCategory>): MethodSignatureShape? =
            if (parameterCategories.size <= 2) {
                if (parameterCategories.isEmpty()) {
                    EMPTY
                } else {
                    MethodSignatureShape(parameterCategories)
                }
            } else {
                null
            }
    }
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

internal data class MethodParameterAbiDescriptor(
    val methodNamePart: String,
    val argumentPlaceholder: String = "%L",
)

internal data class MethodReturnAbiDescriptor(
    val methodNamePart: String,
)

internal object MethodParameterAbiToken {
    val BOOLEAN = MethodParameterAbiDescriptor("Boolean")
    val STRING = MethodParameterAbiDescriptor("String", argumentPlaceholder = "%N")
    val OBJECT = MethodParameterAbiDescriptor("Object")
    val INT32 = MethodParameterAbiDescriptor("Int32")
    val UINT32 = MethodParameterAbiDescriptor("UInt32")
    val INT64 = MethodParameterAbiDescriptor("Int64")
}

internal object MethodAbiToken {
    val HSTRING = MethodReturnAbiDescriptor("HString")
    val UNIT = MethodReturnAbiDescriptor("Unit")
    val BOOLEAN = MethodReturnAbiDescriptor("Boolean")
    val STRING = MethodReturnAbiDescriptor("String")
    val OBJECT = MethodReturnAbiDescriptor("Object")
    val INT32 = MethodReturnAbiDescriptor("Int32")
    val UINT32 = MethodReturnAbiDescriptor("UInt32")
    val INT64 = MethodReturnAbiDescriptor("Int64")
    val UINT64 = MethodReturnAbiDescriptor("UInt64")
    val FLOAT32 = MethodReturnAbiDescriptor("Float32")
    val FLOAT64 = MethodReturnAbiDescriptor("Float64")
    val GUID = MethodReturnAbiDescriptor("Guid")
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

internal fun methodSignatureShapeOf(vararg parameterCategories: MethodParameterCategory): MethodSignatureShape =
    MethodSignatureShape.of(parameterCategories.toList())
        ?: error("Unsupported method signature arity: ${parameterCategories.size}")

private fun methodSignatureShapeOf(parameterCategories: List<MethodParameterCategory>): MethodSignatureShape? =
    MethodSignatureShape.of(parameterCategories)

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

internal fun MethodParameterCategory.toAbiDescriptor(): MethodParameterAbiDescriptor =
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

internal fun methodParameterCategory(
    type: String,
    supportsObjectType: (String) -> Boolean,
): MethodParameterCategory? {
    val canonicalType = canonicalWinRtSpecialType(type)
    return when {
        canonicalType == "String" -> MethodParameterCategory.STRING
        canonicalType == "HResult" -> MethodParameterCategory.INT32
        canonicalType == "Int32" -> MethodParameterCategory.INT32
        canonicalType == "Boolean" -> MethodParameterCategory.BOOLEAN
        canonicalType == "DateTime" -> MethodParameterCategory.INT64
        canonicalType == "TimeSpan" -> MethodParameterCategory.INT64
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
    return methodSignatureShapeOf(parameterCategories)
}

internal fun methodSignatureKey(
    returnType: String,
    parameterTypes: List<String>,
    supportsParameterObjectType: (String) -> Boolean,
    supportsReturnObjectType: (String) -> Boolean = supportsParameterObjectType,
): MethodSignatureKey? {
    val shape = methodSignatureShape(parameterTypes, supportsParameterObjectType) ?: return null
    val returnKind = when (canonicalWinRtSpecialType(returnType)) {
        "Unit" -> MethodReturnKind.UNIT
        "String" -> MethodReturnKind.STRING
        "Float32" -> MethodReturnKind.FLOAT32
        "Float64" -> MethodReturnKind.FLOAT64
        "DateTime" -> MethodReturnKind.DATE_TIME
        "TimeSpan" -> MethodReturnKind.TIME_SPAN
        "Boolean" -> MethodReturnKind.BOOLEAN
        "HResult" -> MethodReturnKind.INT32
        "Int32" -> MethodReturnKind.INT32
        "UInt32" -> MethodReturnKind.UINT32
        "Int64" -> MethodReturnKind.INT64
        "UInt64" -> MethodReturnKind.UINT64
        "Guid" -> MethodReturnKind.GUID
        "EventRegistrationToken" -> MethodReturnKind.EVENT_REGISTRATION_TOKEN
        else -> if (supportsReturnObjectType(returnType)) MethodReturnKind.OBJECT else null
    } ?: return null
    return MethodSignatureKey(returnKind = returnKind, shape = shape)
}

internal fun isEventRegistrationTokenType(type: String): Boolean =
    canonicalWinRtSpecialType(type) == "EventRegistrationToken"

internal fun isHResultType(type: String): Boolean =
    canonicalWinRtSpecialType(type) == "HResult"

internal fun supportsProjectedObjectTypeName(type: String): Boolean =
    (canonicalWinRtSpecialType(type) == "Object" || (type.contains('.') && canonicalWinRtSpecialType(type) == type)) &&
        !type.contains('`') &&
        !type.contains('<') &&
        !type.endsWith("[]")

internal fun canonicalWinRtSpecialType(type: String): String =
    when (stripValueTypeNameMarker(type)) {
        "System.String" -> "String"
        "System.Object" -> "Object"
        "System.Boolean" -> "Boolean"
        "System.Byte" -> "UInt8"
        "System.Int16" -> "Int16"
        "System.UInt16" -> "UInt16"
        "System.Char" -> "Char16"
        "System.Int32" -> "Int32"
        "System.UInt32" -> "UInt32"
        "System.Int64" -> "Int64"
        "System.UInt64" -> "UInt64"
        "System.Single" -> "Float32"
        "System.Double" -> "Float64"
        "Windows.Foundation.UInt8" -> "UInt8"
        "Windows.Foundation.Int16" -> "Int16"
        "Windows.Foundation.UInt16" -> "UInt16"
        "Windows.Foundation.Char16" -> "Char16"
        "System.Guid" -> "Guid"
        "Windows.Foundation.WinRtBoolean" -> "Boolean"
        "HResult" -> "HResult"
        "Windows.Foundation.HResult" -> "HResult"
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
        else -> stripValueTypeNameMarker(type)
    }
