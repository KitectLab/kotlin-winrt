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
}
