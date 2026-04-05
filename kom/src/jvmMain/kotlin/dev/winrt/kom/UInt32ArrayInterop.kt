@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.winrt.kom

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

private val coTaskMemFreeUInt32Handle by lazy {
    Jdk22Foreign.downcall(
        "CoTaskMemFree",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
    )
}

private fun uint32ReceiveArrayMethodHandle(argumentLayouts: List<MemoryLayout>) = Jdk22Foreign.downcallHandle(
    FunctionDescriptor.of(
        ValueLayout.JAVA_INT,
        ValueLayout.ADDRESS,
        *argumentLayouts.toTypedArray(),
        ValueLayout.ADDRESS,
        ValueLayout.ADDRESS,
    ),
)

actual fun invokeUInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<UIntArray> =
    invokeUInt32ReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeUInt32ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<UIntArray> {
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
                    uint32ReceiveArrayMethodHandle(arguments.map(::methodArgumentLayout)).bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        *preparedArguments.values.toTypedArray(),
                        sizeSegment,
                        dataSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeUInt32ReceiveArrayMethod($vtableIndex)")

                val size = sizeSegment.get(ValueLayout.JAVA_INT, 0L)
                check(size >= 0) { "Negative UInt32 array size returned from slot $vtableIndex: $size" }

                dataPointer = dataSegment.get(ValueLayout.ADDRESS, 0L)
                if (size == 0) {
                    return@use UIntArray(0)
                }
                check(dataPointer.address() != 0L) {
                    "Null UInt32 array buffer returned from slot $vtableIndex with size $size"
                }

                val valuesSegment = dataPointer.reinterpret(size.toLong() * ValueLayout.JAVA_INT.byteSize())
                UIntArray(size) { index ->
                    valuesSegment.getAtIndex(ValueLayout.JAVA_INT, index.toLong()).toUInt()
                }
            } finally {
                preparedArguments.close()
                if (dataPointer.address() != 0L) {
                    coTaskMemFreeUInt32Handle.invokeWithArguments(dataPointer)
                }
            }
        }
    }
}
