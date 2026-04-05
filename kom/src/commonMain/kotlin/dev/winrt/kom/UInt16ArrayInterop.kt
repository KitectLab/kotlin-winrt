package dev.winrt.kom

expect fun invokeUInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<ShortArray>

expect fun invokeUInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ShortArray>
