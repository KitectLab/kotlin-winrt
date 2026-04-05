package dev.winrt.kom

actual fun invokeStructReceiveArrayMethod(
    instance: ComPtr,
    vtableIndex: Int,
    layout: ComStructLayout,
): Result<Array<ComStructValue>> {
    return Result.failure(
        UnsupportedOperationException("Native struct receive-array invocation is not wired yet"),
    )
}

actual fun invokeStructReceiveArrayMethod(
    instance: ComPtr,
    vtableIndex: Int,
    layout: ComStructLayout,
    vararg arguments: Any,
): Result<Array<ComStructValue>> {
    return Result.failure(
        UnsupportedOperationException("Native struct receive-array invocation with inputs is not wired yet"),
    )
}
