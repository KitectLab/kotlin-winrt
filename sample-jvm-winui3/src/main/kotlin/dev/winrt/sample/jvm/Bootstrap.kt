package dev.winrt.sample.jvm

import dev.winrt.core.ActivationFactoryProvider
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.WinRtObject
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.PlatformRuntime

object SampleBootstrap {
    private val iidIActivationFactory: Guid = guidOf("00000035-0000-0000-c000-000000000046")
    private var bootstrapLibrary: WindowsAppSdkBootstrap.BootstrapLibrary? = null
    private var bootstrapDiagnostics: String? = null
    private var comInitialized = false
    private var winRtInitialized = false
    var launcher: WinUiApplicationLauncher = DefaultWinUiApplicationLauncher

    fun configure() {
        if (PlatformRuntime.isWindows) {
            val comResult = JvmComRuntime.initializeSingleThreaded()
            comResult.requireSuccessUnlessChangedMode("CoInitializeEx")
            comInitialized = comResult.isSuccess

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

            val winRtResult = JvmWinRtRuntime.initializeSingleThreaded()
            winRtResult.requireSuccessUnlessChangedMode("RoInitialize")
            winRtInitialized = winRtResult.isSuccess
        }

        WinRtRuntime.activationFactoryProvider = object : ActivationFactoryProvider {
            override fun <T : WinRtObject> activate(metadata: WinRtRuntimeClassMetadata, constructor: (ComPtr) -> T): Result<T> {
                if (!PlatformRuntime.isWindows) {
                    return Result.success(constructor(ComPtr.NULL))
                }

                return getActivationFactory(metadata, iidIActivationFactory)
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

            override fun getActivationFactory(metadata: WinRtRuntimeClassMetadata, iid: dev.winrt.kom.Guid): Result<ComPtr> {
                if (!PlatformRuntime.isWindows) {
                    return Result.success(ComPtr.NULL)
                }

                return JvmWinRtRuntime.getActivationFactory(metadata.classId.qualifiedName, iid)
            }
        }
    }

    fun shutdown() {
        WinUiApplicationStart.shutdown()
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
