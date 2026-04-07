package dev.winrt.kom

actual fun invokeStringReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<Array<String>> =
    unsupportedReceiveArray("String[]")

actual fun invokeStringReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Array<String>> =
    unsupportedReceiveArrayWithInputs("String[]")
