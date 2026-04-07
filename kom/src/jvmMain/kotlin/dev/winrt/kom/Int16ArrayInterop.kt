package dev.winrt.kom

import java.lang.foreign.ValueLayout

actual fun invokeInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<ShortArray> =
    invokeInt16ReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeInt16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ShortArray> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeInt16ReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { ShortArray(0) },
    ) { dataPointer, size ->
        dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_SHORT.byteSize()).toArray(ValueLayout.JAVA_SHORT)
    }
}
