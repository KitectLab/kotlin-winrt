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
    fun appends_only_at_tail() {
        val appended = mutableListOf<Int>()
        val list = WinRtMutableListProjection(
            sizeProvider = { appended.size },
            getter = { appended[it] },
            append = { appended += it },
            clearer = { appended.clear() },
        )

        list.add(0, 7)

        assertEquals(listOf(7), appended)
        assertFailsWith<UnsupportedOperationException> { list.add(0, 9) }
    }
}
