package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.encodeValueTypeName
import dev.winrt.winmd.plugin.WinMdMetadataReader
import dev.winrt.winmd.plugin.WinMdField
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.NuGetPackageReferences
import dev.winrt.winmd.plugin.WinMdParameter
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdActivationKind
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class TypeRegistryTest {
    private val windowsAppSdkVersion = "1.8.260317003"

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
    fun identifies_struct_types_from_metadata() {
        assertTrue(registry.isStructType("Point", "Windows.Foundation"))
        assertFalse(registry.isStructType("IStringable", "Windows.Foundation"))
    }

    @Test
    fun does_not_treat_arrays_as_struct_types() {
        assertFalse(registry.isStructType("Windows.Foundation.Point[]", "Windows.Foundation"))
    }

    @Test
    fun does_not_treat_arrays_as_enum_types() {
        val runtimeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = listOf(
                    WinMdNamespace(
                        name = "Example.Core",
                        types = listOf(
                            WinMdType(
                                namespace = "Example.Core",
                                name = "AnnotationType",
                                kind = WinMdTypeKind.Enum,
                                enumUnderlyingType = "Int32",
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertFalse(runtimeRegistry.isEnumType("Example.Core.AnnotationType[]", "Example.Core"))
        assertNull(runtimeRegistry.enumUnderlyingType("Example.Core.AnnotationType[]", "Example.Core"))
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
    fun treats_interfaces_inheriting_runtime_projected_interfaces_as_runtime_projected() {
        val runtimeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = listOf(
                    WinMdNamespace(
                        name = "Example.Collections",
                        types = listOf(
                            WinMdType(
                                namespace = "Example.Collections",
                                name = "VectorHost",
                                kind = WinMdTypeKind.RuntimeClass,
                                defaultInterface = "Example.Collections.IVectorHost",
                            ),
                            WinMdType(
                                namespace = "Example.Collections",
                                name = "IVectorHost",
                                kind = WinMdTypeKind.Interface,
                                guid = "11111111-1111-1111-1111-111111111111",
                            ),
                            WinMdType(
                                namespace = "Example.Collections",
                                name = "ICollectionViewLike",
                                kind = WinMdTypeKind.Interface,
                                guid = "22222222-2222-2222-2222-222222222222",
                                baseInterfaces = listOf("Example.Collections.IVectorHost"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(runtimeRegistry.isRuntimeProjectedInterface("IVectorHost", "Example.Collections"))
        assertTrue(runtimeRegistry.isRuntimeProjectedInterface("ICollectionViewLike", "Example.Collections"))
    }

    @Test
    fun treats_base_interfaces_of_runtime_projected_interfaces_as_runtime_projected() {
        val runtimeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = listOf(
                    WinMdNamespace(
                        name = "Windows.Foundation.Collections",
                        types = listOf(
                            WinMdType(
                                namespace = "Windows.Foundation.Collections",
                                name = "IMapView`2",
                                kind = WinMdTypeKind.Interface,
                                guid = "11111111-1111-1111-1111-111111111111",
                                genericParameters = listOf("K", "V"),
                            ),
                        ),
                    ),
                    WinMdNamespace(
                        name = "Example.Collections",
                        types = listOf(
                            WinMdType(
                                namespace = "Example.Collections",
                                name = "MapHost",
                                kind = WinMdTypeKind.RuntimeClass,
                                defaultInterface = "Example.Collections.IMapHost",
                            ),
                            WinMdType(
                                namespace = "Example.Collections",
                                name = "IMapHost",
                                kind = WinMdTypeKind.Interface,
                                guid = "22222222-2222-2222-2222-222222222222",
                                baseInterfaces = listOf("Windows.Foundation.Collections.IMapView<String, Object>"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(runtimeRegistry.isRuntimeProjectedInterface("IMapHost", "Example.Collections"))
        assertTrue(runtimeRegistry.isRuntimeProjectedInterface("IMapView", "Windows.Foundation.Collections"))
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
    fun recognizes_composable_factory_methods_from_out_inner_parameter_shape_without_overriding_activatable_runtime_classes() {
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
                                defaultInterface = "Example.Xaml.IWidget",
                                hasActivatableAttribute = true,
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidget",
                                kind = WinMdTypeKind.Interface,
                                guid = "11111111-1111-1111-1111-111111111111",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IWidgetFactory",
                                kind = WinMdTypeKind.Interface,
                                guid = "22222222-2222-2222-2222-222222222222",
                                methods = listOf(
                                    WinMdMethod(
                                        name = "CreateInstance",
                                        returnType = "Example.Xaml.Widget",
                                        parameters = listOf(
                                            WinMdParameter(name = "label", type = "String"),
                                            WinMdParameter(name = "baseInterface", type = "Object"),
                                            WinMdParameter(name = "innerInterface", type = "Object", byRef = true, isOut = true),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val widget = runtimeRegistry.findType("Widget", "Example.Xaml")!!
        val composableMethods = runtimeRegistry.findComposableFactoryMethods("Widget", "Example.Xaml")

        assertEquals(1, composableMethods.size)
        assertEquals("CreateInstance", composableMethods.single().method.name)
        assertEquals(WinMdActivationKind.Factory, runtimeRegistry.runtimeClassActivationKind(widget))
    }

    @Test
    fun finds_inherited_composable_factory_methods_and_promotes_activation_kind() {
        val runtimeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = listOf(
                    WinMdNamespace(
                        name = "Example.Xaml",
                        types = listOf(
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "BaseWidget",
                                kind = WinMdTypeKind.RuntimeClass,
                                defaultInterface = "Example.Xaml.IBaseWidget",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "DerivedWidget",
                                kind = WinMdTypeKind.RuntimeClass,
                                baseClass = "Example.Xaml.BaseWidget",
                                defaultInterface = "Example.Xaml.IDerivedWidget",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IBaseWidget",
                                kind = WinMdTypeKind.Interface,
                                guid = "11111111-1111-1111-1111-111111111111",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IDerivedWidget",
                                kind = WinMdTypeKind.Interface,
                                guid = "22222222-2222-2222-2222-222222222222",
                            ),
                            WinMdType(
                                namespace = "Example.Xaml",
                                name = "IBaseWidgetFactory",
                                kind = WinMdTypeKind.Interface,
                                guid = "33333333-3333-3333-3333-333333333333",
                                methods = listOf(
                                    WinMdMethod(
                                        name = "CreateInstance",
                                        returnType = "Example.Xaml.BaseWidget",
                                        parameters = listOf(
                                            WinMdParameter(name = "baseInterface", type = "Object"),
                                            WinMdParameter(name = "innerInterface", type = "Object", byRef = true, isOut = true),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val derivedWidget = runtimeRegistry.findType("DerivedWidget", "Example.Xaml")!!

        assertEquals(WinMdActivationKind.Factory, runtimeRegistry.runtimeClassActivationKind(derivedWidget))
    }

    @Test
    fun resolves_real_winui_runtime_class_activation_kinds_when_available() {
        val winuiWinmd = localWinUiXamlWinmdCandidates().firstOrNull { Files.isRegularFile(it) } ?: return
        val model = try {
            WinMdMetadataReader.readModel(listOf(winuiWinmd))
        } catch (error: IllegalArgumentException) {
            if (winuiWinmd.toString().contains("microsoft.windowsappsdk", ignoreCase = true) &&
                error.message?.startsWith("Metadata index exceeds Int range:") == true
            ) {
                return
            }
            throw error
        }
        val runtimeRegistry = TypeRegistry(model)
        val xamlNamespace = model.namespaces.firstOrNull { it.name == "Microsoft.UI.Xaml" }
            ?: error("Missing Microsoft.UI.Xaml namespace. Available: ${model.namespaces.map { it.name }}")
        val controlsNamespace = model.namespaces.firstOrNull { it.name == "Microsoft.UI.Xaml.Controls" }
            ?: error("Missing Microsoft.UI.Xaml.Controls namespace. Available: ${model.namespaces.map { it.name }}")
        val xamlTypes = xamlNamespace.types.associateBy { it.name }
        val controlTypes = controlsNamespace.types.associateBy { it.name }

        assertEquals(
            WinMdActivationKind.Composable,
            runtimeRegistry.runtimeClassActivationKind(
                requireNotNull(xamlTypes["Application"]) { xamlTypes.keys.sorted().joinToString() },
            ),
        )
        assertEquals(
            WinMdActivationKind.Composable,
            runtimeRegistry.runtimeClassActivationKind(
                requireNotNull(xamlTypes["Window"]) { xamlTypes.keys.sorted().joinToString() },
            ),
        )
        assertEquals(
            WinMdActivationKind.Factory,
            runtimeRegistry.runtimeClassActivationKind(
                requireNotNull(controlTypes["TextBlock"]) { controlTypes.keys.sorted().joinToString() },
            ),
        )
        assertEquals(
            WinMdActivationKind.Composable,
            runtimeRegistry.runtimeClassActivationKind(
                requireNotNull(controlTypes["Button"]) { controlTypes.keys.sorted().joinToString() },
            ),
        )
        assertEquals(
            WinMdActivationKind.Factory,
            runtimeRegistry.runtimeClassActivationKind(
                requireNotNull(controlTypes["ToggleSwitch"]) { controlTypes.keys.sorted().joinToString() },
            ),
        )
        assertEquals(
            WinMdActivationKind.Composable,
            runtimeRegistry.runtimeClassActivationKind(
                requireNotNull(controlTypes["StackPanel"]) { controlTypes.keys.sorted().joinToString() },
            ),
        )
        assertEquals(
            WinMdActivationKind.Factory,
            runtimeRegistry.runtimeClassActivationKind(
                requireNotNull(controlTypes["XamlControlsResources"]) { controlTypes.keys.sorted().joinToString() },
            ),
        )
    }

    @Test
    fun keeps_resource_dictionary_derived_runtime_classes_factory_activated_when_metadata_is_activatable() {
        val runtimeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = listOf(
                    WinMdNamespace(
                        name = "Microsoft.UI.Xaml",
                        types = listOf(
                            WinMdType(
                                namespace = "Microsoft.UI.Xaml",
                                name = "ResourceDictionary",
                                kind = WinMdTypeKind.RuntimeClass,
                                defaultInterface = "Microsoft.UI.Xaml.IResourceDictionary",
                            ),
                            WinMdType(
                                namespace = "Microsoft.UI.Xaml",
                                name = "IResourceDictionary",
                                kind = WinMdTypeKind.Interface,
                                guid = "11111111-1111-1111-1111-111111111111",
                            ),
                            WinMdType(
                                namespace = "Microsoft.UI.Xaml",
                                name = "IResourceDictionaryFactory",
                                kind = WinMdTypeKind.Interface,
                                guid = "22222222-2222-2222-2222-222222222222",
                                methods = listOf(
                                    WinMdMethod(
                                        name = "CreateInstance",
                                        returnType = "Microsoft.UI.Xaml.ResourceDictionary",
                                        parameters = listOf(
                                            WinMdParameter(name = "baseInterface", type = "Object"),
                                            WinMdParameter(name = "innerInterface", type = "Object", byRef = true, isOut = true),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                    WinMdNamespace(
                        name = "Microsoft.UI.Xaml.Controls",
                        types = listOf(
                            WinMdType(
                                namespace = "Microsoft.UI.Xaml.Controls",
                                name = "XamlControlsResources",
                                kind = WinMdTypeKind.RuntimeClass,
                                baseClass = "Microsoft.UI.Xaml.ResourceDictionary",
                                defaultInterface = "Microsoft.UI.Xaml.Controls.IXamlControlsResources",
                                hasActivatableAttribute = true,
                            ),
                            WinMdType(
                                namespace = "Microsoft.UI.Xaml.Controls",
                                name = "IXamlControlsResources",
                                kind = WinMdTypeKind.Interface,
                                guid = "33333333-3333-3333-3333-333333333333",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val xamlControlsResources = runtimeRegistry.findType("XamlControlsResources", "Microsoft.UI.Xaml.Controls")!!

        assertTrue(runtimeRegistry.isResourceDictionaryDerivedRuntimeClass(xamlControlsResources))
        assertEquals(WinMdActivationKind.Factory, runtimeRegistry.runtimeClassActivationKind(xamlControlsResources))
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

    @Test
    fun treats_external_known_value_types_as_structs_only_when_signature_marks_them_as_valuetype() {
        val runtimeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = emptyList(),
            ),
        )

        assertFalse(runtimeRegistry.isStructType("Microsoft.UI.WindowId", "Example.Xaml"))
        assertTrue(runtimeRegistry.isStructType(encodeValueTypeName("Microsoft.UI.WindowId"), "Example.Xaml"))
        assertFalse(runtimeRegistry.isStructType("Windows.UI.Text.FontWeight", "Example.Xaml"))
        assertTrue(runtimeRegistry.isStructType(encodeValueTypeName("Windows.UI.Text.FontWeight"), "Example.Xaml"))
        assertFalse(runtimeRegistry.isStructType("Windows.UI.Xaml.Interop.TypeName", "Example.Xaml"))
        assertTrue(runtimeRegistry.isStructType(encodeValueTypeName("Windows.UI.Xaml.Interop.TypeName"), "Example.Xaml"))
    }

    private fun localWinUiXamlWinmdCandidates(): List<Path> {
        return buildList {
            System.getProperty("dev.winrt.windowsAppSdkRoot")
                ?.takeIf { it.isNotBlank() }
                ?.let { add(Path.of(it).resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd")) }
            addAll(windowsAppSdkWinmdCandidates("Microsoft.UI.Xaml.winmd"))
            add(Path.of("C:/Program Files (x86)/Mica For Everyone/Microsoft.UI.Xaml.winmd"))
        }.distinct()
    }

    private fun windowsAppSdkWinmdCandidates(fileName: String): List<Path> {
        val nugetRoots = buildList {
            add(Path.of("F:/Dependencies/nuget"))
            runCatching { NuGetPackageReferences.discoverPackagesRoot() }.getOrNull()?.let(::add)
        }.distinct()

        return runCatching {
            NuGetPackageReferences.resolvePackageFromRoots(
                packageId = "Microsoft.WindowsAppSDK",
                packageVersion = windowsAppSdkVersion,
                nugetRoots = nugetRoots,
            ).winmdFiles.filter { it.fileName.toString().equals(fileName, ignoreCase = true) }
        }.getOrDefault(emptyList())
    }
}
