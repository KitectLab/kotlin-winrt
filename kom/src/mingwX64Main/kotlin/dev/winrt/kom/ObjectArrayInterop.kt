package dev.winrt.kom

actual fun invokeObjectReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<Array<ComPtr>> {
    return Result.failure(
        UnsupportedOperationException("Native Object[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeObjectReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Array<ComPtr>> {
    return Result.failure(
        UnsupportedOperationException("Native Object[] receive-array invocation with inputs is not wired yet"),
    )
}
