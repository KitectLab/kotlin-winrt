package dev.winrt.kom

actual fun invokeBooleanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<BooleanArray> =
    unsupportedReceiveArray("Boolean[]")

actual fun invokeBooleanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<BooleanArray> =
    unsupportedReceiveArrayWithInputs("Boolean[]")
