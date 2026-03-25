package windows.data.json

import dev.winrt.core.Inspectable
import dev.winrt.core.guidOf
import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.PlatformRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class JsonObjectProjectionTest {
    @Test
    fun can_parse_json_and_read_named_string_through_binding() {
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
                    """{"name":"codex","kind":"winrt","pi":3.5,"flag":true,"nested":{"child":"value"},"items":[{"child":"a"},{"child":"b"}]}""",
                ).getOrThrow()
                try {
                    val jsonObject = JsonObject(Inspectable(instance).pointer)
                    val projected = jsonObject.asIJsonObject()
                    try {
                        assertEquals("codex", projected.getNamedString("name"))
                        assertEquals(3.5, projected.getNamedNumber("pi").value, 0.0)
                        assertTrue(projected.getNamedBoolean("flag").value)
                        val nested = projected.getNamedObject("nested")
                        try {
                            val nestedProjected = nested.asIJsonObject()
                            try {
                                assertEquals("value", nestedProjected.getNamedString("child"))
                            } finally {
                                PlatformComInterop.release(nestedProjected.pointer)
                            }
                        } finally {
                            PlatformComInterop.release(nested.pointer)
                        }
                        val items = projected.getNamedArray("items")
                        try {
                            val itemsProjected = items.asIJsonArray()
                            try {
                                assertTrue(!items.pointer.isNull)
                                assertTrue(!itemsProjected.pointer.isNull)
                            } finally {
                                PlatformComInterop.release(itemsProjected.pointer)
                            }
                        } finally {
                            PlatformComInterop.release(items.pointer)
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
