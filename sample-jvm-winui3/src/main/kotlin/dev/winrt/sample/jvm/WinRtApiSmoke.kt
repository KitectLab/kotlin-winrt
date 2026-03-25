package dev.winrt.sample.jvm

import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.PlatformComInterop

class WinRtApiSmokeResult(
    val runtimeClass: String,
    val activationFactoryAcquired: Boolean,
    val instanceActivated: Boolean,
)

object WinRtApiSmoke {
    private const val calendarRuntimeClass = "Windows.Globalization.Calendar"

    fun run(): WinRtApiSmokeResult {
        val factory = JvmWinRtRuntime.getActivationFactory(calendarRuntimeClass).getOrThrow()
        try {
            val hasFactory = !factory.isNull
            val instance = JvmWinRtRuntime.activateInstance(factory).getOrThrow()
            try {
                return WinRtApiSmokeResult(
                    runtimeClass = calendarRuntimeClass,
                    activationFactoryAcquired = hasFactory,
                    instanceActivated = !instance.isNull,
                )
            } finally {
                PlatformComInterop.release(instance)
            }
        } finally {
            PlatformComInterop.release(factory)
        }
    }
}
