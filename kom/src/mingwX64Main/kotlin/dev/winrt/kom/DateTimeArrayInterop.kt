package dev.winrt.kom

actual fun invokeDateTimeReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray> {
    return Result.failure(
        UnsupportedOperationException("Native DateTime[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeDateTimeReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray> {
    return Result.failure(
        UnsupportedOperationException("Native DateTime[] receive-array invocation with inputs is not wired yet"),
    )
}
