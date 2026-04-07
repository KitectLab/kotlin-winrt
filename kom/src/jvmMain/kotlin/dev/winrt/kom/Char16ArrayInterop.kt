package dev.winrt.kom

import java.lang.foreign.ValueLayout

actual fun invokeChar16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<CharArray> =
    invokeChar16ReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeChar16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<CharArray> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeChar16ReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { CharArray(0) },
    ) { dataPointer, size ->
        dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_CHAR.byteSize()).toArray(ValueLayout.JAVA_CHAR)
    }
}
