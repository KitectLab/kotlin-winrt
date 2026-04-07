package dev.winrt.kom

import java.lang.foreign.ValueLayout

actual fun invokeInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<IntArray> =
    invokeInt32ReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<IntArray> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeInt32ReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { IntArray(0) },
    ) { dataPointer, size ->
        val valuesSegment = dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_INT.byteSize())
        IntArray(size) { index ->
            valuesSegment.getAtIndex(ValueLayout.JAVA_INT, index.toLong())
        }
    }
}
