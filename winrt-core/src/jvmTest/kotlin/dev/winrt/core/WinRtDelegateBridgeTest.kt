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

        WinRtDelegateBridge.createUnitDelegate(
            iid = iid,
            parameterKinds = emptyList(),
        ) { args ->
            assertTrue(args.isEmpty())
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

        WinRtDelegateBridge.createUnitDelegate(
            iid = iid,
            parameterKinds = listOf(WinRtDelegateValueKind.OBJECT),
        ) { args ->
            captured = args[0] as ComPtr
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

        WinRtDelegateBridge.createBooleanDelegate(
            iid = iid,
            parameterKinds = emptyList(),
        ) { args ->
            assertTrue(args.isEmpty())
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

        WinRtDelegateBridge.createBooleanDelegate(
            iid = iid,
            parameterKinds = listOf(WinRtDelegateValueKind.OBJECT),
        ) { args ->
            captured = args[0] as ComPtr
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

        WinRtDelegateBridge.createUnitDelegate(
            iid = iid,
            parameterKinds = listOf(WinRtDelegateValueKind.INT32),
        ) { args ->
            captured = args[0] as Int
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
    fun creates_int32_arg_boolean_delegate_handle() {
        val iid = guidOf("acacacac-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: Int? = null

        WinRtDelegateBridge.createBooleanDelegate(
            iid = iid,
            parameterKinds = listOf(WinRtDelegateValueKind.INT32),
        ) { args ->
            val value = args[0] as Int
            captured = value
            value > 0
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                ),
            )
            val result = Arena.ofConfined().use { arena ->
                val out = arena.allocate(ValueLayout.JAVA_INT)
                val hresult = function.invokeWithArguments(callbackPointer, 7, out) as Int
                assertEquals(0, hresult)
                out.get(ValueLayout.JAVA_INT, 0L)
            }

            assertEquals(7, captured)
            assertEquals(1, result)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_string_arg_unit_delegate_handle() {
        val iid = guidOf("dddddddd-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: String? = null
        val hString = PlatformHStringBridge.create("hello")

        try {
            WinRtDelegateBridge.createUnitDelegate(
                iid = iid,
                parameterKinds = listOf(WinRtDelegateValueKind.STRING),
            ) { args ->
                captured = args[0] as String
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
    fun creates_string_arg_boolean_delegate_handle() {
        val iid = guidOf("bcbcbcbc-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: String? = null
        val hString = PlatformHStringBridge.create("hello")

        try {
            WinRtDelegateBridge.createBooleanDelegate(
                iid = iid,
                parameterKinds = listOf(WinRtDelegateValueKind.STRING),
            ) { args ->
                val value = args[0] as String
                captured = value
                value == "hello"
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
                    val hresult = function.invokeWithArguments(callbackPointer, MemorySegment.ofAddress(hString.raw), out) as Int
                    assertEquals(0, hresult)
                    out.get(ValueLayout.JAVA_INT, 0L)
                }

                assertEquals("hello", captured)
                assertEquals(1, result)
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

        WinRtDelegateBridge.createUnitDelegate(
            iid = iid,
            parameterKinds = listOf(WinRtDelegateValueKind.UINT32),
        ) { args ->
            captured = args[0] as UInt
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
    fun creates_uint32_arg_boolean_delegate_handle() {
        val iid = guidOf("dededede-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: UInt? = null

        WinRtDelegateBridge.createBooleanDelegate(
            iid = iid,
            parameterKinds = listOf(WinRtDelegateValueKind.UINT32),
        ) { args ->
            val value = args[0] as UInt
            captured = value
            value == UInt.MAX_VALUE
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                ),
            )
            val result = Arena.ofConfined().use { arena ->
                val out = arena.allocate(ValueLayout.JAVA_INT)
                val hresult = function.invokeWithArguments(callbackPointer, -1, out) as Int
                assertEquals(0, hresult)
                out.get(ValueLayout.JAVA_INT, 0L)
            }

            assertEquals(UInt.MAX_VALUE, captured)
            assertEquals(1, result)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_boolean_arg_unit_delegate_handle() {
        val iid = guidOf("f0f0f0f0-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: Boolean? = null

        WinRtDelegateBridge.createUnitDelegate(
            iid = iid,
            parameterKinds = listOf(WinRtDelegateValueKind.BOOLEAN),
        ) { args ->
            captured = args[0] as Boolean
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

            val hresult = function.invokeWithArguments(callbackPointer, 1) as Int
            assertEquals(0, hresult)
            assertEquals(true, captured)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_int64_arg_unit_delegate_handle() {
        val iid = guidOf("abababab-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: Long? = null

        WinRtDelegateBridge.createUnitDelegate(
            iid = iid,
            parameterKinds = listOf(WinRtDelegateValueKind.INT64),
        ) { args ->
            captured = args[0] as Long
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_LONG,
                ),
            )

            val hresult = function.invokeWithArguments(callbackPointer, 1234567890123L) as Int
            assertEquals(0, hresult)
            assertEquals(1234567890123L, captured)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_int64_arg_boolean_delegate_handle() {
        val iid = guidOf("edededed-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: Long? = null

        WinRtDelegateBridge.createBooleanDelegate(
            iid = iid,
            parameterKinds = listOf(WinRtDelegateValueKind.INT64),
        ) { args ->
            val value = args[0] as Long
            captured = value
            value > 0
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS,
                ),
            )
            val result = Arena.ofConfined().use { arena ->
                val out = arena.allocate(ValueLayout.JAVA_INT)
                val hresult = function.invokeWithArguments(callbackPointer, 7L, out) as Int
                assertEquals(0, hresult)
                out.get(ValueLayout.JAVA_INT, 0L)
            }

            assertEquals(7L, captured)
            assertEquals(1, result)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_uint64_arg_unit_delegate_handle() {
        val iid = guidOf("cdcdcdcd-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: ULong? = null

        WinRtDelegateBridge.createUnitDelegate(
            iid = iid,
            parameterKinds = listOf(WinRtDelegateValueKind.UINT64),
        ) { args ->
            captured = args[0] as ULong
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_LONG,
                ),
            )

            val hresult = function.invokeWithArguments(callbackPointer, -1L) as Int
            assertEquals(0, hresult)
            assertEquals(ULong.MAX_VALUE, captured)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_multi_parameter_unit_delegate_handle() {
        val iid = guidOf("eeeeeeee-1111-2222-3333-444444444444")
        var capturedInt: Int? = null
        var capturedString: String? = null
        val hString = PlatformHStringBridge.create("alpha")

        try {
            WinRtDelegateBridge.createUnitDelegate(
                iid = iid,
                parameterKinds = listOf(WinRtDelegateValueKind.INT32, WinRtDelegateValueKind.STRING),
            ) { args ->
                capturedInt = args[0] as Int
                capturedString = args[1] as String
            }.use { handle ->
                val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
                val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
                val function = Linker.nativeLinker().downcallHandle(
                    vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                    FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                    ),
                )

                val hresult = function.invokeWithArguments(callbackPointer, 7, MemorySegment.ofAddress(hString.raw)) as Int
                assertEquals(0, hresult)
                assertEquals(7, capturedInt)
                assertEquals("alpha", capturedString)
                assertFalse(handle.pointer.isNull)
            }
        } finally {
            PlatformHStringBridge.release(hString)
        }
    }

    @Test
    fun creates_three_parameter_boolean_delegate_handle() {
        val iid = guidOf("ffffffff-1111-2222-3333-555555555555")
        var capturedInt: Int? = null
        var capturedString: String? = null
        var capturedFlag: Boolean? = null
        val hString = PlatformHStringBridge.create("beta")

        try {
            WinRtDelegateBridge.createBooleanDelegate(
                iid = iid,
                parameterKinds = listOf(
                    WinRtDelegateValueKind.INT32,
                    WinRtDelegateValueKind.STRING,
                    WinRtDelegateValueKind.BOOLEAN,
                ),
            ) { args ->
                capturedInt = args[0] as Int
                capturedString = args[1] as String
                capturedFlag = args[2] as Boolean
                true
            }.use { handle ->
                val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
                val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
                val function = Linker.nativeLinker().downcallHandle(
                    vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                    FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                    ),
                )

                val result = Arena.ofConfined().use { arena ->
                    val out = arena.allocate(ValueLayout.JAVA_INT)
                    val hresult = function.invokeWithArguments(callbackPointer, 7, MemorySegment.ofAddress(hString.raw), 1, out) as Int
                    assertEquals(0, hresult)
                    out.get(ValueLayout.JAVA_INT, 0L)
                }

                assertEquals(7, capturedInt)
                assertEquals("beta", capturedString)
                assertEquals(true, capturedFlag)
                assertEquals(1, result)
                assertFalse(handle.pointer.isNull)
            }
        } finally {
            PlatformHStringBridge.release(hString)
        }
    }

    @Test
    fun creates_four_parameter_unit_delegate_handle() {
        val iid = guidOf("12341234-1111-2222-3333-444444444444")
        val captured = mutableListOf<Int>()

        WinRtDelegateBridge.createUnitDelegate(
            iid = iid,
            parameterKinds = listOf(
                WinRtDelegateValueKind.INT32,
                WinRtDelegateValueKind.INT32,
                WinRtDelegateValueKind.INT32,
                WinRtDelegateValueKind.INT32,
            ),
        ) { args ->
            captured += args.map { it as Int }
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                ),
            )

            val hresult = function.invokeWithArguments(callbackPointer, 1, 2, 3, 4) as Int
            assertEquals(0, hresult)
            assertEquals(listOf(1, 2, 3, 4), captured)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_four_parameter_boolean_delegate_handle() {
        val iid = guidOf("56785678-1111-2222-3333-444444444444")
        val captured = mutableListOf<Int>()

        WinRtDelegateBridge.createBooleanDelegate(
            iid = iid,
            parameterKinds = listOf(
                WinRtDelegateValueKind.INT32,
                WinRtDelegateValueKind.INT32,
                WinRtDelegateValueKind.INT32,
                WinRtDelegateValueKind.INT32,
            ),
        ) { args ->
            captured += args.map { it as Int }
            true
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                ),
            )
            val result = Arena.ofConfined().use { arena ->
                val out = arena.allocate(ValueLayout.JAVA_INT)
                val hresult = function.invokeWithArguments(callbackPointer, 1, 2, 3, 4, out) as Int
                assertEquals(0, hresult)
                out.get(ValueLayout.JAVA_INT, 0L)
            }

            assertEquals(listOf(1, 2, 3, 4), captured)
            assertEquals(1, result)
            assertFalse(handle.pointer.isNull)
        }
    }

    @Test
    fun creates_float32_arg_unit_delegate_handle() {
        val iid = guidOf("ffffffff-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: Float? = null

        WinRtDelegateBridge.createUnitDelegate(
            iid = iid,
            parameterKinds = listOf(WinRtDelegateValueKind.FLOAT32),
        ) { args ->
            captured = args[0] as Float
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

    @Test
    fun creates_float64_arg_unit_delegate_handle() {
        val iid = guidOf("10101010-bbbb-cccc-dddd-eeeeeeeeeeee")
        var captured: Double? = null

        WinRtDelegateBridge.createUnitDelegate(
            iid = iid,
            parameterKinds = listOf(WinRtDelegateValueKind.FLOAT64),
        ) { args ->
            captured = args[0] as Double
        }.use { handle ->
            val callbackPointer = MemorySegment.ofAddress(handle.pointer.value.rawValue)
            val vtablePointer = callbackPointer.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L)
            val function = Linker.nativeLinker().downcallHandle(
                vtablePointer.reinterpret(ValueLayout.ADDRESS.byteSize() * 4).getAtIndex(ValueLayout.ADDRESS, 3),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_DOUBLE,
                ),
            )

            val hresult = function.invokeWithArguments(callbackPointer, 2.5) as Int
            assertEquals(0, hresult)
            assertEquals(2.5, captured)
            assertFalse(handle.pointer.isNull)
        }
    }
}
