package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformHStringBridge
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun creates_no_arg_boolean_delegate_handle() {
        val iid = guidOf("99999999-2222-3333-4444-555555555555")
        var invoked = false

        WinRtDelegateBridge.createNoArgBooleanDelegate(iid) {
            invoked = true
            true
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
            val result = Arena.ofConfined().use { arena ->
                val out = arena.allocate(ValueLayout.JAVA_INT)
                val hresult = function.invokeWithArguments(callbackPointer, out) as Int
                assertEquals(0, hresult)
                out.get(ValueLayout.JAVA_INT, 0L)
            }

            assertTrue(invoked)
            assertEquals(1, result)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_object_arg_boolean_delegate_handle() {
        val iid = guidOf("bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: ComPtr? = null
        val arg = ComPtr(AbiIntPtr(0x1234L))

        WinRtDelegateBridge.createObjectArgBooleanDelegate(iid) { value ->
            captured = value
            true
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                ),
            )
            val result = Arena.ofConfined().use { arena ->
                val out = arena.allocate(ValueLayout.JAVA_INT)
                val hresult = function.invokeWithArguments(callbackPointer, MemorySegment.ofAddress(arg.value.rawValue), out) as Int
                assertEquals(0, hresult)
                out.get(ValueLayout.JAVA_INT, 0L)
            }

            assertEquals(arg, captured)
            assertEquals(1, result)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_int32_arg_unit_delegate_handle() {
        val iid = guidOf("cccccccc-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: Int? = null

        WinRtDelegateBridge.createInt32ArgUnitDelegate(iid) { value ->
            captured = value
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT,
                ),
            )

            val hresult = function.invokeWithArguments(callbackPointer, 123) as Int
            assertEquals(0, hresult)
            assertEquals(123, captured)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_string_arg_unit_delegate_handle() {
        val iid = guidOf("dddddddd-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: String? = null
        val hString = PlatformHStringBridge.create("hello")

        try {
            WinRtDelegateBridge.createStringArgUnitDelegate(iid) { value ->
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

                val hresult = function.invokeWithArguments(callbackPointer, MemorySegment.ofAddress(hString.raw)) as Int
                assertEquals(0, hresult)
                assertEquals("hello", captured)
                assertFalse(handle.pointer.isNull)
            }
        } finally {
            PlatformHStringBridge.release(hString)
        }
    }

    @Test
    fun creates_uint32_arg_unit_delegate_handle() {
        val iid = guidOf("eeeeeeee-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: UInt? = null

        WinRtDelegateBridge.createUInt32ArgUnitDelegate(iid) { value ->
            captured = value
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT,
                ),
            )

            val hresult = function.invokeWithArguments(callbackPointer, -1) as Int
            assertEquals(0, hresult)
            assertEquals(UInt.MAX_VALUE, captured)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_float32_arg_unit_delegate_handle() {
        val iid = guidOf("ffffffff-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: Float? = null

        WinRtDelegateBridge.createFloat32ArgUnitDelegate(iid) { value ->
            captured = value
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_FLOAT,
                ),
            )

            val hresult = function.invokeWithArguments(callbackPointer, 1.5f) as Int
            assertEquals(0, hresult)
            assertEquals(1.5f, captured)
            assertFalse(handle.pointer.isNull)
        }
    }
}
