package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimePropertyTest {
    @Test
    fun property_roundtrip() {
        val property = RuntimeProperty("before")
        property.set("after")

        assertEquals("after", property.get())
    }

    @Test
    fun interface_metadata_projects_using_constructor() {
        val metadata = object : WinRtInterfaceMetadata {
            override val qualifiedName: String = "Test.IProjection"
            override val iid = Guid(0, 0, 0, byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0))
        }

        val projected = metadata.project(ComPtr.NULL) { pointer -> WinRtInterfaceProjection(pointer) }

        assertEquals(ComPtr.NULL, projected.pointer)
    }
}
