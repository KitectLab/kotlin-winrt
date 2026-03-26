package dev.winrt.sample.jvm

import dev.winrt.kom.PlatformRuntime
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class WinUiApplicationStartProbeTest {
    @Test
    fun application_start_invokes_kotlin_callback() {
        assumeTrue(PlatformRuntime.isWindows)

        SampleBootstrap.configure()
        try {
            assertTrue(WinUiApplicationStart.probeCallbackOnly())
        } finally {
            SampleBootstrap.shutdown()
        }
    }
}
