package dev.winrt.kom

expect fun invokeGuidReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<Array<Guid>>

expect fun invokeGuidReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Array<Guid>>
