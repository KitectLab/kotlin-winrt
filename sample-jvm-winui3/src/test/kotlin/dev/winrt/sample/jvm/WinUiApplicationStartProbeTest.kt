package dev.winrt.sample.jvm

import dev.winrt.kom.PlatformRuntime
import org.junit.Assert.assertEquals
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

    @Test
    fun application_start_callback_receives_initialization_params() {
        assumeTrue(PlatformRuntime.isWindows)

        SampleBootstrap.configure()
        try {
            assertTrue(WinUiApplicationStart.probeCallbackParamsInterface())
        } finally {
            SampleBootstrap.shutdown()
        }
    }

    @Test
    fun application_start_launches_visible_window() {
        assumeTrue(PlatformRuntime.isWindows)

        val previous = System.getProperty("dev.winrt.autoQuitVisible")
        System.setProperty("dev.winrt.autoQuitVisible", "true")
        SampleBootstrap.configure()
        try {
            assertEquals(
                "xaml=application-start-visible",
                WinUiApplicationStart.launchWindow(
                    windowTitle = "kotlin-winrt probe",
                    messageText = "Hello from probe",
                ),
            )
        } finally {
            if (previous == null) {
                System.clearProperty("dev.winrt.autoQuitVisible")
            } else {
                System.setProperty("dev.winrt.autoQuitVisible", previous)
            }
            SampleBootstrap.shutdown()
        }
    }
}
