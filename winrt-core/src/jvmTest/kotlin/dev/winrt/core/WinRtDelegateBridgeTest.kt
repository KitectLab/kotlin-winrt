package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WinRtDelegateBridgeTest {
    @Test
    fun creates_no_arg_unit_delegate_handle() {
        val iid = guidOf("11111111-2222-3333-4444-555555555555")
        var invoked = false

        WinRtDelegateBridge.createNoArgUnitDelegate(iid) {
            invoked = true
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                ),
            )

            val hresult = function.invokeWithArguments(callbackPointer) as Int
            assertEquals(0, hresult)
            assertEquals(true, invoked)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_object_arg_unit_delegate_handle() {
        val iid = guidOf("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: ComPtr? = null
        val arg = ComPtr(AbiIntPtr(0x1234L))

        WinRtDelegateBridge.createObjectArgUnitDelegate(iid) { value ->
            captured = value
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                ),
            )

            val hresult = function.invokeWithArguments(callbackPointer, MemorySegment.ofAddress(arg.value.rawValue)) as Int
            assertEquals(0, hresult)
            assertEquals(arg, captured)
            assertFalse(handle.pointer.isNull)
        }
    }
}
