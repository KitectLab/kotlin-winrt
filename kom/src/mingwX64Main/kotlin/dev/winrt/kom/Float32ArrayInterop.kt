package dev.winrt.kom

actual fun invokeFloat32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<FloatArray> {
    return Result.failure(
        UnsupportedOperationException("Native Float32[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeFloat32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<FloatArray> {
    return Result.failure(
        UnsupportedOperationException("Native Float32[] receive-array invocation with inputs is not wired yet"),
    )
}
