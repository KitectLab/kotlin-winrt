package dev.winrt.kom

expect fun invokeInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray>

expect fun invokeInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray>
