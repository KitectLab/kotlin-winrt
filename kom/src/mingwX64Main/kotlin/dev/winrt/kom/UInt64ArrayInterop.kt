package dev.winrt.kom

actual fun invokeUInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray> =
    unsupportedReceiveArray("UInt64[]")

actual fun invokeUInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray> =
    unsupportedReceiveArrayWithInputs("UInt64[]")
