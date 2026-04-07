package dev.winrt.kom

import java.lang.foreign.ValueLayout

actual fun invokeUInt8ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<ByteArray> =
    invokeUInt8ReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeUInt8ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ByteArray> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeUInt8ReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { ByteArray(0) },
    ) { dataPointer, size ->
        dataPointer.reinterpret(size.toLong()).toArray(ValueLayout.JAVA_BYTE)
    }
}
