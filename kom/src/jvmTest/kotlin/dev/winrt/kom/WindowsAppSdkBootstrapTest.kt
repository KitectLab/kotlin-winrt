package dev.winrt.kom

import org.junit.Assert.assertTrue
import org.junit.Test

class WindowsAppSdkBootstrapTest {
    @Test
    fun discovers_bootstrap_library_or_reports_absence() {
        if (!PlatformRuntime.isWindows) {
            return
        }

        val library = WindowsAppSdkBootstrap.discoverBootstrapLibrary()
        if (library != null) {
            assertTrue(library.path.toString(), library.path.toString().endsWith(".dll", ignoreCase = true))
        } else {
            val result = WindowsAppSdkBootstrap.initialize()
            assertTrue(result.exceptionOrNull().toString(), result.isFailure)
        }
    }

    @Test
    fun initializes_and_shuts_down_when_explicit_bootstrap_dll_is_configured() {
        if (!PlatformRuntime.isWindows) {
            return
        }

        val bootstrapDll = System.getProperty("dev.winrt.bootstrapDll").orEmpty()
        if (bootstrapDll.isBlank()) {
            return
        }

        val result = WindowsAppSdkBootstrap.initialize()
        if (result.isSuccess) {
            val library = result.getOrThrow()
            WindowsAppSdkBootstrap.shutdown(library)
                .getOrElse { throw AssertionError("Bootstrap shutdown failed", it) }
            return
        }

        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message, message.contains("0x80670016", ignoreCase = true))
    }
}
