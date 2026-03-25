package dev.winrt.sample.jvm

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleBootstrapTest {
    @Test
    fun sample_can_run_through_launcher() {
        SampleBootstrap.configure()
        SampleBootstrap.launcher = object : WinUiApplicationLauncher {
            override fun launch(): SampleLaunchResult {
                return SampleLaunchResult(
                    diagnostics = "diagnostics",
                    launcherSummary = "launcher",
                )
            }
        }

        val result = SampleBootstrap.launch()
        assertEquals("diagnostics", result.diagnostics)
        assertEquals("launcher", result.launcherSummary)
    }
}
