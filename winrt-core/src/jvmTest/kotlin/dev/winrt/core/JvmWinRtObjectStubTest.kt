package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult
import dev.winrt.kom.PlatformComInterop
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmWinRtObjectStubTest {
    private val linker: Linker = Linker.nativeLinker()
    private val ole32: SymbolLookup? = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
        SymbolLookup.libraryLookup("ole32", Arena.ofAuto())
    } else {
        null
    }

    @Test
    fun object_stub_supports_multiple_interfaces_and_method_dispatch() {
        val primaryIid = guidOf("11111111-2222-3333-4444-555555555555")
        val secondaryIid = guidOf("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        var noArgInvoked = false
        var seenObjectArg = ComPtr(AbiIntPtr(0))

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(
                iid = primaryIid,
                noArgUnitMethods = mapOf(
                    6 to {
                        noArgInvoked = true
                        HResult(0)
                    },
                ),
            ),
            JvmWinRtObjectStub.InterfaceSpec(
                iid = secondaryIid,
                objectArgUnitMethods = mapOf(
                    6 to { arg ->
                        seenObjectArg = arg
                        HResult(0)
                    },
                ),
            ),
        ).use { stub ->
            PlatformComInterop.invokeUnitMethod(stub.primaryPointer, 6).getOrThrow()
            assertTrue(noArgInvoked)

            val secondary = PlatformComInterop.queryInterface(stub.primaryPointer, secondaryIid).getOrThrow()
            PlatformComInterop.invokeObjectSetter(secondary, 6, stub.primaryPointer).getOrThrow()
            assertEquals(stub.primaryPointer.value.rawValue, seenObjectArg.value.rawValue)
            PlatformComInterop.release(secondary)
        }
    }

    @Test
    fun object_stub_supports_iinspectable_object_results_and_query_interface_fallback() {
        val primaryIid = guidOf("12345678-1111-2222-3333-444444444444")
        val forwardedIid = guidOf("87654321-1111-2222-3333-444444444444")

        JvmWinRtObjectStub.createWithRuntimeClassName(
            runtimeClassName = "Example.RuntimeClass",
            JvmWinRtObjectStub.InterfaceSpec(
                iid = primaryIid,
                objectArgObjectMethods = mapOf(
                    6 to { arg -> PlatformComInterop.queryInterface(arg, forwardedIid).getOrThrow() },
                ),
            ),
        ).use { outer ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
            ).use { inner ->
                outer.setQueryInterfaceFallback { iid ->
                    if (iid.toString() == forwardedIid.toString()) {
                        PlatformComInterop.queryInterface(inner.primaryPointer, iid).getOrThrow()
                    } else {
                        null
                    }
                }

                val forwarded = PlatformComInterop.queryInterface(outer.primaryPointer, forwardedIid).getOrThrow()
                assertFalse(forwarded.isNull)
                PlatformComInterop.release(forwarded)

                val inspectable = PlatformComInterop.queryInterface(outer.primaryPointer, Inspectable.iinspectableIid).getOrThrow()
                PlatformComInterop.invokeHStringMethod(inspectable, 4).getOrThrow().use { className ->
                    assertEquals("Example.RuntimeClass", className.toKotlinString())
                }
                PlatformComInterop.release(inspectable)

                val echoed = PlatformComInterop.invokeObjectMethodWithObjectArg(
                    outer.primaryPointer,
                    6,
                    inner.primaryPointer,
                ).getOrThrow()
                assertEquals(inner.primaryPointer.value.rawValue, echoed.value.rawValue)
                PlatformComInterop.release(echoed)
            }
        }
    }

    @Test
    fun object_stub_reports_published_iids_via_iinspectable() {
        val primaryIid = guidOf("11111111-2222-3333-4444-555555555555")
        val secondaryIid = guidOf("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = primaryIid),
            JvmWinRtObjectStub.InterfaceSpec(iid = secondaryIid),
        ).use { stub ->
            val reportedIids = invokeGetIids(stub.primaryPointer)
            assertEquals(setOf(primaryIid.toString(), secondaryIid.toString()), reportedIids.map(Guid::toString).toSet())
        }
    }

    private fun invokeGetIids(pointer: ComPtr): List<Guid> {
        Arena.ofConfined().use { arena ->
            val iidCount = arena.allocate(ValueLayout.JAVA_INT)
            val iidArrayAddress = arena.allocate(ValueLayout.ADDRESS)
            val function = vtableEntry(pointer, 3)
            val getIids = linker.downcallHandle(
                function,
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                ),
            )
            val hresult = getIids.invokeWithArguments(
                MemorySegment.ofAddress(pointer.value.rawValue),
                iidCount,
                iidArrayAddress,
            ) as Int
            assertEquals(0, hresult)

            val count = iidCount.get(ValueLayout.JAVA_INT, 0L)
            val address = iidArrayAddress.get(ValueLayout.ADDRESS, 0L)
            if (count == 0 || address.address() == 0L) {
                return emptyList()
            }

            return try {
                val buffer = address.reinterpret(count * 16L)
                List(count) { index -> readGuid(buffer.asSlice(index * 16L, 16L)) }
            } finally {
                if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
                    val coTaskMemFree = ole32?.find("CoTaskMemFree")?.get()?.let { symbol ->
                        linker.downcallHandle(symbol, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS))
                    }
                    coTaskMemFree?.invokeWithArguments(address)
                }
            }
        }
    }

    private fun vtableEntry(pointer: ComPtr, slot: Int): MemorySegment {
        val instance = MemorySegment.ofAddress(pointer.value.rawValue).reinterpret(ValueLayout.ADDRESS.byteSize())
        val vtable = instance.get(ValueLayout.ADDRESS, 0L)
        return vtable.reinterpret((slot + 1L) * ValueLayout.ADDRESS.byteSize()).getAtIndex(ValueLayout.ADDRESS, slot.toLong())
    }

    private fun readGuid(segment: MemorySegment): Guid {
        val guidSegment = segment.reinterpret(16L)
        return Guid(
            data1 = guidSegment.get(ValueLayout.JAVA_INT, 0L),
            data2 = guidSegment.get(ValueLayout.JAVA_SHORT, 4L),
            data3 = guidSegment.get(ValueLayout.JAVA_SHORT, 6L),
            data4 = ByteArray(8) { index ->
                guidSegment.get(ValueLayout.JAVA_BYTE, 8L + index)
            },
        )
    }
}
