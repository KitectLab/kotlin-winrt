package dev.winrt.kom

actual fun invokeUInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray> {
    return Result.failure(
        UnsupportedOperationException("Native UInt64[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeUInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray> {
    return Result.failure(
        UnsupportedOperationException("Native UInt64[] receive-array invocation with inputs is not wired yet"),
    )
}
