package dev.winrt.sample.jvm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
                    winRtSmoke = WinRtApiSmokeResult(
                        runtimeClass = "Windows.Data.Json.JsonObject",
                        parsedName = "codex",
                    ),
                )
            }
        }

        val result = SampleBootstrap.launch()
        assertEquals("diagnostics", result.diagnostics)
        assertEquals("launcher", result.launcherSummary)
        assertEquals("Windows.Data.Json.JsonObject", result.winRtSmoke?.runtimeClass)
        assertEquals("codex", result.winRtSmoke?.parsedName)
        assertNotNull(SampleBootstrap.diagnostics())
        SampleBootstrap.shutdown()
    }
}
