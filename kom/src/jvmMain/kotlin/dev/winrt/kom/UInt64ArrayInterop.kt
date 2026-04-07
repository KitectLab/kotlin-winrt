package dev.winrt.kom

import java.lang.foreign.ValueLayout

actual fun invokeUInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray> =
    invokeUInt64ReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeUInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeUInt64ReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { LongArray(0) },
    ) { dataPointer, size ->
        val valuesSegment = dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_LONG.byteSize())
        LongArray(size) { index ->
            valuesSegment.getAtIndex(ValueLayout.JAVA_LONG, index.toLong())
        }
    }
}
