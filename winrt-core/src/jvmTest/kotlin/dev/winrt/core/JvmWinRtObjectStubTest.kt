package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import dev.winrt.kom.PlatformComInterop
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
