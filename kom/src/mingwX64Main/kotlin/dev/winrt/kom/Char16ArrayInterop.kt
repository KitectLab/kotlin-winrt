package dev.winrt.kom

actual fun invokeChar16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<CharArray> =
    unsupportedReceiveArray("Char16[]")

actual fun invokeChar16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<CharArray> =
    unsupportedReceiveArrayWithInputs("Char16[]")
