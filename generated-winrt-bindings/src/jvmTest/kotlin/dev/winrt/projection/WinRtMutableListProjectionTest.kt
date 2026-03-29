package dev.winrt.projection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WinRtMutableListProjectionTest {
    @Test
    fun rejects_negative_indices_before_getter_invocation() {
        var getterCalls = 0
        val list = WinRtMutableListProjection(
            sizeProvider = { 0 },
            getter = {
                getterCalls += 1
                it
            },
            append = {},
            clearer = {},
        )

        val error = assertFailsWith<IllegalArgumentException> { list[-1] }

        assertEquals("index must be non-negative", error.message)
        assertEquals(0, getterCalls)
    }

    @Test
    fun supports_insert_set_and_remove_via_rewrite() {
        val appended = mutableListOf<Int>()
        val list = WinRtMutableListProjection(
            sizeProvider = { appended.size },
            getter = { appended[it] },
            append = { appended += it },
            clearer = { appended.clear() },
        )

        list.add(0, 7)
        list.add(1, 9)

        assertEquals(listOf(7, 9), appended)
        list.add(1, 8)
        assertEquals(listOf(7, 8, 9), appended)
        assertEquals(8, list.set(1, 6))
        assertEquals(listOf(7, 6, 9), appended)
        assertEquals(6, list.removeAt(1))
        assertEquals(listOf(7, 9), appended)
    }
}
