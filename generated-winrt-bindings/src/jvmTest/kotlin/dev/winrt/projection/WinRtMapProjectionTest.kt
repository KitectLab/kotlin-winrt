package dev.winrt.projection

import kotlin.test.Test
import kotlin.test.assertEquals

class WinRtMapProjectionTest {
    @Test
    fun exposes_map_shape() {
        val backing = linkedMapOf("a" to 1, "b" to 2)
        val map: Map<String, Int> = WinRtMapProjection(
            sizeProvider = { backing.size },
            lookupFn = { key -> backing.getValue(key) },
            containsKeyFn = { key -> backing.containsKey(key) },
            entriesProvider = { backing.entries },
        )

        assertEquals(2, map.size)
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
    }

    @Test
    fun splits_map_into_two_views() {
        val backing = linkedMapOf("a" to 1, "b" to 2, "c" to 3)
        val map = WinRtMapProjection(
            sizeProvider = { backing.size },
            lookupFn = { key -> backing.getValue(key) },
            containsKeyFn = { key -> backing.containsKey(key) },
            entriesProvider = { backing.entries },
        )

        val (first, second) = map.split()

        assertEquals(mapOf("a" to 1), first)
        assertEquals(mapOf("b" to 2, "c" to 3), second)
    }
}
