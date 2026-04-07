package dev.winrt.kom

import java.lang.foreign.ValueLayout

actual fun invokeBooleanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<BooleanArray> =
    invokeBooleanReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeBooleanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<BooleanArray> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeBooleanReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { BooleanArray(0) },
    ) { dataPointer, size ->
        val valuesSegment = dataPointer.reinterpret(size.toLong())
        BooleanArray(size) { index ->
            valuesSegment.getAtIndex(ValueLayout.JAVA_BYTE, index.toLong()).toInt() != 0
        }
    }
}
