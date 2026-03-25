package dev.winrt.sample.jvm

import dev.winrt.core.guidOf
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.PlatformHStringBridge
import dev.winrt.kom.PlatformComInterop

class WinRtApiSmokeResult(
    val runtimeClass: String,
    val parsedName: String,
)

object WinRtApiSmoke {
    private const val jsonObjectRuntimeClass = "Windows.Data.Json.JsonObject"
    private val iidIJsonObjectStatics = guidOf("2289f159-54de-45d8-abcc-22603fa066a0")

    fun run(): WinRtApiSmokeResult {
        val factory = JvmWinRtRuntime.getActivationFactory(jsonObjectRuntimeClass, iidIJsonObjectStatics).getOrThrow()
        try {
            val instance = PlatformComInterop.invokeObjectMethodWithStringArg(
                factory,
                6,
                """{"name":"codex","kind":"winrt"}""",
            ).getOrThrow()
            try {
                val value = PlatformComInterop.invokeHStringMethodWithStringArg(instance, 10, "name").getOrThrow()
                return WinRtApiSmokeResult(
                    runtimeClass = jsonObjectRuntimeClass,
                    parsedName = try {
                        PlatformHStringBridge.toKotlinString(value)
                    } finally {
                        PlatformHStringBridge.release(value)
                    },
                )
            } finally {
                PlatformComInterop.release(instance)
            }
        } finally {
            PlatformComInterop.release(factory)
        }
    }
}
