package dev.winrt.kom

expect fun invokeChar16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<CharArray>

expect fun invokeChar16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<CharArray>
