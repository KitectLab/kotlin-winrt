package dev.winrt.projection

import kotlin.test.Test
import kotlin.test.assertEquals

class WinRtMutableMapProjectionTest {
    @Test
    fun supports_put_remove_and_clear() {
        val backing = linkedMapOf("a" to 1, "b" to 2)
        val map = WinRtMutableMapProjection(
            sizeProvider = { backing.size },
            lookupFn = { key -> backing.getValue(key) },
            containsKeyFn = { key -> backing.containsKey(key) },
            putValueFn = { key, value ->
                backing[key] = value
                true
            },
            removeKeyFn = { key ->
                backing.remove(key) != null
            },
            clearerFn = { backing.clear() },
            entriesProvider = { backing.entries },
        )

        assertEquals(1, map.put("a", 3))
        assertEquals(3, backing["a"])
        assertEquals(2, map.remove("b"))
        assertEquals(mapOf("a" to 3), backing)
        map.clear()
        assertEquals(emptyMap(), backing)
    }
}
