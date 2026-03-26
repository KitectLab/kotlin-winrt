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
        val packageState = WindowsAppSdkEnvironment.detect()
        val activationSummary = when {
            !PlatformRuntime.isWindows -> "xaml=skipped(non-windows)"
            !SampleBootstrap.isWindowsAppSdkReady() -> when (packageState?.readiness()) {
                WindowsAppSdkEnvironment.Readiness.MissingFramework -> "xaml=blocked(missing-framework-package)"
                WindowsAppSdkEnvironment.Readiness.MissingMain -> "xaml=blocked(missing-main-package)"
                WindowsAppSdkEnvironment.Readiness.MissingDdlm -> "xaml=blocked(missing-ddlm-package)"
                WindowsAppSdkEnvironment.Readiness.MissingSingleton -> "xaml=blocked(missing-singleton-package)"
                else -> "xaml=skipped(bootstrap-not-ready)"
            }
            else -> runCatching {
                val window = Window.activateInstance()
                window.title = "kotlin-winrt sample"
                window.activate()
                WindowsMessageLoop.run()
                "xaml=window-activated"
            }.getOrElse { error ->
                "xaml=${error::class.simpleName}:${error.message.orEmpty()}"
            }
        }

        val summary = if (PlatformRuntime.isWindows) {
            "Sample launcher completed with $activationSummary"
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
