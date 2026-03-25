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

class JsonArrayRealMetadataProbeTest {
    @Test
    fun classify_real_metadata_slots_for_json_array() {
        assumeTrue(PlatformRuntime.isWindows)
        assumeTrue(System.getProperty("dev.winrt.enableProbe") == "true")

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val arrayPointer = parseJsonArray()
            try {
                val projected = IJsonArray.from(Inspectable(arrayPointer))
                val outcomes = linkedMapOf(
                    "slot 13 (projected)" to classifyObjectAt(projected.pointer, 13u),
                    "slot 6 (projected)" to classifyObjectAt(projected.pointer, 6u),
                    "slot 13 (raw)" to classifyObjectAt(arrayPointer, 13u),
                    "slot 6 (raw)" to classifyObjectAt(arrayPointer, 6u),
                )
                outcomes.values.forEach { outcome -> assertTrue(outcome.isNotBlank()) }
                fail(
                    buildString {
                        appendLine("JsonArray slot outcomes:")
                        outcomes.entries.forEachIndexed { index, entry ->
                            if (index > 0) appendLine()
                            append("${entry.key}: ${entry.value}")
                        }
                    },
                )
            } finally {
                PlatformComInterop.release(arrayPointer)
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
    fun classify_type_specific_slots_for_json_array() {
        assumeTrue(PlatformRuntime.isWindows)
        assumeTrue(System.getProperty("dev.winrt.enableProbe") == "true")

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val arrayPointer = parseTypedJsonArray()
            try {
                val projected = IJsonArray.from(Inspectable(arrayPointer))
                val outcomes = linkedMapOf(
                    "string getStringAt" to classifyHStringAt(projected.pointer, 8, 0u),
                    "number getNumberAt" to classifyFloat64At(projected.pointer, 9, 1u),
                    "boolean getBooleanAt" to classifyBooleanAt(projected.pointer, 10, 2u),
                    "object getObjectAt" to classifyObjectAt(projected.pointer, 6u, 3u),
                    "array getArrayAt" to classifyObjectAt(projected.pointer, 7u, 4u),
                )
                outcomes.values.forEach { outcome -> assertTrue(outcome.isNotBlank()) }
                fail(
                    buildString {
                        appendLine("Typed JsonArray slot outcomes:")
                        outcomes.entries.forEachIndexed { index, entry ->
                            if (index > 0) appendLine()
                            append("${entry.key}: ${entry.value}")
                        }
                    },
                )
            } finally {
                PlatformComInterop.release(arrayPointer)
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

    private fun parseJsonArray(): dev.winrt.kom.ComPtr {
        val factory = JvmWinRtRuntime.getActivationFactory(
            "Windows.Data.Json.JsonObject",
            guidOf("2289f159-54de-45d8-abcc-22603fa066a0"),
        ).getOrThrow()
        try {
            val instance = PlatformComInterop.invokeObjectMethodWithStringArg(
                factory,
                6,
                """{"items":[{"child":"a"},{"child":"b"}]}""",
            ).getOrThrow()
            try {
                val jsonObject = JsonObject(Inspectable(instance).pointer)
                val projected = jsonObject.asIJsonObject()
                try {
                    return projected.getNamedArray("items").pointer
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

    private fun parseTypedJsonArray(): dev.winrt.kom.ComPtr {
        val factory = JvmWinRtRuntime.getActivationFactory(
            "Windows.Data.Json.JsonObject",
            guidOf("2289f159-54de-45d8-abcc-22603fa066a0"),
        ).getOrThrow()
        try {
            val instance = PlatformComInterop.invokeObjectMethodWithStringArg(
                factory,
                6,
                """{"items":["alpha",3.5,true,{"child":"a"},[1,2]]}""",
            ).getOrThrow()
            try {
                val jsonObject = JsonObject(Inspectable(instance).pointer)
                val projected = jsonObject.asIJsonObject()
                try {
                    return projected.getNamedArray("items").pointer
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

    private fun classifyObjectAt(pointer: dev.winrt.kom.ComPtr, slot: UInt, index: UInt = 0u): String {
        val result = PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, slot.toInt(), index)
        if (result.isFailure) {
            return "failure(${result.exceptionOrNull()?.message})"
        }
        val value = result.getOrThrow()
        return "success(pointer=${value.value.rawValue})"
    }

    private fun classifyHStringAt(pointer: dev.winrt.kom.ComPtr, slot: Int, index: UInt): String {
        val result = PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, slot, index)
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

    private fun classifyFloat64At(pointer: dev.winrt.kom.ComPtr, slot: Int, index: UInt): String {
        val result = PlatformComInterop.invokeFloat64MethodWithUInt32Arg(pointer, slot, index)
        if (result.isFailure) {
            return "failure(${result.exceptionOrNull()?.message})"
        }
        return "success(${result.getOrThrow()})"
    }

    private fun classifyBooleanAt(pointer: dev.winrt.kom.ComPtr, slot: Int, index: UInt): String {
        val result = PlatformComInterop.invokeBooleanMethodWithUInt32Arg(pointer, slot, index)
        if (result.isFailure) {
            return "failure(${result.exceptionOrNull()?.message})"
        }
        return "success(${result.getOrThrow()})"
    }
}
