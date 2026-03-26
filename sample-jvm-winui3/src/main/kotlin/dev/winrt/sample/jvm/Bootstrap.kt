package dev.winrt.sample.jvm

import dev.winrt.core.ActivationFactoryProvider
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.WinRtObject
import dev.winrt.core.WinRtRuntime
import dev.winrt.kom.ComPtr
import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.PlatformRuntime

object SampleBootstrap {
    private var bootstrapLibrary: WindowsAppSdkBootstrap.BootstrapLibrary? = null
    private var bootstrapDiagnostics: String? = null
    private var comInitialized = false
    private var winRtInitialized = false
    var launcher: WinUiApplicationLauncher = DefaultWinUiApplicationLauncher

    fun configure() {
        if (PlatformRuntime.isWindows) {
            val bootstrapResult = WindowsAppSdkBootstrap.initialize()
            bootstrapResult.onSuccess { library ->
                bootstrapLibrary = library
                bootstrapDiagnostics = "bootstrap=initialized"
            }.onFailure { error ->
                val packageSummary = WindowsAppSdkEnvironment.detect()?.summary()
                bootstrapDiagnostics = listOfNotNull(
                    "bootstrap=${error.message}",
                    packageSummary,
                ).joinToString(" | ")
            }

            val comResult = JvmComRuntime.initializeMultithreaded()
            comResult.requireSuccessUnlessChangedMode("CoInitializeEx")
            comInitialized = comResult.isSuccess

            val winRtResult = JvmWinRtRuntime.initializeMultithreaded()
            winRtResult.requireSuccessUnlessChangedMode("RoInitialize")
            winRtInitialized = winRtResult.isSuccess
        }

        WinRtRuntime.activationFactoryProvider = object : ActivationFactoryProvider {
            override fun <T : WinRtObject> activate(metadata: WinRtRuntimeClassMetadata, constructor: (ComPtr) -> T): Result<T> {
                if (!PlatformRuntime.isWindows) {
                    return Result.success(constructor(ComPtr.NULL))
                }

                val qualifiedName = metadata.classId.qualifiedName
                return JvmWinRtRuntime.getActivationFactory(qualifiedName)
                    .mapCatching { factory ->
                        try {
                            val instance = JvmWinRtRuntime.activateInstance(factory).getOrThrow()
                            constructor(instance)
                        } finally {
                            PlatformComInterop.release(factory)
                        }
                    }
                    .recoverCatching {
                        constructor(ComPtr.NULL)
                    }
            }
        }
    }

    fun shutdown() {
        if (winRtInitialized) {
            JvmWinRtRuntime.uninitialize()
            winRtInitialized = false
        }
        if (comInitialized) {
            JvmComRuntime.uninitialize()
            comInitialized = false
        }
        bootstrapLibrary?.let { library ->
            WindowsAppSdkBootstrap.shutdown(library)
            bootstrapLibrary = null
        }
    }

    private fun dev.winrt.kom.HResult.requireSuccessUnlessChangedMode(operation: String) {
        if (this != KnownHResults.RPC_E_CHANGED_MODE) {
            requireSuccess(operation)
        }
    }

    fun launch(): SampleLaunchResult = launcher.launch()

    fun diagnostics(): String? = bootstrapDiagnostics

    fun isWindowsAppSdkReady(): Boolean = bootstrapLibrary != null
}
