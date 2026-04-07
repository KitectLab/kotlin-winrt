package dev.winrt.kom

actual fun invokeObjectReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<Array<ComPtr>> =
    unsupportedReceiveArray("Object[]")

actual fun invokeObjectReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Array<ComPtr>> =
    unsupportedReceiveArrayWithInputs("Object[]")
