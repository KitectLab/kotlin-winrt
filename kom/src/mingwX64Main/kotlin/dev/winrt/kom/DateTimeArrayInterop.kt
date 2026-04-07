package dev.winrt.kom

actual fun invokeDateTimeReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray> =
    unsupportedReceiveArray("DateTime[]")

actual fun invokeDateTimeReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray> =
    unsupportedReceiveArrayWithInputs("DateTime[]")
