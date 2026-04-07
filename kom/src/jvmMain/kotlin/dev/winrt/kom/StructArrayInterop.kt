package dev.winrt.kom

import java.lang.foreign.MemorySegment

actual fun invokeStructReceiveArrayMethod(
    instance: ComPtr,
    vtableIndex: Int,
    layout: ComStructLayout,
): Result<Array<ComStructValue>> = invokeStructReceiveArrayMethod(instance, vtableIndex, layout, *emptyArray<Any>())

actual fun invokeStructReceiveArrayMethod(
    instance: ComPtr,
    vtableIndex: Int,
    layout: ComStructLayout,
    vararg arguments: Any,
): Result<Array<ComStructValue>> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeStructReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { emptyArray() },
    ) { dataPointer, size ->
        val structByteSize = Jdk22Foreign.structLayout(layout).byteSize()
        Array(size) { index ->
            val bytes = ByteArray(layout.byteSize)
            MemorySegment.ofArray(bytes).copyFrom(
                dataPointer.asSlice(index.toLong() * structByteSize, structByteSize),
            )
            ComStructValue(layout, bytes)
        }
    }
}
