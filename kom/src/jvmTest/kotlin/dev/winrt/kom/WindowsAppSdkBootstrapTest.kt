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
}
