package dev.winrt.kom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class Jdk22ForeignTest {
    @Test
    fun jvm_runtime_reports_ffm_backend() {
        assertEquals("jdk22-ffm", PlatformRuntime.ffiBackend)
        assertNotNull(Jdk22Foreign.linker)
    }

    @Test
    fun windows_runtime_resolves_com_symbols() {
        assumeTrue(PlatformRuntime.isWindows)

        assertFalse(Jdk22Foreign.windowsLookups.isEmpty())
        assertNotNull(Jdk22Foreign.downcall("CoInitializeEx", java.lang.foreign.FunctionDescriptor.of(java.lang.foreign.ValueLayout.JAVA_INT, java.lang.foreign.ValueLayout.ADDRESS, java.lang.foreign.ValueLayout.JAVA_INT)))
        assertNotNull(Jdk22Foreign.downcall("CoUninitialize", java.lang.foreign.FunctionDescriptor.ofVoid()))
        assertNotNull(Jdk22Foreign.downcall("RoInitialize", java.lang.foreign.FunctionDescriptor.of(java.lang.foreign.ValueLayout.JAVA_INT, java.lang.foreign.ValueLayout.JAVA_INT)))
        assertNotNull(Jdk22Foreign.downcall("RoGetActivationFactory", java.lang.foreign.FunctionDescriptor.of(java.lang.foreign.ValueLayout.JAVA_INT, java.lang.foreign.ValueLayout.ADDRESS, java.lang.foreign.ValueLayout.ADDRESS, java.lang.foreign.ValueLayout.ADDRESS)))
    }

    @Test
    fun downcall_handle_cache_reuses_same_descriptor_handle() {
        val descriptor = java.lang.foreign.FunctionDescriptor.of(
            java.lang.foreign.ValueLayout.JAVA_INT,
            java.lang.foreign.ValueLayout.ADDRESS,
        )

        val first = Jdk22Foreign.downcallHandle(descriptor)
        val second = Jdk22Foreign.downcallHandle(descriptor)

        assertSame(first, second)
    }

    @Test
    fun windows_runtime_can_initialize_com() {
        assumeTrue(PlatformRuntime.isWindows)

        val result = JvmComRuntime.initializeMultithreaded()
        val shouldUninitialize = result.isSuccess

        try {
            val accepted = result.isSuccess || result == KnownHResults.RPC_E_CHANGED_MODE
            assertEquals(true, accepted)
        } finally {
            if (shouldUninitialize) {
                JvmComRuntime.uninitialize()
            }
        }
    }

    @Test
    fun windows_runtime_can_get_activation_factory_for_calendar() {
        assumeTrue(PlatformRuntime.isWindows)

        val initResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitialize = initResult.isSuccess

        try {
            val factory = JvmWinRtRuntime.getActivationFactory("Windows.Globalization.Calendar")
                .getOrThrow()

            try {
                assertFalse(factory.isNull)
                val instance = JvmWinRtRuntime.activateInstance(factory).getOrThrow()
                assertFalse(instance.isNull)
                PlatformComInterop.release(instance)
            } finally {
                PlatformComInterop.release(factory)
            }
        } finally {
            if (shouldUninitialize) {
                JvmWinRtRuntime.uninitialize()
            }
        }
    }

    @Test
    fun windows_runtime_hstring_roundtrip_works() {
        assumeTrue(PlatformRuntime.isWindows)

        val hString = JvmWinRtRuntime.createHString("hello winrt")
        try {
            assertEquals("hello winrt", JvmWinRtRuntime.toKotlinString(hString))
            assertTrue(!hString.isNull)
        } finally {
            JvmWinRtRuntime.releaseHString(hString)
        }
    }
}
