package windows.data.json

import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32
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
                    """{"name":"codex","kind":"winrt","pi":3.5,"flag":true,"nested":{"child":"value"},"items":[{"child":"a"},{"child":"b"}],"scalarItems":["alpha",3.5,true]}""",
                ).getOrThrow()
                try {
                    val jsonObject = JsonObject(Inspectable(instance).pointer)
                    val projected = jsonObject.asIJsonObject()
                    try {
                        assertEquals("codex", projected.getNamedString("name"))
                        assertEquals(3.5, projected.getNamedNumber("pi").value, 0.0)
                        assertTrue(projected.getNamedBoolean("flag").value)
                        val textValue = projected.getNamedValue("name")
                        try {
                            assertEquals(JsonValueType.String, textValue.valueType)
                            assertEquals("\"codex\"", textValue.stringify())
                            assertEquals("codex", textValue.getString())
                        } finally {
                            PlatformComInterop.release(textValue.pointer)
                        }
                        val numberValue = projected.getNamedValue("pi")
                        try {
                            assertEquals(JsonValueType.Number, numberValue.valueType)
                            assertEquals("3.5", numberValue.stringify())
                            assertEquals(3.5, numberValue.getNumber().value, 0.0)
                        } finally {
                            PlatformComInterop.release(numberValue.pointer)
                        }
                        val booleanValue = projected.getNamedValue("flag")
                        try {
                            assertEquals(JsonValueType.Boolean, booleanValue.valueType)
                            assertEquals("true", booleanValue.stringify())
                            assertTrue(booleanValue.getBoolean().value)
                        } finally {
                            PlatformComInterop.release(booleanValue.pointer)
                        }
                        val nested = projected.getNamedObject("nested")
                        try {
                            val nestedValue = IJsonValue.from(Inspectable(nested.pointer))
                            try {
                                assertEquals(JsonValueType.Object, nestedValue.valueType)
                                assertEquals("""{"child":"value"}""", nestedValue.stringify())
                                val nestedObject = nestedValue.getObject()
                                try {
                                    assertTrue(!nestedObject.pointer.isNull)
                                } finally {
                                    PlatformComInterop.release(nestedObject.pointer)
                                }
                            } finally {
                                PlatformComInterop.release(nestedValue.pointer)
                            }
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
                            val arrayValue = projected.getNamedValue("items")
                            try {
                                assertEquals(JsonValueType.Array, arrayValue.valueType)
                                assertEquals("[{\"child\":\"a\"},{\"child\":\"b\"}]", arrayValue.stringify())
                                val nestedArray = arrayValue.getArray()
                                try {
                                    assertTrue(!nestedArray.pointer.isNull)
                                } finally {
                                    PlatformComInterop.release(nestedArray.pointer)
                                }
                            } finally {
                                PlatformComInterop.release(arrayValue.pointer)
                            }
                            val itemsProjected = items.asIJsonArray()
                            try {
                                assertTrue(!items.pointer.isNull)
                                assertTrue(!itemsProjected.pointer.isNull)
                                val firstItem = itemsProjected.getObjectAt(UInt32(0u))
                                try {
                                    val firstItemProjected = firstItem.asIJsonObject()
                                    try {
                                        assertEquals("a", firstItemProjected.getNamedString("child"))
                                    } finally {
                                        PlatformComInterop.release(firstItemProjected.pointer)
                                    }
                                } finally {
                                    PlatformComInterop.release(firstItem.pointer)
                                }
                            } finally {
                                PlatformComInterop.release(itemsProjected.pointer)
                            }
                        } finally {
                            PlatformComInterop.release(items.pointer)
                        }
                        val scalarItems = projected.getNamedArray("scalarItems")
                        try {
                            val scalarItemsProjected = scalarItems.asIJsonArray()
                            try {
                                assertEquals("alpha", scalarItemsProjected.getStringAt(UInt32(0u)))
                                assertEquals(3.5, scalarItemsProjected.getNumberAt(UInt32(1u)).value, 0.0)
                                assertTrue(scalarItemsProjected.getBooleanAt(UInt32(2u)).value)
                            } finally {
                                PlatformComInterop.release(scalarItemsProjected.pointer)
                            }
                        } finally {
                            PlatformComInterop.release(scalarItems.pointer)
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
