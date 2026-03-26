package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmWinRtObjectArgDelegateTest {
    @Test
    fun delegate_supports_query_interface_and_invoke() {
        val seenPointer = AtomicLong(0L)
        val iid = guidOf("d8ea1239-1234-56f1-9963-45dd9c80a661")
        JvmWinRtObjectArgDelegate.create(iid) { arg ->
            seenPointer.set(arg.value.rawValue)
            HResult(0)
        }.use { callback ->
            val callbackPointer = MemorySegment.ofAddress(callback.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)

            val queryInterface = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 1).getAtIndex(ValueLayout.ADDRESS, 0),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                ),
            )
            val invoke = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                ),
            )

            java.lang.foreign.Arena.ofConfined().use { arena ->
                val iidSegment = arena.allocate(16)
                iidSegment.set(ValueLayout.JAVA_INT, 0L, iid.data1)
                iidSegment.set(ValueLayout.JAVA_SHORT, 4L, iid.data2)
                iidSegment.set(ValueLayout.JAVA_SHORT, 6L, iid.data3)
                iid.data4.forEachIndexed { index, byte ->
                    iidSegment.set(ValueLayout.JAVA_BYTE, 8L + index, byte)
                }
                val resultPointer = arena.allocate(ValueLayout.ADDRESS)
                val hr = queryInterface.invokeWithArguments(
                    callbackPointer,
                    iidSegment,
                    resultPointer,
                ) as Int
                assertEquals(0, hr)
                assertTrue(resultPointer.get(ValueLayout.ADDRESS, 0L).address() != 0L)
            }

            val invokeResult = invoke.invokeWithArguments(
                callbackPointer,
                MemorySegment.ofAddress(0x1234L),
            ) as Int
            assertEquals(0, invokeResult)
            assertEquals(ComPtr(AbiIntPtr(0x1234L)).value.rawValue, seenPointer.get())
        }
    }
}
