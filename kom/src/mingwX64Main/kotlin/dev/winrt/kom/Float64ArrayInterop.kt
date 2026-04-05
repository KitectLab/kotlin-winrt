package dev.winrt.kom

actual fun invokeFloat64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<DoubleArray> {
    return Result.failure(
        UnsupportedOperationException("Native Float64[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeFloat64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<DoubleArray> {
    return Result.failure(
        UnsupportedOperationException("Native Float64[] receive-array invocation with inputs is not wired yet"),
    )
}
