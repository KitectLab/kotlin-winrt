package windows.data.json

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtStrings
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
                    "slot 6 (valueType projected)" to classifyResult(PlatformComInterop.invokeUInt32Method(projected.pointer, 6)),
                    "slot 7 (stringify projected)" to classifyHStringResult(PlatformComInterop.invokeHStringMethod(projected.pointer, 7)),
                    "slot 8 (getString projected)" to classifyHStringResult(PlatformComInterop.invokeHStringMethod(projected.pointer, 8)),
                    "slot 9 (number projected)" to classifyResult(PlatformComInterop.invokeFloat64Method(projected.pointer, 9)),
                    "slot 10 (boolean projected)" to classifyResult(PlatformComInterop.invokeBooleanGetter(projected.pointer, 10)),
                    "slot 11 (array projected)" to classifyResult(PlatformComInterop.invokeObjectMethod(projected.pointer, 11)),
                    "slot 12 (object projected)" to classifyResult(PlatformComInterop.invokeObjectMethod(projected.pointer, 12)),
                    "slot 6 (valueType raw)" to classifyResult(PlatformComInterop.invokeUInt32Method(valuePointer, 6)),
                    "slot 7 (stringify raw)" to classifyHStringResult(PlatformComInterop.invokeHStringMethod(valuePointer, 7)),
                    "slot 8 (getString raw)" to classifyHStringResult(PlatformComInterop.invokeHStringMethod(valuePointer, 8)),
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

    @Test
    fun classify_type_specific_json_value_slots() {
        assumeTrue(PlatformRuntime.isWindows)
        assumeTrue(System.getProperty("dev.winrt.enableProbe") == "true")

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val valuePointers = parseTypedJsonValues()
            try {
                val outcomes = linkedMapOf<String, String>()
                valuePointers.forEach { (label, pointer) ->
                    val projected = IJsonValue.from(Inspectable(pointer))
                    outcomes["$label valueType"] = classifyResult(PlatformComInterop.invokeUInt32Method(projected.pointer, 6))
                    outcomes["$label stringify"] = classifyHStringResult(PlatformComInterop.invokeHStringMethod(projected.pointer, 7))
                    outcomes["$label getString"] = classifyHStringResult(PlatformComInterop.invokeHStringMethod(projected.pointer, 8))
                    outcomes["$label getNumber"] = classifyResult(PlatformComInterop.invokeFloat64Method(projected.pointer, 9))
                    outcomes["$label getBoolean"] = classifyResult(PlatformComInterop.invokeBooleanGetter(projected.pointer, 10))
                    outcomes["$label getArray"] = classifyResult(PlatformComInterop.invokeObjectMethod(projected.pointer, 11))
                    outcomes["$label getObject"] = classifyResult(PlatformComInterop.invokeObjectMethod(projected.pointer, 12))
                    PlatformComInterop.release(projected.pointer)
                }
                outcomes.values.forEach { outcome -> assertTrue(outcome.isNotBlank()) }
                fail(
                    buildString {
                        appendLine("Typed JsonValue slot outcomes:")
                        outcomes.entries.forEachIndexed { index, entry ->
                            if (index > 0) appendLine()
                            append("${entry.key}: ${entry.value}")
                        }
                    },
                )
            } finally {
                valuePointers.values.forEach(PlatformComInterop::release)
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
                try {
                    val nested = jsonObject.getNamedObject("nested")
                    try {
                        return IJsonValue.from(Inspectable(nested.pointer)).pointer
                    } finally {
                        PlatformComInterop.release(nested.pointer)
                    }
                } finally {
                }
            } finally {
                PlatformComInterop.release(instance)
            }
        } finally {
            PlatformComInterop.release(factory)
        }
    }

    private fun parseTypedJsonValues(): Map<String, dev.winrt.kom.ComPtr> {
        val factory = JvmWinRtRuntime.getActivationFactory(
            "Windows.Data.Json.JsonObject",
            guidOf("2289f159-54de-45d8-abcc-22603fa066a0"),
        ).getOrThrow()
        try {
            val instance = PlatformComInterop.invokeObjectMethodWithStringArg(
                factory,
                6,
                """{"text":"codex","num":3.5,"flag":true,"arr":[1,2],"obj":{"child":"value"}}""",
            ).getOrThrow()
            try {
                val jsonObject = JsonObject(Inspectable(instance).pointer)
                return linkedMapOf(
                    "text" to PlatformComInterop.invokeObjectMethodWithStringArg(jsonObject.pointer, 6, "text").getOrThrow(),
                    "num" to PlatformComInterop.invokeObjectMethodWithStringArg(jsonObject.pointer, 6, "num").getOrThrow(),
                    "flag" to PlatformComInterop.invokeObjectMethodWithStringArg(jsonObject.pointer, 6, "flag").getOrThrow(),
                    "arr" to PlatformComInterop.invokeObjectMethodWithStringArg(jsonObject.pointer, 6, "arr").getOrThrow(),
                    "obj" to PlatformComInterop.invokeObjectMethodWithStringArg(jsonObject.pointer, 6, "obj").getOrThrow(),
                )
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

    private fun classifyHStringResult(result: Result<dev.winrt.kom.HString>): String {
        if (result.isFailure) {
            return "failure(${result.exceptionOrNull()?.message})"
        }
        val value = result.getOrThrow()
        return try {
            "success(${WinRtStrings.toKotlin(value)})"
        } finally {
            WinRtStrings.release(value)
        }
    }
}
