package dev.winrt.kom

actual fun invokeTimeSpanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray> =
    unsupportedReceiveArray("TimeSpan[]")

actual fun invokeTimeSpanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray> =
    unsupportedReceiveArrayWithInputs("TimeSpan[]")
