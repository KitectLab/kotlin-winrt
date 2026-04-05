package dev.winrt.kom

expect fun invokeObjectReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<Array<ComPtr>>

expect fun invokeObjectReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Array<ComPtr>>
