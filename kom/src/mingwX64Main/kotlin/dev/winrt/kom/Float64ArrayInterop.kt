package dev.winrt.kom

actual fun invokeFloat64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<DoubleArray> =
    unsupportedReceiveArray("Float64[]")

actual fun invokeFloat64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<DoubleArray> =
    unsupportedReceiveArrayWithInputs("Float64[]")
