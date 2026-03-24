package dev.winrt.kom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

        assertNotNull(Jdk22Foreign.windowsLookup)
        assertFalse(Jdk22Foreign.windowsLookup!!.find("CoInitializeEx").isEmpty)
        assertFalse(Jdk22Foreign.windowsLookup!!.find("CoUninitialize").isEmpty)
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
}
