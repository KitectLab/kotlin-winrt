package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdField
import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun orders_versioned_runtime_class_helper_types_by_numeric_suffix() {
        val runtimeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = listOf(
                    WinMdNamespace(
                        name = "Example.Xaml",
                        types = listOf(
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "Widget",
                                kind = WinMdTypeKind.RuntimeClass,
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetStatics2",
                                kind = WinMdTypeKind.Interface,
                                guid = "11111111-1111-1111-1111-111111111112",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetStatics",
                                kind = WinMdTypeKind.Interface,
                                guid = "11111111-1111-1111-1111-111111111111",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetStatics10",
                                kind = WinMdTypeKind.Interface,
                                guid = "11111111-1111-1111-1111-111111111120",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetFactory2",
                                kind = WinMdTypeKind.Interface,
                                guid = "22222222-2222-2222-2222-222222222222",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetFactory",
                                kind = WinMdTypeKind.Interface,
                                guid = "22222222-2222-2222-2222-222222222221",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetOverrides2",
                                kind = WinMdTypeKind.Interface,
                                guid = "33333333-3333-3333-3333-333333333332",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetOverrides",
                                kind = WinMdTypeKind.Interface,
                                guid = "33333333-3333-3333-3333-333333333331",
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            listOf("IWidgetStatics", "IWidgetStatics2", "IWidgetStatics10"),
            runtimeRegistry.findRuntimeClassStaticsTypes("Widget", "Example.Xaml").map { it.name },
        )
        assertEquals(
            listOf("IWidgetFactory", "IWidgetFactory2"),
            runtimeRegistry.findRuntimeClassFactoryTypes("Widget", "Example.Xaml").map { it.name },
        )
        assertEquals(
            listOf("IWidgetOverrides", "IWidgetOverrides2"),
            runtimeRegistry.findRuntimeClassOverridesTypes("Widget", "Example.Xaml").map { it.name },
        )
    }

    @Test
    fun ignores_non_versioned_helper_suffixes() {
        val runtimeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = listOf(
                    WinMdNamespace(
                        name = "Example.Xaml",
                        types = listOf(
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "Widget",
                                kind = WinMdTypeKind.RuntimeClass,
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetStaticsHelper",
                                kind = WinMdTypeKind.Interface,
                                guid = "44444444-4444-4444-4444-444444444444",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetFactoryHelper",
                                kind = WinMdTypeKind.Interface,
                                guid = "55555555-5555-5555-5555-555555555555",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetOverridesHelper",
                                kind = WinMdTypeKind.Interface,
                                guid = "66666666-6666-6666-6666-666666666666",
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(emptyList<String>(), runtimeRegistry.findRuntimeClassStaticsTypes("Widget", "Example.Xaml").map { it.name })
        assertEquals(emptyList<String>(), runtimeRegistry.findRuntimeClassFactoryTypes("Widget", "Example.Xaml").map { it.name })
        assertEquals(emptyList<String>(), runtimeRegistry.findRuntimeClassOverridesTypes("Widget", "Example.Xaml").map { it.name })
    }

    @Test
    fun recognizes_only_factory_and_static_interfaces_as_runtime_class_helpers() {
        val runtimeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = listOf(
                    WinMdNamespace(
                        name = "Example.Xaml",
                        types = listOf(
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "Widget",
                                kind = WinMdTypeKind.RuntimeClass,
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetStatics",
                                kind = WinMdTypeKind.Interface,
                                guid = "77777777-7777-7777-7777-777777777771",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetFactory2",
                                kind = WinMdTypeKind.Interface,
                                guid = "77777777-7777-7777-7777-777777777772",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetOverrides2",
                                kind = WinMdTypeKind.Interface,
                                guid = "77777777-7777-7777-7777-777777777773",
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(runtimeRegistry.isRuntimeClassHelperInterface("IWidgetStatics", "Example.Xaml"))
        assertTrue(runtimeRegistry.isRuntimeClassHelperInterface("IWidgetFactory2", "Example.Xaml"))
        assertFalse(runtimeRegistry.isRuntimeClassHelperInterface("IWidgetOverrides2", "Example.Xaml"))
    }

    @Test
    fun helper_accessor_names_preserve_versioned_factory_and_static_suffixes() {
        assertEquals("statics", helperAccessorName("IWidgetStatics"))
        assertEquals("statics2", helperAccessorName("IWidgetStatics2"))
        assertEquals("statics10", helperAccessorName("IWidgetStatics10"))
        assertEquals("factory", helperAccessorName("IWidgetFactory"))
        assertEquals("factory2", helperAccessorName("IWidgetFactory2"))
        assertEquals("factory10", helperAccessorName("IWidgetFactory10"))
        assertEquals("widgetOverrides", helperAccessorName("IWidgetOverrides"))
        assertEquals("widgetOverrides2", helperAccessorName("IWidgetOverrides2"))
        assertEquals("widgetOverrides10", helperAccessorName("IWidgetOverrides10"))
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
