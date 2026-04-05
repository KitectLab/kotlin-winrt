package dev.winrt.kom

expect fun invokeTimeSpanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray>

expect fun invokeTimeSpanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray>
