package dev.winrt.kom

actual fun invokeInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<IntArray> =
    unsupportedReceiveArray("Int32[]")

actual fun invokeInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<IntArray> =
    unsupportedReceiveArrayWithInputs("Int32[]")
