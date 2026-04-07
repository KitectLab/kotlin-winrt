package dev.winrt.kom

import java.lang.foreign.ValueLayout

actual fun invokeTimeSpanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray> =
    invokeTimeSpanReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeTimeSpanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeTimeSpanReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { LongArray(0) },
    ) { dataPointer, size ->
        dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_LONG.byteSize()).toArray(ValueLayout.JAVA_LONG)
    }
}
