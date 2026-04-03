package dev.winrt.projection

import kotlin.test.Test
import kotlin.test.assertEquals

class WinRtMapProjectionSnapshotTest {
    @Test
    fun map_projection_snapshot_is_stable() {
        val map = WinRtMapProjection(
            sizeProvider = { 2 },
            lookupFn = { key: String -> key.uppercase() },
            containsKeyFn = { key: String -> key == "a" || key == "b" },
            entriesProvider = {
                listOf(
                    object : Map.Entry<String, String> {
                        override val key: String = "a"
                        override val value: String = "A"
                    },
                    object : Map.Entry<String, String> {
                        override val key: String = "b"
                        override val value: String = "B"
                    },
                )
            },
        )

        assertEquals(mapOf("a" to "A", "b" to "B"), map.snapshot())
    }

    @Test
    fun mutable_map_projection_snapshot_is_stable() {
        val map = WinRtMutableMapProjection(
            sizeProvider = { 2 },
            lookupFn = { key: String -> key.uppercase() },
            containsKeyFn = { key: String -> key == "a" || key == "b" },
            putValueFn = { _, _ -> true },
            removeKeyFn = { _ -> true },
            clearerFn = { },
            entriesProvider = {
                listOf(
                    object : Map.Entry<String, String> {
                        override val key: String = "a"
                        override val value: String = "A"
                    },
                    object : Map.Entry<String, String> {
                        override val key: String = "b"
                        override val value: String = "B"
                    },
                )
            },
        )

        assertEquals(linkedMapOf("a" to "A", "b" to "B"), map.snapshot())
    }
}
