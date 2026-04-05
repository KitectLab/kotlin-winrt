package dev.winrt.kom

expect fun invokeInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<ShortArray>

expect fun invokeInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ShortArray>
