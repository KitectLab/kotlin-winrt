package dev.winrt.kom

actual fun invokeGuidReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<Array<Guid>> =
    unsupportedReceiveArray("Guid[]")

actual fun invokeGuidReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Array<Guid>> =
    unsupportedReceiveArrayWithInputs("Guid[]")
