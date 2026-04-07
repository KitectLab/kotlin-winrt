package dev.winrt.kom

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

actual fun invokeStringReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<Array<String>> =
    invokeStringReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeStringReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Array<String>> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeStringReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { emptyArray() },
    ) { dataPointer, size ->
        val valuesSegment = dataPointer.reinterpret(size.toLong() * ValueLayout.ADDRESS.byteSize())
        Array(size) { index ->
            val hString = HString(valuesSegment.getAtIndex(ValueLayout.ADDRESS, index.toLong()).address())
            try {
                JvmWinRtRuntime.toKotlinString(hString)
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
            }
        }
    }
}
