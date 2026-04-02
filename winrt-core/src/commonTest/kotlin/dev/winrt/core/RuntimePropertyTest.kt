package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertNull

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
    fun interface_metadata_projection_cache_key_tracks_projection_and_qualified_name() {
        val metadata = object : WinRtInterfaceMetadata {
            override val qualifiedName: String = "Test.IProjection"
            override val projectionTypeKey: String = "Test.Alias"
            override val iid = Guid(0, 0, 0, byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0))
        }

        assertEquals("projection:Test.Alias:Test.IProjection", metadata.projectionCacheKey)
    }

    @Test
    fun runtime_class_metadata_defaults_activation_kind_to_factory() {
        val metadata = object : WinRtRuntimeClassMetadata {
            override val qualifiedName: String = "Test.RuntimeClass"
            override val classId = RuntimeClassId("Test", "RuntimeClass")
            override val defaultInterfaceName: String? = null
        }

        assertEquals(WinRtActivationKind.Factory, metadata.activationKind)
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

        val first = subject.getObjectReferenceForType("kotlin.collections.MutableList", iid)
        val second = subject.getObjectReferenceForType("kotlin.collections.MutableList", iid)

        assertEquals(ComPtr.NULL, first)
        assertEquals(ComPtr.NULL, second)
        assertEquals(1, subject.queryCount)
    }

    @Test
    fun projection_registry_maps_projected_types_to_helper_keys() {
        WinRtProjectionRegistry.resetForTests()

        assertEquals(
            "kotlin.collections.MutableList",
            WinRtProjectionRegistry.projectionTypeKeyFor("Microsoft.UI.Xaml.Interop.IBindableVector"),
        )
        assertNull(WinRtProjectionRegistry.findProjectionTypeKey("Windows.Foundation.Collections.IVector<String>"))
        assertEquals(
            "ABI.System.Collections.IList",
            WinRtProjectionRegistry.abiHelperTypeKeyFor("kotlin.collections.MutableList"),
        )
        assertEquals(
            "ABI.System.Collections.Generic.IList<Microsoft.UI.Xaml.UIElement>",
            WinRtProjectionRegistry.abiHelperTypeKeyFor("kotlin.collections.MutableList<Microsoft.UI.Xaml.UIElement>"),
        )
    }

    @Test
    fun inspectable_can_cache_object_references_by_projected_type_key() {
        WinRtProjectionRegistry.resetForTests()

        val iid = Guid(0, 0, 0, byteArrayOf(8, 7, 6, 5, 4, 3, 2, 1))
        val subject = object : Inspectable(ComPtr.NULL) {
            var queryCount = 0

            override fun queryInterface(iid: Guid): ComPtr {
                queryCount += 1
                return ComPtr.NULL
            }
        }

        val first = subject.getObjectReferenceForProjectedType("Microsoft.UI.Xaml.Interop.IBindableVector", iid)
        val second = subject.getObjectReferenceForType("ABI.System.Collections.IList", iid)

        assertEquals(ComPtr.NULL, first)
        assertEquals(ComPtr.NULL, second)
        assertEquals(1, subject.queryCount)
    }

    @Test
    fun interface_projection_wrapper_is_cached_by_projection_key() {
        WinRtProjectionRegistry.resetForTests()

        val metadata = object : WinRtInterfaceMetadata {
            override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVector"
            override val projectionTypeKey: String = "kotlin.collections.MutableList"
            override val iid = Guid(0, 0, 0, byteArrayOf(5, 4, 3, 2, 1, 0, 9, 8))
        }
        val subject = object : Inspectable(ComPtr.NULL) {
            var queryCount = 0
            var constructorCount = 0

            override fun queryInterface(iid: Guid): ComPtr {
                queryCount += 1
                return ComPtr.NULL
            }
        }

        val first = subject.projectInterface(metadata) {
            subject.constructorCount += 1
            WinRtInterfaceProjection(it)
        }
        val second = subject.projectInterface(metadata) {
            subject.constructorCount += 1
            WinRtInterfaceProjection(it)
        }

        assertSame(first, second)
        assertEquals(1, subject.queryCount)
        assertEquals(1, subject.constructorCount)
    }

    @Test
    fun projected_type_lookup_aliases_winrt_and_projection_keys_to_same_reference() {
        WinRtProjectionRegistry.resetForTests()

        val iid = Guid(0, 0, 0, byteArrayOf(4, 3, 2, 1, 0, 9, 8, 7))
        val subject = object : Inspectable(ComPtr.NULL) {
            var queryCount = 0

            override fun queryInterface(iid: Guid): ComPtr {
                queryCount += 1
                return ComPtr.NULL
            }
        }

        val first = subject.getObjectReferenceForProjectedType("Microsoft.UI.Xaml.Interop.IBindableVector", iid)
        val second = subject.getObjectReferenceForType("kotlin.collections.MutableList", iid)
        val third = subject.getObjectReferenceForType("ABI.System.Collections.IList", iid)
        val fourth = subject.getObjectReferenceForType("Microsoft.UI.Xaml.Interop.IBindableVector", iid)

        assertEquals(ComPtr.NULL, first)
        assertEquals(ComPtr.NULL, second)
        assertEquals(ComPtr.NULL, third)
        assertEquals(ComPtr.NULL, fourth)
        assertEquals(1, subject.queryCount)
    }

    @Test
    fun inspectable_argument_pointer_uses_cached_iinspectable_reference() {
        val subject = object : Inspectable(ComPtr.NULL) {
            val requestedIids = mutableListOf<Guid>()

            override fun queryInterface(iid: Guid): ComPtr {
                requestedIids += iid
                return ComPtr.NULL
            }
        }

        val first = subject.getInspectableArgumentPointer()
        val second = subject.getInspectableArgumentPointer()

        assertEquals(ComPtr.NULL, first)
        assertEquals(ComPtr.NULL, second)
        assertEquals(listOf(Inspectable.iinspectableIid), subject.requestedIids)
    }

    @Test
    fun inspectable_can_cache_helper_wrappers_by_type_key() {
        val subject = Inspectable(ComPtr.NULL)

        val first = subject.getOrPutHelperWrapper("kotlin.collections.MutableList") { mutableListOf("cached") }
        val second = subject.getOrPutHelperWrapper("kotlin.collections.MutableList") { mutableListOf("new") }

        assertSame(first, second)
        assertEquals(listOf("cached"), first)
    }

    @Test
    fun interface_projection_uses_helper_type_mapping_for_object_reference_lookup() {
        WinRtProjectionRegistry.resetForTests()

        val metadata = object : WinRtInterfaceMetadata {
            override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVector"
            override val iid = Guid(0, 0, 0, byteArrayOf(3, 1, 4, 1, 5, 9, 2, 6))
        }
        val subject = object : Inspectable(ComPtr.NULL) {
            val requestedKeys = mutableListOf<String>()

            override fun queryInterface(iid: Guid): ComPtr {
                return ComPtr.NULL
            }

            override fun getObjectReferenceForType(typeKey: String, iid: Guid): ComPtr {
                requestedKeys += typeKey
                return super.getObjectReferenceForType(typeKey, iid)
            }
        }

        val projection = subject.projectInterface(metadata) { pointer ->
            WinRtInterfaceProjection(pointer)
        }

        assertEquals(ComPtr.NULL, projection.pointer)
        assertEquals(listOf("ABI.System.Collections.IList"), subject.requestedKeys)
    }

    @Test
    fun interface_projection_uses_projection_type_key_when_provided() {
        WinRtProjectionRegistry.resetForTests()
        WinRtProjectionRegistry.registerAbiHelperTypeMapping(
            projectionTypeKey = "kotlin.collections.MutableList",
            abiHelperTypeKey = "ABI.System.Collections.IList",
        )

        val metadata = object : WinRtInterfaceMetadata {
            override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVector"
            override val projectionTypeKey: String = "kotlin.collections.MutableList"
            override val iid = Guid(0, 0, 0, byteArrayOf(9, 9, 9, 9, 9, 9, 9, 9))
        }
        val subject = object : Inspectable(ComPtr.NULL) {
            val requestedKeys = mutableListOf<String>()

            override fun queryInterface(iid: Guid): ComPtr = ComPtr.NULL

            override fun getObjectReferenceForType(typeKey: String, iid: Guid): ComPtr {
                requestedKeys += typeKey
                return super.getObjectReferenceForType(typeKey, iid)
            }
        }

        subject.projectInterface(metadata) { pointer -> WinRtInterfaceProjection(pointer) }

        assertEquals(listOf("ABI.System.Collections.IList"), subject.requestedKeys)
    }

    @Test
    fun interface_projection_caches_projected_wrapper_by_projection_cache_key() {
        WinRtProjectionRegistry.resetForTests()

        val metadata = object : WinRtInterfaceMetadata {
            override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVector"
            override val projectionTypeKey: String = "kotlin.collections.MutableList"
            override val iid = Guid(0, 0, 0, byteArrayOf(2, 7, 1, 8, 2, 8, 1, 8))
        }
        val subject = object : Inspectable(ComPtr.NULL) {
            var queryCount = 0

            override fun queryInterface(iid: Guid): ComPtr {
                queryCount += 1
                return ComPtr.NULL
            }
        }

        val first = subject.projectInterface(metadata, ::WinRtInterfaceProjection)
        val second = subject.projectInterface(metadata, ::WinRtInterfaceProjection)

        assertSame(first, second)
        assertEquals(1, subject.queryCount)
    }

    @Test
    fun interface_projection_separates_cache_entries_for_distinct_projection_type_keys() {
        WinRtProjectionRegistry.resetForTests()

        val metadataA = object : WinRtInterfaceMetadata {
            override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVector"
            override val projectionTypeKey: String = "kotlin.collections.MutableList"
            override val iid = Guid(0, 0, 0, byteArrayOf(1, 1, 1, 1, 1, 1, 1, 1))
        }
        val metadataB = object : WinRtInterfaceMetadata {
            override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVector"
            override val projectionTypeKey: String = "Test.CustomProjection"
            override val iid = Guid(0, 0, 0, byteArrayOf(1, 1, 1, 1, 1, 1, 1, 1))
        }
        val subject = object : Inspectable(ComPtr.NULL) {
            var queryCount = 0

            override fun queryInterface(iid: Guid): ComPtr {
                queryCount += 1
                return ComPtr.NULL
            }
        }

        val first = subject.projectInterface(metadataA, ::WinRtInterfaceProjection)
        val second = subject.projectInterface(metadataB, ::WinRtInterfaceProjection)

        assertEquals(ComPtr.NULL, first.pointer)
        assertEquals(ComPtr.NULL, second.pointer)
        assertEquals(2, subject.queryCount)
    }
}
