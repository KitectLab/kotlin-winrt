package dev.winrt.kom

actual fun invokeTimeSpanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray> {
    return Result.failure(
        UnsupportedOperationException("Native TimeSpan[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeTimeSpanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray> {
    return Result.failure(
        UnsupportedOperationException("Native TimeSpan[] receive-array invocation with inputs is not wired yet"),
    )
}
