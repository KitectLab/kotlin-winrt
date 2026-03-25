package dev.winrt.sample.jvm

import dev.winrt.kom.KomSmoke
import dev.winrt.kom.PlatformRuntime
import microsoft.ui.xaml.Application
import microsoft.ui.xaml.Window

class SampleLaunchResult(
    val diagnostics: String,
    val launcherSummary: String,
    val winRtSmoke: WinRtApiSmokeResult?,
)

interface WinUiApplicationLauncher {
    fun launch(): SampleLaunchResult
}

object DefaultWinUiApplicationLauncher : WinUiApplicationLauncher {
    override fun launch(): SampleLaunchResult {
        val app = Application.activate()
        val window = Window.activateInstance()
        window.title = "kotlin-winrt sample"
        app.start()
        window.activate()

        val summary = if (PlatformRuntime.isWindows) {
            "Sample launcher completed with JDK 22+ FFM COM initialization. Replace DefaultWinUiApplicationLauncher with a real WinUI 3 launcher bridge."
        } else {
            "Non-Windows host detected. Static sample launcher path executed without native WinUI 3 launch."
        }

        return SampleLaunchResult(
            diagnostics = KomSmoke.description(),
            launcherSummary = summary,
            winRtSmoke = if (PlatformRuntime.isWindows) WinRtApiSmoke.run() else null,
        )
    }
}
