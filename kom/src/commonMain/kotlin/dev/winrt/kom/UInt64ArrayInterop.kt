package dev.winrt.kom

expect fun invokeUInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray>

expect fun invokeUInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray>
