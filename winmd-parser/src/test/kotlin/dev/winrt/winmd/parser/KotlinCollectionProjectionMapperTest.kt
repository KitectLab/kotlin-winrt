package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinCollectionProjectionMapperTest {
    private val typeRegistry = TypeRegistry(
        WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Globalization",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Globalization",
                            name = "Calendar",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Globalization.ICalendar",
                            baseInterfaces = listOf(
                                "Windows.Globalization.ICalendar",
                                "Windows.Foundation.Collections.IIterable`1<Windows.Foundation.String>",
                                "Windows.Foundation.Collections.IIterator`1<Windows.Foundation.String>",
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Globalization",
                            name = "ICalendar",
                            kind = WinMdTypeKind.Interface,
                            guid = "cccccccc-cccc-cccc-cccc-cccccccccccc",
                        ),
                        WinMdType(
                            namespace = "Windows.Globalization",
                            name = "DictionaryHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Globalization.ICalendar",
                            baseInterfaces = listOf(
                                "Windows.Foundation.Collections.IMap<String, Windows.Globalization.Calendar>",
                                "Windows.Foundation.Collections.IMapView<String, Windows.Globalization.Calendar>",
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterable`1",
                            kind = WinMdTypeKind.Interface,
                            guid = "faa585ea-6214-4217-afda-7f46de5869b3",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator`1",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IMap`2",
                            kind = WinMdTypeKind.Interface,
                            guid = "fbd6f7c2-0035-4f89-91cb-6b0bf5d8c9d6",
                            genericParameters = listOf("K", "V"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IMapView`2",
                            kind = WinMdTypeKind.Interface,
                            guid = "e5f839be-1a86-4e27-b357-f8c0d2d9d0d1",
                            genericParameters = listOf("K", "V"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "MapRuntimeClass",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IMap<String, Windows.Globalization.Calendar>",
                            baseInterfaces = listOf("Windows.Foundation.Collections.IMap<String, Windows.Globalization.Calendar>"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "MapViewRuntimeClass",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IMapView<String, Windows.Globalization.Calendar>",
                            baseInterfaces = listOf("Windows.Foundation.Collections.IMapView<String, Windows.Globalization.Calendar>"),
                        ),
                    ),
                ),
            ),
        ),
    )

    private val mapper = KotlinCollectionProjectionMapper()

    @Test
    fun enumerates_collection_interfaces_using_registry_helpers() {
        assertEquals(
            listOf(
                "Windows.Globalization.ICalendar",
                "Windows.Foundation.Collections.IIterable`1<Windows.Foundation.String>",
                "Windows.Foundation.Collections.IIterator`1<Windows.Foundation.String>",
            ),
            mapper.runtimeClassCollectionInterfaces(typeRegistry.findType("Calendar", "Windows.Globalization")!!).toList(),
        )
    }

    @Test
    fun enumerates_dictionary_base_interfaces_using_registry_helpers() {
        assertEquals(
            listOf(
                "Windows.Globalization.ICalendar",
                "Windows.Foundation.Collections.IMap<String, Windows.Globalization.Calendar>",
                "Windows.Foundation.Collections.IMapView<String, Windows.Globalization.Calendar>",
            ),
            mapper.runtimeClassCollectionInterfaces(typeRegistry.findType("DictionaryHost", "Windows.Globalization")!!).toList(),
        )
    }

    @Test
    fun projects_dictionary_interfaces_to_kotlin_map_surfaces() {
        val mutableMapProjection = mapper.interfaceProjection(
            typeRegistry.findType("IMap`2", "Windows.Foundation.Collections")!!,
        )
        requireNotNull(mutableMapProjection)
        assertEquals("kotlin.collections.MutableMap<K, V>", mutableMapProjection.superinterface.toString())
        assertEquals(
            true,
            mutableMapProjection.delegateFactory.toString().contains("WinRtMutableMapProjection"),
        )

        val mapProjection = mapper.interfaceProjection(
            typeRegistry.findType("IMapView`2", "Windows.Foundation.Collections")!!,
        )
        requireNotNull(mapProjection)
        assertEquals("kotlin.collections.Map<K, V>", mapProjection.superinterface.toString())
        assertEquals(
            true,
            mapProjection.delegateFactory.toString().contains("WinRtMapProjection"),
        )
    }

    @Test
    fun projects_runtime_classes_with_dictionary_base_interfaces() {
        val mutableMapProjection = mapper.runtimeClassInterfaceProjection(
            typeRegistry.findType("MapRuntimeClass", "Windows.Foundation.Collections")!!,
            TypeNameMapper(),
            WinRtSignatureMapper(typeRegistry),
            WinRtProjectionTypeMapper(),
        )
        requireNotNull(mutableMapProjection)
        assertEquals("kotlin.collections.MutableMap<kotlin.String, windows.globalization.Calendar>", mutableMapProjection.superinterface.toString())
        assertEquals(
            true,
            mutableMapProjection.delegateFactory.toString().contains("WinRtMutableMapProjection"),
        )

        val mapProjection = mapper.runtimeClassInterfaceProjection(
            typeRegistry.findType("MapViewRuntimeClass", "Windows.Foundation.Collections")!!,
            TypeNameMapper(),
            WinRtSignatureMapper(typeRegistry),
            WinRtProjectionTypeMapper(),
        )
        requireNotNull(mapProjection)
        assertEquals("kotlin.collections.Map<kotlin.String, windows.globalization.Calendar>", mapProjection.superinterface.toString())
        assertEquals(
            true,
            mapProjection.delegateFactory.toString().contains("WinRtMapProjection"),
        )
    }

    @Test
    fun maps_key_value_pair_interfaces_to_entry_projections() {
        assertEquals(
            "kotlin.collections.Map.Entry<String, Microsoft.UI.Xaml.UIElement>",
            WinRtProjectionTypeMapper().projectionTypeKeyFor(
                "Windows.Foundation.Collections.IKeyValuePair`2<String, Microsoft.UI.Xaml.UIElement>",
                "Windows.Foundation.Collections",
            ),
        )
    }
}
