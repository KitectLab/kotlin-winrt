package windows.data.json

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtStrings
import dev.winrt.core.guidOf
import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.PlatformRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Test

class JsonObjectRealMetadataProbeTest {
    @Ignore("Experimental probe for real IJsonObject metadata slots; enable manually while aligning runtime surface")
    @Test
    fun probe_real_metadata_slots_for_json_object() {
        assumeTrue(PlatformRuntime.isWindows)

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val factory = JvmWinRtRuntime.getActivationFactory(
                "Windows.Data.Json.JsonObject",
                guidOf("2289f159-54de-45d8-abcc-22603fa066a0"),
            ).getOrThrow()
            try {
                val instance = PlatformComInterop.invokeObjectMethodWithStringArg(
                    factory,
                    6,
                    """{"name":"codex","pi":3.5,"flag":true,"nested":{"child":"value"},"items":[{"child":"a"},{"child":"b"}]}""",
                ).getOrThrow()
                try {
                    val inspectable = Inspectable(instance)

                    val nameValue = PlatformComInterop.invokeHStringMethodWithStringArg(inspectable.pointer, 17, "name").getOrThrow()
                    try {
                        assertEquals("codex", WinRtStrings.toKotlin(nameValue))
                    } finally {
                        WinRtStrings.release(nameValue)
                    }

                    assertEquals(3.5, PlatformComInterop.invokeFloat64MethodWithStringArg(inspectable.pointer, 18, "pi").getOrThrow(), 0.0)
                    assertTrue(PlatformComInterop.invokeBooleanMethodWithStringArg(inspectable.pointer, 19, "flag").getOrThrow())

                    val nested = PlatformComInterop.invokeObjectMethodWithStringArg(inspectable.pointer, 15, "nested").getOrThrow()
                    try {
                        assertTrue(!nested.isNull)
                    } finally {
                        PlatformComInterop.release(nested)
                    }

                    val items = PlatformComInterop.invokeObjectMethodWithStringArg(inspectable.pointer, 16, "items").getOrThrow()
                    try {
                        assertTrue(!items.isNull)
                    } finally {
                        PlatformComInterop.release(items)
                    }
                } finally {
                    PlatformComInterop.release(instance)
                }
            } finally {
                PlatformComInterop.release(factory)
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
}
