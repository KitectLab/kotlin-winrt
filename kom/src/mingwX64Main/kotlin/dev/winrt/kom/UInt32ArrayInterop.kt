@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.winrt.kom

actual fun invokeUInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<UIntArray> {
    return Result.failure(
        UnsupportedOperationException("Native UInt32[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeUInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<UIntArray> {
    return Result.failure(
        UnsupportedOperationException("Native UInt32[] receive-array invocation with inputs is not wired yet"),
    )
}
