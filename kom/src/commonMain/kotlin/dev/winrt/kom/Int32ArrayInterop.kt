package dev.winrt.kom

expect fun invokeInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<IntArray>

expect fun invokeInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<IntArray>
