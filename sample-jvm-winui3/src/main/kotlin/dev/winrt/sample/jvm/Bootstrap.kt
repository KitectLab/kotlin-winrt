package dev.winrt.sample.jvm

import dev.winrt.core.ActivationFactoryProvider
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtObject
import dev.winrt.core.WinRtRuntime
import dev.winrt.kom.ComPtr
import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformRuntime

object SampleBootstrap {
    private var comInitialized = false

    fun configure() {
        if (PlatformRuntime.isWindows) {
            val result = JvmComRuntime.initializeMultithreaded()
            result.requireSuccessUnlessChangedMode("CoInitializeEx")
            comInitialized = result.isSuccess
        }

        WinRtRuntime.activationFactoryProvider = object : ActivationFactoryProvider {
            override fun <T : WinRtObject> activate(classId: RuntimeClassId, constructor: (ComPtr) -> T): Result<T> {
                if (!PlatformRuntime.isWindows) {
                    return Result.success(constructor(ComPtr.NULL))
                }

                return Result.success(constructor(ComPtr.NULL))
            }
        }
    }

    fun shutdown() {
        if (comInitialized) {
            JvmComRuntime.uninitialize()
            comInitialized = false
        }
    }

    private fun dev.winrt.kom.HResult.requireSuccessUnlessChangedMode(operation: String) {
        if (this != KnownHResults.RPC_E_CHANGED_MODE) {
            requireSuccess(operation)
        }
    }
}
