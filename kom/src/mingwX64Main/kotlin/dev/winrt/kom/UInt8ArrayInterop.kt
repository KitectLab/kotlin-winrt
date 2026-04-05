package dev.winrt.kom

actual fun invokeUInt8ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<ByteArray> {
    return Result.failure(
        UnsupportedOperationException("Native UInt8[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeUInt8ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ByteArray> {
    return Result.failure(
        UnsupportedOperationException("Native UInt8[] receive-array invocation with inputs is not wired yet"),
    )
}
