package dev.winrt.kom

actual fun invokeInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray> {
    return Result.failure(
        UnsupportedOperationException("Native Int64[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray> {
    return Result.failure(
        UnsupportedOperationException("Native Int64[] receive-array invocation with inputs is not wired yet"),
    )
}
