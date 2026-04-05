package dev.winrt.kom

expect fun invokeStringReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<Array<String>>

expect fun invokeStringReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Array<String>>
