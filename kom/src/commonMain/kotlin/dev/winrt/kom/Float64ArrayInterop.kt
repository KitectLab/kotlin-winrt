package dev.winrt.kom

expect fun invokeFloat64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<DoubleArray>

expect fun invokeFloat64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<DoubleArray>
