package dev.winrt.winmd.parser

internal enum class MethodSignatureShape {
    EMPTY,
    STRING,
    INT32,
    INT64,
    UINT32,
    OBJECT,
}

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
