package dev.winrt.kom

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

private val coTaskMemFreeInt64Handle by lazy {
    Jdk22Foreign.downcall(
        "CoTaskMemFree",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
    )
}

private fun int64ReceiveArrayMethodHandle(argumentLayouts: List<MemoryLayout>) = Jdk22Foreign.downcallHandle(
    FunctionDescriptor.of(
        ValueLayout.JAVA_INT,
        ValueLayout.ADDRESS,
        *argumentLayouts.toTypedArray(),
        ValueLayout.ADDRESS,
        ValueLayout.ADDRESS,
    ),
)

actual fun invokeInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<LongArray> =
    invokeInt64ReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeInt64ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<LongArray> {
    return runCatching {
        require(!instance.isNull) { "Method invocation requires a non-null COM pointer" }
        val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
        Arena.ofConfined().use { arena ->
            val sizeSegment = arena.allocate(ValueLayout.JAVA_INT)
            val dataSegment = arena.allocate(ValueLayout.ADDRESS)
            val preparedArguments = prepareAbiArguments(arguments)
            var dataPointer = MemorySegment.NULL
            try {
                val hresult = HResult(
                    int64ReceiveArrayMethodHandle(arguments.map(::methodArgumentLayout)).bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        *preparedArguments.values.toTypedArray(),
                        sizeSegment,
                        dataSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeInt64ReceiveArrayMethod($vtableIndex)")

                val size = sizeSegment.get(ValueLayout.JAVA_INT, 0L)
                check(size >= 0) { "Negative Int64 array size returned from slot $vtableIndex: $size" }

                dataPointer = dataSegment.get(ValueLayout.ADDRESS, 0L)
                if (size == 0) {
                    return@use LongArray(0)
                }
                check(dataPointer.address() != 0L) {
                    "Null Int64 array buffer returned from slot $vtableIndex with size $size"
                }

                val valuesSegment = dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_LONG.byteSize())
                LongArray(size) { index ->
                    valuesSegment.getAtIndex(ValueLayout.JAVA_LONG, index.toLong())
                }
            } finally {
                preparedArguments.close()
                if (dataPointer.address() != 0L) {
                    coTaskMemFreeInt64Handle.invokeWithArguments(dataPointer)
                }
            }
        }
    }
}
