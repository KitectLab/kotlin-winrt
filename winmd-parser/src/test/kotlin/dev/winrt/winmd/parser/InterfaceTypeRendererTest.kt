package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterfaceTypeRendererTest {
    @Test
    fun renders_versioned_runtime_helpers_as_internal_and_hides_override_interfaces() {
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
                            implementedInterfaces = listOf("Example.Xaml.IWidgetOverrides"),
                        ),
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidget",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                        ),
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidgetStatics",
                            kind = WinMdTypeKind.Interface,
                            guid = "22222222-2222-2222-2222-222222222222",
                        ),
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidgetStatics2",
                            kind = WinMdTypeKind.Interface,
                            guid = "33333333-3333-3333-3333-333333333333",
                        ),
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidgetOverrides",
                            kind = WinMdTypeKind.Interface,
                            guid = "44444444-4444-4444-4444-444444444444",
                        ),
                    ),
                ),
            ),
        )
        val typeRegistry = TypeRegistry(model)
        val renderer = InterfaceTypeRenderer(
            typeNameMapper = TypeNameMapper(),
            delegateLambdaPlanResolver = DelegateLambdaPlanResolver(TypeNameMapper()),
            eventSlotDelegatePlanResolver = EventSlotDelegatePlanResolver(TypeNameMapper(), typeRegistry),
            typeRegistry = typeRegistry,
            asyncMethodProjectionPlanner = AsyncMethodProjectionPlanner(TypeNameMapper(), WinRtSignatureMapper(typeRegistry)),
            asyncMethodRuleRegistry = AsyncMethodRuleRegistry(TypeNameMapper(), AsyncMethodProjectionPlanner(TypeNameMapper(), WinRtSignatureMapper(typeRegistry))),
            winRtProjectionTypeMapper = WinRtProjectionTypeMapper(),
        )

        val widgetStatics = renderer.render(typeRegistry.findType("IWidgetStatics", "Example.Xaml")!!)
        val widgetStatics2 = renderer.render(typeRegistry.findType("IWidgetStatics2", "Example.Xaml")!!)
        val widgetOverrides = renderer.render(typeRegistry.findType("IWidgetOverrides", "Example.Xaml")!!)

        assertEquals(1, widgetStatics.size)
        assertEquals(1, widgetStatics2.size)
        assertTrue(widgetStatics.single().toString().contains("internal open class IWidgetStatics"))
        assertTrue(widgetStatics2.single().toString().contains("internal open class IWidgetStatics2"))
        assertTrue(widgetOverrides.isEmpty())
    }

    @Test
    fun renders_boolean_interface_properties_as_winrt_boolean_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Core",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Core",
                            name = "IWidget",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            properties = listOf(
                                WinMdProperty("Visible", "Boolean", mutable = false, getterVtableIndex = 6),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val typeRegistry = TypeRegistry(model)
        val renderer = InterfaceTypeRenderer(
            typeNameMapper = TypeNameMapper(),
            delegateLambdaPlanResolver = DelegateLambdaPlanResolver(TypeNameMapper()),
            eventSlotDelegatePlanResolver = EventSlotDelegatePlanResolver(TypeNameMapper(), typeRegistry),
            typeRegistry = typeRegistry,
            asyncMethodProjectionPlanner = AsyncMethodProjectionPlanner(TypeNameMapper(), WinRtSignatureMapper(typeRegistry)),
            asyncMethodRuleRegistry = AsyncMethodRuleRegistry(TypeNameMapper(), AsyncMethodProjectionPlanner(TypeNameMapper(), WinRtSignatureMapper(typeRegistry))),
            winRtProjectionTypeMapper = WinRtProjectionTypeMapper(),
        )

        val binding = renderer.render(typeRegistry.findType("IWidget", "Example.Core")!!).single().toString()

        assertTrue(binding.contains("visible"))
        assertTrue(binding.contains("invokeBooleanGetter(pointer, 6).getOrThrow()"))
    }
}
