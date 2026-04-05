package dev.winrt.kom

actual fun invokeInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<ShortArray> {
    return Result.failure(
        UnsupportedOperationException("Native Int16[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ShortArray> {
    return Result.failure(
        UnsupportedOperationException("Native Int16[] receive-array invocation with inputs is not wired yet"),
    )
}
