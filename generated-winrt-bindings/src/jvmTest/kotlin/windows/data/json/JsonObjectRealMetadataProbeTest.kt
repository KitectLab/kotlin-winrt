package windows.data.json

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtStrings
import dev.winrt.core.guidOf
import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.KomException
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.PlatformRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Test

class JsonObjectRealMetadataProbeTest {
    @Ignore("Experimental probe for comparing verified and real metadata JSON object slot surfaces")
    @Test
    fun compare_verified_and_real_metadata_slots_for_json_object() {
        assumeTrue(PlatformRuntime.isWindows)

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val instance = parseJsonObject("""{"name":"codex","pi":3.5,"flag":true}""")
            try {
                val verifiedName = readNamedString(instance, 10, "name")
                val metadataName = readNamedString(instance, 17, "name")
                assertEquals("codex", verifiedName)
                assertNotEquals(
                    "Real metadata slot probe unexpectedly matches the currently verified surface; update checked-in JSON bindings instead of keeping the split tests.",
                    verifiedName,
                    metadataName,
                )
            } finally {
                PlatformComInterop.release(instance)
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

    @Ignore("Experimental probe for real IJsonObject metadata slots; enable manually while aligning runtime surface")
    @Test
    fun probe_real_metadata_slots_for_json_object() {
        assumeTrue(PlatformRuntime.isWindows)

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val instance = parseJsonObject(
                """{"name":"codex","pi":3.5,"flag":true,"nested":{"child":"value"},"items":[{"child":"a"},{"child":"b"}]}""",
            )
            try {
                assertEquals("codex", readNamedString(instance, 17, "name"))
                assertEquals(3.5, PlatformComInterop.invokeFloat64MethodWithStringArg(instance, 18, "pi").getOrThrow(), 0.0)
                assertTrue(PlatformComInterop.invokeBooleanMethodWithStringArg(instance, 19, "flag").getOrThrow())

                val nested = PlatformComInterop.invokeObjectMethodWithStringArg(instance, 15, "nested").getOrThrow()
                try {
                    assertTrue(!nested.isNull)
                } finally {
                    PlatformComInterop.release(nested)
                }

                val items = PlatformComInterop.invokeObjectMethodWithStringArg(instance, 16, "items").getOrThrow()
                try {
                    assertTrue(!items.isNull)
                } finally {
                    PlatformComInterop.release(items)
                }
            } finally {
                PlatformComInterop.release(instance)
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
    fun classify_real_metadata_slot_outcomes_for_json_object() {
        assumeTrue(PlatformRuntime.isWindows)
        assumeTrue(System.getProperty("dev.winrt.enableProbe") == "true")

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val instance = parseJsonObject(
                """{"name":"codex","pi":3.5,"flag":true,"nested":{"child":"value"},"items":[{"child":"a"},{"child":"b"}]}""",
            )
            try {
                val nameOutcome = readNamedStringOutcome(instance, 17, "name")
                val numberOutcome = classifyResult(PlatformComInterop.invokeFloat64MethodWithStringArg(instance, 18, "pi"))
                val booleanOutcome = classifyResult(PlatformComInterop.invokeBooleanMethodWithStringArg(instance, 19, "flag"))
                val objectOutcome = classifyResult(PlatformComInterop.invokeObjectMethodWithStringArg(instance, 15, "nested"))
                val arrayOutcome = classifyResult(PlatformComInterop.invokeObjectMethodWithStringArg(instance, 16, "items"))

                assertTrue(nameOutcome.isNotBlank())
                assertTrue(numberOutcome.isNotBlank())
                assertTrue(booleanOutcome.isNotBlank())
                assertTrue(objectOutcome.isNotBlank())
                assertTrue(arrayOutcome.isNotBlank())
                fail(
                    buildString {
                        appendLine("Real metadata slot outcomes:")
                        appendLine("slot 17 (String): $nameOutcome")
                        appendLine("slot 18 (Float64): $numberOutcome")
                        appendLine("slot 19 (Boolean): $booleanOutcome")
                        appendLine("slot 15 (Object): $objectOutcome")
                        append("slot 16 (Array): $arrayOutcome")
                    },
                )
            } finally {
                PlatformComInterop.release(instance)
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

    private fun parseJsonObject(json: String) : dev.winrt.kom.ComPtr {
        val factory = JvmWinRtRuntime.getActivationFactory(
            "Windows.Data.Json.JsonObject",
            guidOf("2289f159-54de-45d8-abcc-22603fa066a0"),
        ).getOrThrow()
        try {
            return PlatformComInterop.invokeObjectMethodWithStringArg(factory, 6, json).getOrThrow()
        } finally {
            PlatformComInterop.release(factory)
        }
    }

    private fun readNamedString(instance: dev.winrt.kom.ComPtr, slot: Int, name: String): String {
        val inspectable = Inspectable(instance)
        val nameValue = PlatformComInterop.invokeHStringMethodWithStringArg(inspectable.pointer, slot, name).getOrThrow()
        return try {
            WinRtStrings.toKotlin(nameValue)
        } finally {
            WinRtStrings.release(nameValue)
        }
    }

    private fun readNamedStringOutcome(instance: dev.winrt.kom.ComPtr, slot: Int, name: String): String {
        val inspectable = Inspectable(instance)
        val result = PlatformComInterop.invokeHStringMethodWithStringArg(inspectable.pointer, slot, name)
        if (result.isFailure) {
            return classifyFailure(result.exceptionOrNull())
        }

        val hString = result.getOrThrow()
        return try {
            """success("${WinRtStrings.toKotlin(hString)}")"""
        } finally {
            WinRtStrings.release(hString)
        }
    }

    private fun <T> classifyResult(result: Result<T>): String {
        if (result.isFailure) {
            return classifyFailure(result.exceptionOrNull())
        }
        val value = result.getOrThrow()
        return when (value) {
            is dev.winrt.kom.ComPtr -> "success(pointer=${value.value.rawValue})"
            else -> "success($value)"
        }
    }

    private fun classifyFailure(throwable: Throwable?): String {
        if (throwable == null) {
            return "failure(unknown)"
        }
        if (throwable is KomException) {
            return "failure(${throwable.message})"
        }
        return "failure(${throwable::class.simpleName}: ${throwable.message})"
    }
}
