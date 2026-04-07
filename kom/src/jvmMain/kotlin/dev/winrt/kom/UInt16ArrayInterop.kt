package dev.winrt.kom

import java.lang.foreign.ValueLayout

actual fun invokeUInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<ShortArray> =
    invokeUInt16ReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeUInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ShortArray> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeUInt16ReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { ShortArray(0) },
    ) { dataPointer, size ->
        dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_SHORT.byteSize()).toArray(ValueLayout.JAVA_SHORT)
    }
}
