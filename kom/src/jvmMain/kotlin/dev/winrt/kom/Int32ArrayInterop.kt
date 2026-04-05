package dev.winrt.kom

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

private val int32ReceiveArrayMethodHandle by lazy {
    Jdk22Foreign.downcallHandle(
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
        ),
    )
}

private val coTaskMemFreeHandle by lazy {
    Jdk22Foreign.downcall(
        "CoTaskMemFree",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
    )
}

actual fun invokeInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<IntArray> {
    return runCatching {
        require(!instance.isNull) { "Method invocation requires a non-null COM pointer" }
        val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
        Arena.ofConfined().use { arena ->
            val sizeSegment = arena.allocate(ValueLayout.JAVA_INT)
            val dataSegment = arena.allocate(ValueLayout.ADDRESS)
            var dataPointer = MemorySegment.NULL
            try {
                val hresult = HResult(
                    int32ReceiveArrayMethodHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        sizeSegment,
                        dataSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeInt32ReceiveArrayMethod($vtableIndex)")

                val size = sizeSegment.get(ValueLayout.JAVA_INT, 0L)
                check(size >= 0) { "Negative Int32 array size returned from slot $vtableIndex: $size" }

                dataPointer = dataSegment.get(ValueLayout.ADDRESS, 0L)
                if (size == 0) {
                    return@use IntArray(0)
                }
                check(dataPointer.address() != 0L) {
                    "Null Int32 array buffer returned from slot $vtableIndex with size $size"
                }

                val valuesSegment = dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_INT.byteSize())
                IntArray(size) { index ->
                    valuesSegment.getAtIndex(ValueLayout.JAVA_INT, index.toLong())
                }
            } finally {
                if (dataPointer.address() != 0L) {
                    coTaskMemFreeHandle.invokeWithArguments(dataPointer)
                }
            }
        }
    }
}
