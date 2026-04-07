@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.winrt.kom

actual fun invokeUInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<UIntArray> =
    unsupportedReceiveArray("UInt32[]")

actual fun invokeUInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<UIntArray> =
    unsupportedReceiveArrayWithInputs("UInt32[]")
