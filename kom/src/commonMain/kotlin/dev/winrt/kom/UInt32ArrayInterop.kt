@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.winrt.kom

expect fun invokeUInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<UIntArray>

expect fun invokeUInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<UIntArray>
