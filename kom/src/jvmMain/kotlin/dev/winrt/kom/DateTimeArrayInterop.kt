package dev.winrt.kom

import java.lang.foreign.ValueLayout

actual fun invokeDateTimeReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray> =
    invokeDateTimeReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeDateTimeReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeDateTimeReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { LongArray(0) },
    ) { dataPointer, size ->
        dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_LONG.byteSize()).toArray(ValueLayout.JAVA_LONG)
    }
}
