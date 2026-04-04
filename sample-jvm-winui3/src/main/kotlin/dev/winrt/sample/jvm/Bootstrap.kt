package dev.winrt.sample.jvm

import dev.winrt.core.ActivationFactoryProvider
import dev.winrt.core.WinRtInterfaceMetadata
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
    private val defaultInterfaceIidCache = mutableMapOf<String, Guid>()
    private var activationContext: WindowsActivationContext.ActivationContext? = null
    private var bootstrapLibrary: WindowsAppSdkBootstrap.BootstrapLibrary? = null
    private var bootstrapDiagnostics: String? = null
    private var activationContextDiagnostics: String? = null
    private var comInitialized = false
    private var winRtInitialized = false
    var launcher: WinUiApplicationLauncher = DefaultWinUiApplicationLauncher

    fun configure() {
        if (PlatformRuntime.isWindows) {
            if (activationContext == null && bootstrapLibrary == null) {
                val runtimeRoot = System.getProperty("dev.winrt.windowsAppSdkRoot")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(java.nio.file.Path::of)
                if (runtimeRoot != null) {
                    val activationContextResult = WindowsActivationContext.activateConfigured(runtimeRoot)
                    activationContextResult.onSuccess { context ->
                        if (context != null) {
                            activationContext = context
                            activationContextDiagnostics = "activation-context=self-contained"
                        }
                    }.onFailure { error ->
                        activationContextDiagnostics = "activation-context=${error.message}"
                    }
                }
            }

            val comResult = JvmComRuntime.initializeSingleThreaded()
            comResult.requireSuccessUnlessChangedMode("CoInitializeEx")
            comInitialized = comResult.isSuccess

            if (activationContext == null) {
                val bootstrapResult = WindowsAppSdkBootstrap.initialize()
                bootstrapResult.onSuccess { library ->
                    bootstrapLibrary = library
                    bootstrapDiagnostics = listOfNotNull(
                        activationContextDiagnostics,
                        "bootstrap=initialized",
                    ).joinToString(" | ")
                }.onFailure { error ->
                    val packageSummary = WindowsAppSdkEnvironment.detect()?.summary()
                    bootstrapDiagnostics = listOfNotNull(
                        activationContextDiagnostics,
                        "bootstrap=${error.message}",
                        packageSummary,
                    ).joinToString(" | ")
                }
            } else {
                bootstrapDiagnostics = activationContextDiagnostics
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
                            val projectedInstance = metadata.defaultInterfaceName
                                ?.let(::defaultInterfaceIidOrNull)
                                ?.let { iid -> PlatformComInterop.queryInterface(instance, iid).getOrThrow() }
                                ?: instance
                            try {
                                constructor(projectedInstance)
                            } finally {
                                if (projectedInstance != instance) {
                                    PlatformComInterop.release(instance)
                                }
                            }
                        } finally {
                            PlatformComInterop.release(factory)
                        }
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
        activationContext?.close()
        activationContext = null
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

    fun isWindowsAppSdkReady(): Boolean = activationContext != null || bootstrapLibrary != null

    private fun defaultInterfaceIidOrNull(qualifiedName: String): Guid? =
        runCatching {
            defaultInterfaceIidCache.getOrPut(qualifiedName) {
                val jvmClassName = buildString {
                    append(qualifiedName.substringBeforeLast('.').lowercase())
                    append('.')
                    append(qualifiedName.substringAfterLast('.'))
                }
                val companion = Class.forName(jvmClassName).getDeclaredField("Companion").get(null)
                val metadata = companion as? WinRtInterfaceMetadata
                    ?: error("Companion for $qualifiedName does not implement WinRtInterfaceMetadata")
                metadata.iid
            }
        }.getOrNull()
}
