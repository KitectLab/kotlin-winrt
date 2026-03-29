package dev.winrt.winmd.parser

internal enum class MethodSignatureShape {
    EMPTY,
    STRING,
    INT32,
    INT64,
    UINT32,
    OBJECT,
}

internal enum class MethodReturnKind {
    UNIT,
    STRING,
    FLOAT64,
    BOOLEAN,
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
        parameterTypes == listOf("Int64") -> MethodSignatureShape.INT64
        parameterTypes == listOf("UInt32") -> MethodSignatureShape.UINT32
        parameterTypes.size == 1 && supportsObjectType(parameterTypes.single()) -> MethodSignatureShape.OBJECT
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
        "Float64" -> MethodReturnKind.FLOAT64
        "Boolean" -> MethodReturnKind.BOOLEAN
        else -> if (supportsObjectType(returnType)) MethodReturnKind.OBJECT else null
    } ?: return null
    return MethodSignatureKey(returnKind = returnKind, shape = shape)
}
