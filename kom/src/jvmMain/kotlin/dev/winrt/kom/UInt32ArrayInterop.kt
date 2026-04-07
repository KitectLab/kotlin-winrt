@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.winrt.kom

import java.lang.foreign.ValueLayout

actual fun invokeUInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<UIntArray> =
    invokeUInt32ReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeUInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<UIntArray> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeUInt32ReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { UIntArray(0) },
    ) { dataPointer, size ->
        val valuesSegment = dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_INT.byteSize())
        UIntArray(size) { index ->
            valuesSegment.getAtIndex(ValueLayout.JAVA_INT, index.toLong()).toUInt()
        }
    }
}
