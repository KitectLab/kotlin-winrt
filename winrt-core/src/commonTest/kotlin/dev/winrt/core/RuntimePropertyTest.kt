package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

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

    @Test
    fun builtin_scalar_types_wrap_values() {
        assertEquals(42, Int32(42).value)
        assertEquals(42u, UInt32(42u).value)
        assertFalse(WinRtBoolean.FALSE.value)
    }

    @Test
    fun projected_objects_expose_query_interface_and_helper_state() {
        val subject = WinRtObject(ComPtr.NULL)

        assertEquals(0, subject.queryInterfaceCache.size)
        assertEquals(0, subject.additionalTypeData.size)

        subject.queryInterfaceCache["test"] = ComPtr.NULL
        val first = subject.getOrPutAdditionalTypeData("helper") { mutableListOf("cached") }
        val second = subject.getOrPutAdditionalTypeData("helper") { mutableListOf("new") }

        assertEquals(1, subject.queryInterfaceCache.size)
        assertEquals(1, subject.additionalTypeData.size)
        assertSame(first, second)
    }

    @Test
    fun inspectable_can_cache_object_references_by_type_key() {
        val iid = Guid(0, 0, 0, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        val subject = object : Inspectable(ComPtr.NULL) {
            var queryCount = 0

            override fun queryInterface(iid: Guid): ComPtr {
                queryCount += 1
                return ComPtr.NULL
            }
        }

        val first = subject.getObjectReferenceForType("System.Collections.IList", iid)
        val second = subject.getObjectReferenceForType("System.Collections.IList", iid)

        assertEquals(ComPtr.NULL, first)
        assertEquals(ComPtr.NULL, second)
        assertEquals(1, subject.queryCount)
    }
}
