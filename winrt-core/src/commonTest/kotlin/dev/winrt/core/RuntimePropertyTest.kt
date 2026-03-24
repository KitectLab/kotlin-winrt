package dev.winrt.core

import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimePropertyTest {
    @Test
    fun property_roundtrip() {
        val property = RuntimeProperty("before")
        property.set("after")

        assertEquals("after", property.get())
    }
}
