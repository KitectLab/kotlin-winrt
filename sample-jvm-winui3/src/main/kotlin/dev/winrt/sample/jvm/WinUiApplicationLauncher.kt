package dev.winrt.sample.jvm

import dev.winrt.kom.KomSmoke
import dev.winrt.kom.PlatformRuntime

class SampleLaunchResult(
    val diagnostics: String,
    val launcherSummary: String,
    val winRtSmoke: WinRtApiSmokeResult?,
)

interface WinUiApplicationLauncher {
    fun launch(): SampleLaunchResult
}

object DefaultWinUiApplicationLauncher : WinUiApplicationLauncher {
    private const val sampleWindowTitle = "kotlin-winrt sample"

    override fun launch(): SampleLaunchResult {
        val packageState = WindowsAppSdkEnvironment.detect()
        val activationSummary = when {
            !PlatformRuntime.isWindows -> "xaml=skipped(non-windows)"
            !SampleBootstrap.isWindowsAppSdkReady() -> "xaml=blocked(bootstrap-not-ready)"
            else -> runCatching {
                WinUiApplicationStart.launchWindow(
                    windowTitle = sampleWindowTitle,
                    messageText = "Hello from Kotlin/WinUI 3",
                )
            }.getOrElse { error ->
                "xaml=${error::class.simpleName}:${error.message.orEmpty()}"
            }
        }

        val summary = if (PlatformRuntime.isWindows) {
            val packageSummary = packageState?.summary()?.let { " | $it" }.orEmpty()
            "Sample launcher completed with $activationSummary$packageSummary"
        } else {
            "Non-Windows host detected. Static sample launcher path executed without native WinUI 3 launch."
        }

        return SampleLaunchResult(
            diagnostics = listOfNotNull(
                KomSmoke.description(),
                SampleBootstrap.diagnostics(),
            ).joinToString(" | "),
            launcherSummary = summary,
            winRtSmoke = if (PlatformRuntime.isWindows) WinRtApiSmoke.run() else null,
        )
    }
}
