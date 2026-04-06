package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModelFactory
import dev.winrt.winmd.plugin.WinMdField
import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import dev.winrt.winmd.plugin.encodeValueTypeName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class WinMdModelFiltersTest {
    @Test
    fun filters_model_to_requested_namespaces() {
        val tempFile = Files.createTempFile("sample", ".winmd")
        Files.write(tempFile, byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()))

        val model = WinMdModelFactory.merge(
            primary = WinMdModelFactory.minimalModel(listOf(tempFile)),
            supplemental = WinMdModelFactory.sampleSupplementalModel(),
        )

        val filtered = WinMdModelFilters.filterNamespaces(
            model = model,
            namespaceFilters = listOf("Windows.Data.Json"),
        )

        assertEquals(listOf("Windows.Data.Json"), filtered.namespaces.map { it.name })
        assertTrue(filtered.namespaces.first().types.isNotEmpty())
    }

    @Test
    fun matches_namespace_prefixes() {
        val model = WinMdModelFactory.sampleSupplementalModel().copy(
            files = emptyList(),
            namespaces = WinMdModelFactory.sampleSupplementalModel().namespaces + listOf(
                dev.winrt.winmd.plugin.WinMdNamespace(
                    name = "Windows.Data.Json.Experimental",
                    types = emptyList(),
                ),
            ),
        )

        val filtered = WinMdModelFilters.filterNamespaces(
            model = model,
            namespaceFilters = listOf("Windows.Data.Json"),
        )

        assertEquals(
            listOf("Windows.Data.Json", "Windows.Data.Json.Experimental"),
            filtered.namespaces.map { it.name },
        )
    }

    @Test
    fun leaves_model_unchanged_without_filters() {
        val tempFile = Files.createTempFile("sample", ".winmd")
        Files.write(tempFile, byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()))

        val model = WinMdModelFactory.merge(
            primary = WinMdModelFactory.minimalModel(listOf(tempFile)),
            supplemental = WinMdModelFactory.sampleSupplementalModel(),
        )

        val filtered = WinMdModelFilters.filterNamespaces(
            model = model,
            namespaceFilters = emptyList(),
        )

        assertEquals(model.namespaces.map { it.name }, filtered.namespaces.map { it.name })
    }

    @Test
    fun filtering_with_projection_dependencies_retains_recursive_external_types() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Example.Xaml.IWidget",
                        ),
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidget",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            properties = listOf(
                                WinMdProperty(
                                    name = "Brush",
                                    type = encodeValueTypeName("Example.Graphics.BrushDescriptor"),
                                    mutable = false,
                                ),
                            ),
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Example.Graphics",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Graphics",
                            name = "BrushDescriptor",
                            kind = WinMdTypeKind.Struct,
                            fields = listOf(
                                WinMdField(
                                    name = "Color",
                                    type = encodeValueTypeName("Example.Graphics.Color"),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Graphics",
                            name = "Color",
                            kind = WinMdTypeKind.Struct,
                            fields = listOf(
                                WinMdField("A", "UInt8"),
                                WinMdField("R", "UInt8"),
                                WinMdField("G", "UInt8"),
                                WinMdField("B", "UInt8"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val filtered = WinMdModelFilters.filterNamespacesWithProjectionDependencies(
            model = model,
            namespaceFilters = listOf("Example.Xaml"),
        )

        assertEquals(
            setOf("Example.Xaml", "Example.Graphics"),
            filtered.namespaces.map { it.name }.toSet(),
        )
        assertEquals(
            setOf("Widget", "IWidget"),
            filtered.namespaces.first { it.name == "Example.Xaml" }.types.map { it.name }.toSet(),
        )
        assertEquals(
            setOf("BrushDescriptor", "Color"),
            filtered.namespaces.first { it.name == "Example.Graphics" }.types.map { it.name }.toSet(),
        )
    }

    @Test
    fun filtering_with_projection_dependencies_retains_structs_nested_inside_ireference() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Example.Xaml.IWidget",
                        ),
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidget",
                            kind = WinMdTypeKind.Interface,
                            guid = "22222222-2222-2222-2222-222222222222",
                            properties = listOf(
                                WinMdProperty(
                                    name = "Range",
                                    type = "Windows.Foundation.IReference`1<${encodeValueTypeName("Example.Graphics.TimeRange")}>",
                                    mutable = false,
                                ),
                            ),
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Example.Graphics",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Graphics",
                            name = "TimeRange",
                            kind = WinMdTypeKind.Struct,
                            fields = listOf(
                                WinMdField("Start", "Float64"),
                                WinMdField("End", "Float64"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val filtered = WinMdModelFilters.filterNamespacesWithProjectionDependencies(
            model = model,
            namespaceFilters = listOf("Example.Xaml"),
        )

        assertEquals(
            setOf("Widget", "IWidget"),
            filtered.namespaces.first { it.name == "Example.Xaml" }.types.map { it.name }.toSet(),
        )
        assertEquals(
            setOf("TimeRange"),
            filtered.namespaces.first { it.name == "Example.Graphics" }.types.map { it.name }.toSet(),
        )
    }
}
