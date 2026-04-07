package dev.winrt.kom

actual fun invokeInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray> =
    unsupportedReceiveArray("Int64[]")

actual fun invokeInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray> =
    unsupportedReceiveArrayWithInputs("Int64[]")
