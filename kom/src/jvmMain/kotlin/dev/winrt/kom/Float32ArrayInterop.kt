package dev.winrt.kom

import java.lang.foreign.ValueLayout

actual fun invokeFloat32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<FloatArray> =
    invokeFloat32ReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeFloat32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<FloatArray> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeFloat32ReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { FloatArray(0) },
    ) { dataPointer, size ->
        val valuesSegment = dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_FLOAT.byteSize())
        FloatArray(size) { index ->
            valuesSegment.getAtIndex(ValueLayout.JAVA_FLOAT, index.toLong())
        }
    }
}
