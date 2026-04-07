package dev.winrt.kom

import java.lang.foreign.ValueLayout

actual fun invokeFloat64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<DoubleArray> =
    invokeFloat64ReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeFloat64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<DoubleArray> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeFloat64ReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { DoubleArray(0) },
    ) { dataPointer, size ->
        val valuesSegment = dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_DOUBLE.byteSize())
        DoubleArray(size) { index ->
            valuesSegment.getAtIndex(ValueLayout.JAVA_DOUBLE, index.toLong())
        }
    }
}
