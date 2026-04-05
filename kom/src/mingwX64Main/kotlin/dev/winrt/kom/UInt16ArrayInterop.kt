package dev.winrt.kom

actual fun invokeUInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<ShortArray> {
    return Result.failure(
        UnsupportedOperationException("Native UInt16[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeUInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ShortArray> {
    return Result.failure(
        UnsupportedOperationException("Native UInt16[] receive-array invocation with inputs is not wired yet"),
    )
}
