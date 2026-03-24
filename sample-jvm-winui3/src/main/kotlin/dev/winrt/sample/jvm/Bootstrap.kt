package dev.winrt.sample.jvm

import dev.winrt.core.ActivationFactoryProvider
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtObject
import dev.winrt.core.WinRtRuntime
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformRuntime

object SampleBootstrap {
    fun configure() {
        WinRtRuntime.activationFactoryProvider = object : ActivationFactoryProvider {
            override fun <T : WinRtObject> activate(classId: RuntimeClassId, constructor: (ComPtr) -> T): Result<T> {
                if (!PlatformRuntime.isWindows) {
                    return Result.success(constructor(ComPtr.NULL))
                }

                return Result.success(constructor(ComPtr.NULL))
            }
        }
    }
}
