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

internal fun methodSignatureShape(
    parameterTypes: List<String>,
    supportsObjectType: (String) -> Boolean,
): MethodSignatureShape? {
    return when {
        parameterTypes.isEmpty() -> MethodSignatureShape.EMPTY
        parameterTypes == listOf("String") -> MethodSignatureShape.STRING
        parameterTypes == listOf("Int32") -> MethodSignatureShape.INT32
        parameterTypes == listOf("Boolean") -> MethodSignatureShape.BOOLEAN
        parameterTypes == listOf("Int64") -> MethodSignatureShape.INT64
        parameterTypes == listOf("UInt32") -> MethodSignatureShape.UINT32
        parameterTypes == listOf("EventRegistrationToken") -> MethodSignatureShape.EVENT_REGISTRATION_TOKEN
        parameterTypes.size == 1 && supportsObjectType(parameterTypes.single()) -> MethodSignatureShape.OBJECT
        parameterTypes.size == 2 && supportsObjectType(parameterTypes[0]) && parameterTypes[1] == "String" ->
            MethodSignatureShape.OBJECT_STRING
        parameterTypes.size == 2 && parameterTypes[0] == "String" && supportsObjectType(parameterTypes[1]) ->
            MethodSignatureShape.STRING_OBJECT
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
