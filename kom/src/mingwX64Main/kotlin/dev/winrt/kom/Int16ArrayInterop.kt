package dev.winrt.kom

actual fun invokeInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<ShortArray> =
    unsupportedReceiveArray("Int16[]")

actual fun invokeInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ShortArray> =
    unsupportedReceiveArrayWithInputs("Int16[]")
