package dev.winrt.kom

actual fun invokeUInt8ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<ByteArray> =
    unsupportedReceiveArray("UInt8[]")

actual fun invokeUInt8ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ByteArray> =
    unsupportedReceiveArrayWithInputs("UInt8[]")
