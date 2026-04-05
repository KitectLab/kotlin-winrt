package dev.winrt.kom

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

private val coTaskMemFreeStructHandle by lazy {
    Jdk22Foreign.downcall(
        "CoTaskMemFree",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
    )
}

private fun structReceiveArrayMethodHandle(argumentLayouts: List<MemoryLayout>) = Jdk22Foreign.downcallHandle(
    FunctionDescriptor.of(
        ValueLayout.JAVA_INT,
        ValueLayout.ADDRESS,
        *argumentLayouts.toTypedArray(),
        ValueLayout.ADDRESS,
        ValueLayout.ADDRESS,
    ),
)

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
    return runCatching {
        require(!instance.isNull) { "Method invocation requires a non-null COM pointer" }
        val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
        val structByteSize = Jdk22Foreign.structLayout(layout).byteSize()
        Arena.ofConfined().use { arena ->
            val sizeSegment = arena.allocate(ValueLayout.JAVA_INT)
            val dataSegment = arena.allocate(ValueLayout.ADDRESS)
            val preparedArguments = prepareAbiArguments(arguments)
            var dataPointer = MemorySegment.NULL
            try {
                val hresult = HResult(
                    structReceiveArrayMethodHandle(arguments.map(::methodArgumentLayout)).bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        *preparedArguments.values.toTypedArray(),
                        sizeSegment,
                        dataSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeStructReceiveArrayMethod($vtableIndex)")

                val size = sizeSegment.get(ValueLayout.JAVA_INT, 0L)
                check(size >= 0) { "Negative struct array size returned from slot $vtableIndex: $size" }

                dataPointer = dataSegment.get(ValueLayout.ADDRESS, 0L)
                if (size == 0) {
                    return@use emptyArray()
                }
                check(dataPointer.address() != 0L) {
                    "Null struct array buffer returned from slot $vtableIndex with size $size"
                }

                Array(size) { index ->
                    val bytes = ByteArray(layout.byteSize)
                    MemorySegment.ofArray(bytes).copyFrom(
                        dataPointer.asSlice(index.toLong() * structByteSize, structByteSize),
                    )
                    ComStructValue(layout, bytes)
                }
            } finally {
                preparedArguments.close()
                if (dataPointer.address() != 0L) {
                    coTaskMemFreeStructHandle.invokeWithArguments(dataPointer)
                }
            }
        }
    }
}
