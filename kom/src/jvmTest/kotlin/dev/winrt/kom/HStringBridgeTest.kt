package dev.winrt.kom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HStringBridgeTest {
    @Test
    fun hstring_round_trips_and_reports_nulls_correctly() {
        val hString = PlatformHStringBridge.create("hello")

        assertFalse(hString.isNull)
        assertEquals("hello", PlatformHStringBridge.toKotlinString(hString))

        PlatformHStringBridge.release(hString)
    }

    @Test
    fun hstring_null_is_reported_as_null() {
        assertTrue(HString.NULL.isNull)
    }
}
