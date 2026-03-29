package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdField
import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TypeRegistryTest {
    private val registry = TypeRegistry(
        WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Point",
                            kind = WinMdTypeKind.Struct,
                            fields = listOf(
                                WinMdField("X", "Float64"),
                                WinMdField("Y", "Float64"),
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IStringable",
                            kind = WinMdTypeKind.Interface,
                            guid = "96369f54-8eb6-48f0-abce-c1b211e627c3",
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVector`1",
                            kind = WinMdTypeKind.Interface,
                            guid = "913337e9-11a1-4345-a3a2-4e7f956e222d",
                            genericParameters = listOf("T"),
                        ),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun resolves_short_names_against_current_namespace() {
        assertEquals(
            "Point",
            registry.findType("Point", "Windows.Foundation")?.name,
        )
    }

    @Test
    fun resolves_canonicalized_generic_type_names() {
        assertEquals(
            "IVector`1",
            registry.findType("Windows.Foundation.Collections.IVector`1<Windows.Foundation.Point>", "Windows.Foundation")?.name,
        )
    }

    @Test
    fun resolves_canonicalized_array_type_names() {
        assertEquals(
            "Point",
            registry.findType("Windows.Foundation.Point[]", "Windows.Foundation")?.name,
        )
    }

    @Test
    fun returns_null_for_unknown_types() {
        assertNull(registry.findType("Windows.Foundation.Missing"))
    }

    @Test
    fun finds_runtime_class_statics_by_runtime_class_name() {
        val runtimeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = listOf(
                    WinMdNamespace(
                        name = "Windows.Globalization",
                        types = listOf(
                            WinMdType(
                                namespace = "Windows.Globalization",
                                name = "ApplicationLanguages",
                                kind = WinMdTypeKind.RuntimeClass,
                                defaultInterface = "Windows.Globalization.IApplicationLanguages",
                            ),
                            WinMdType(
                                namespace = "Windows.Globalization",
                                name = "IApplicationLanguagesStatics",
                                kind = WinMdTypeKind.Interface,
                                guid = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            "IApplicationLanguagesStatics",
            runtimeRegistry.findRuntimeClassStaticsType("ApplicationLanguages", "Windows.Globalization")?.name,
        )
    }

    @Test
    fun finds_default_interface_by_runtime_class_name() {
        val runtimeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = listOf(
                    WinMdNamespace(
                        name = "Microsoft.UI.Xaml",
                        types = listOf(
                            WinMdType(
                                namespace = "Microsoft.UI.Xaml",
                                name = "Window",
                                kind = WinMdTypeKind.RuntimeClass,
                                defaultInterface = "Microsoft.UI.Xaml.IWindow",
                            ),
                            WinMdType(
                                namespace = "Microsoft.UI.Xaml",
                                name = "IWindow",
                                kind = WinMdTypeKind.Interface,
                                guid = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            "IWindow",
            runtimeRegistry.findDefaultInterfaceType("Window", "Microsoft.UI.Xaml")?.name,
        )
    }

    @Test
    fun finds_implemented_interfaces_by_runtime_class_name_without_default_interface_duplication() {
        val runtimeRegistry = TypeRegistry(
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
                                    "Windows.Foundation.IStringable",
                                ),
                            ),
                            WinMdType(
                                namespace = "Windows.Globalization",
                                name = "ICalendar",
                                kind = WinMdTypeKind.Interface,
                                guid = "cccccccc-cccc-cccc-cccc-cccccccccccc",
                            ),
                            WinMdType(
                                namespace = "Windows.Foundation",
                                name = "IStringable",
                                kind = WinMdTypeKind.Interface,
                                guid = "96369f54-8eb6-48f0-abce-c1b211e627c3",
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            listOf("IStringable"),
            runtimeRegistry.findImplementedInterfaceTypes("Calendar", "Windows.Globalization").map { it.name },
        )
    }
}
