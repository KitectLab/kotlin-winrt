package dev.winrt.kom

actual fun invokeUInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<ShortArray> =
    unsupportedReceiveArray("UInt16[]")

actual fun invokeUInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ShortArray> =
    unsupportedReceiveArrayWithInputs("UInt16[]")
