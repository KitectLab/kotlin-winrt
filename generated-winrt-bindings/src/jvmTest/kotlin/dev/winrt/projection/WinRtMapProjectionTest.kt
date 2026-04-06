package dev.winrt.projection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun splits_map_into_sorted_partitions_like_cswinrt() {
        val backing = linkedMapOf("c" to 3, "a" to 1, "b" to 2)
        val map = WinRtMapProjection(
            sizeProvider = { backing.size },
            lookupFn = { key -> backing.getValue(key) },
            containsKeyFn = { key -> backing.containsKey(key) },
            entriesProvider = { backing.entries },
        )

        val (first, second) = map.split()

        assertEquals(listOf("a", "b"), first?.keys?.toList())
        assertEquals(listOf("c"), second?.keys?.toList())
        assertEquals(mapOf("a" to 1, "b" to 2), first)
        assertEquals(mapOf("c" to 3), second)
    }

    @Test
    fun split_returns_null_partitions_for_single_item_map() {
        val backing = linkedMapOf("a" to 1)
        val map = WinRtMapProjection(
            sizeProvider = { backing.size },
            lookupFn = { key -> backing.getValue(key) },
            containsKeyFn = { key -> backing.containsKey(key) },
            entriesProvider = { backing.entries },
        )

        val (first, second) = map.split()

        assertNull(first)
        assertNull(second)
    }
}
