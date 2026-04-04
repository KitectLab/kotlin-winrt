package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import dev.winrt.kom.PlatformComInterop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmWinRtObjectStubTest {
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
}
