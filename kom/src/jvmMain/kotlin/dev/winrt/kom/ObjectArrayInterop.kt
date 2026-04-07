package dev.winrt.kom

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

actual fun invokeObjectReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<Array<ComPtr>> =
    invokeObjectReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeObjectReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Array<ComPtr>> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeObjectReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { emptyArray() },
    ) { dataPointer, size ->
        val valuesSegment = dataPointer.reinterpret(size.toLong() * ValueLayout.ADDRESS.byteSize())
        Array(size) { index ->
            Jdk22Foreign.addressResult(valuesSegment.getAtIndex(ValueLayout.ADDRESS, index.toLong()))
        }
    }
}
