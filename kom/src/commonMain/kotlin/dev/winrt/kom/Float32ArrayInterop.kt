package dev.winrt.kom

expect fun invokeFloat32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<FloatArray>

expect fun invokeFloat32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<FloatArray>
