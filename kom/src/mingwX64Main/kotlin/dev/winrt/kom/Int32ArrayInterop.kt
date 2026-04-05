package dev.winrt.kom

actual fun invokeInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<IntArray> {
    return Result.failure(
        UnsupportedOperationException("Native Int32[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<IntArray> {
    return Result.failure(
        UnsupportedOperationException("Native Int32[] receive-array invocation with inputs is not wired yet"),
    )
}
