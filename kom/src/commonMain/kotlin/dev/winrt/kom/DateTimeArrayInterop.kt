package dev.winrt.kom

expect fun invokeDateTimeReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray>

expect fun invokeDateTimeReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray>
