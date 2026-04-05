package dev.winrt.kom

expect fun invokeUInt8ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<ByteArray>

expect fun invokeUInt8ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ByteArray>
