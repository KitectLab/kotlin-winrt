package windows.data.json

import dev.winrt.core.Inspectable
import dev.winrt.core.guidOf
import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.PlatformRuntime
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

class JsonValueRealMetadataProbeTest {
    @Test
    fun classify_json_value_slots() {
        assumeTrue(PlatformRuntime.isWindows)
        assumeTrue(System.getProperty("dev.winrt.enableProbe") == "true")

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val valuePointer = parseJsonValue()
            try {
                val projected = IJsonValue.from(Inspectable(valuePointer))
                val outcomes = linkedMapOf(
                    "slot 9 (number projected)" to classifyResult(PlatformComInterop.invokeFloat64Method(projected.pointer, 9)),
                    "slot 10 (boolean projected)" to classifyResult(PlatformComInterop.invokeBooleanGetter(projected.pointer, 10)),
                    "slot 11 (array projected)" to classifyResult(PlatformComInterop.invokeObjectMethod(projected.pointer, 11)),
                    "slot 12 (object projected)" to classifyResult(PlatformComInterop.invokeObjectMethod(projected.pointer, 12)),
                    "slot 9 (number raw)" to classifyResult(PlatformComInterop.invokeFloat64Method(valuePointer, 9)),
                    "slot 10 (boolean raw)" to classifyResult(PlatformComInterop.invokeBooleanGetter(valuePointer, 10)),
                    "slot 11 (array raw)" to classifyResult(PlatformComInterop.invokeObjectMethod(valuePointer, 11)),
                    "slot 12 (object raw)" to classifyResult(PlatformComInterop.invokeObjectMethod(valuePointer, 12)),
                )
                outcomes.values.forEach { outcome -> assertTrue(outcome.isNotBlank()) }
                fail(
                    buildString {
                        appendLine("JsonValue slot outcomes:")
                        outcomes.entries.forEachIndexed { index, entry ->
                            if (index > 0) appendLine()
                            append("${entry.key}: ${entry.value}")
                        }
                    },
                )
            } finally {
                PlatformComInterop.release(valuePointer)
            }
        } finally {
            if (shouldUninitializeRo) {
                JvmWinRtRuntime.uninitialize()
            }
            if (shouldUninitializeCom && roResult != KnownHResults.RPC_E_CHANGED_MODE) {
                JvmComRuntime.uninitialize()
            }
        }
    }

    private fun parseJsonValue(): dev.winrt.kom.ComPtr {
        val factory = JvmWinRtRuntime.getActivationFactory(
            "Windows.Data.Json.JsonObject",
            guidOf("2289f159-54de-45d8-abcc-22603fa066a0"),
        ).getOrThrow()
        try {
            val instance = PlatformComInterop.invokeObjectMethodWithStringArg(
                factory,
                6,
                """{"nested":{"child":"value"},"items":[{"child":"a"},{"child":"b"}],"pi":3.5,"flag":true}""",
            ).getOrThrow()
            try {
                val jsonObject = JsonObject(Inspectable(instance).pointer)
                val projected = jsonObject.asIJsonObject()
                try {
                    val nested = projected.getNamedObject("nested")
                    try {
                        return IJsonValue.from(Inspectable(nested.pointer)).pointer
                    } finally {
                        PlatformComInterop.release(nested.pointer)
                    }
                } finally {
                    PlatformComInterop.release(projected.pointer)
                }
            } finally {
                PlatformComInterop.release(instance)
            }
        } finally {
            PlatformComInterop.release(factory)
        }
    }

    private fun <T> classifyResult(result: Result<T>): String {
        if (result.isFailure) {
            return "failure(${result.exceptionOrNull()?.message})"
        }
        val value = result.getOrThrow()
        return when (value) {
            is dev.winrt.kom.ComPtr -> "success(pointer=${value.value.rawValue})"
            else -> "success($value)"
        }
    }
}
