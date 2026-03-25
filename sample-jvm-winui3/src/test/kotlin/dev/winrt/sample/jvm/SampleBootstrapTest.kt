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
                    winRtSmoke = WinRtApiSmokeResult(
                        runtimeClass = "Windows.Globalization.Calendar",
                        activationFactoryAcquired = true,
                        instanceActivated = true,
                    ),
                )
            }
        }

        val result = SampleBootstrap.launch()
        assertEquals("diagnostics", result.diagnostics)
        assertEquals("launcher", result.launcherSummary)
        assertEquals("Windows.Globalization.Calendar", result.winRtSmoke?.runtimeClass)
        assertEquals(true, result.winRtSmoke?.activationFactoryAcquired)
        assertEquals(true, result.winRtSmoke?.instanceActivated)
    }
}
