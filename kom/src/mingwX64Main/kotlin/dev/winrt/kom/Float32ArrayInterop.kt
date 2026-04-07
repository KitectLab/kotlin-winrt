package dev.winrt.kom

actual fun invokeFloat32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<FloatArray> =
    unsupportedReceiveArray("Float32[]")

actual fun invokeFloat32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<FloatArray> =
    unsupportedReceiveArrayWithInputs("Float32[]")
