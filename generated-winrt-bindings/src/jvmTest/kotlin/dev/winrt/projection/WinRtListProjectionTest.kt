package dev.winrt.projection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WinRtListProjectionTest {
    @Test
    fun rejects_negative_indices_before_getter_invocation() {
        var getterCalls = 0
        val list = WinRtListProjection(
            sizeProvider = { 0 },
            getter = {
                getterCalls += 1
                it
            },
        )

        val error = assertFailsWith<IllegalArgumentException> { list[-1] }

        assertEquals("index must be non-negative", error.message)
        assertEquals(0, getterCalls)
    }

    @Test
    fun exposes_list_shape() {
        val list: List<Int> = WinRtListProjection(
            sizeProvider = { 2 },
            getter = { index -> index + 1 },
        )

        assertEquals(2, list.size)
        assertEquals(1, list[0])
        assertEquals(2, list[1])
    }
}
