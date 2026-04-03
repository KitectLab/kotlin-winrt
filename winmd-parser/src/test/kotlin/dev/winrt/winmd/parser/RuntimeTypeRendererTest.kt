package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeTypeRendererTest {
    @Test
    fun keeps_runtime_class_overrides_interfaces_out_of_public_superinterface_list() {
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
                            name = "IWidgetOverrides",
                            kind = WinMdTypeKind.Interface,
                            guid = "22222222-2222-2222-2222-222222222222",
                        ),
                    ),
                ),
            ),
        )
        val typeRegistry = TypeRegistry(model)
        val renderer = RuntimeTypeRenderer(
            typeNameMapper = TypeNameMapper(),
            typeRegistry = typeRegistry,
            delegateLambdaPlanResolver = DelegateLambdaPlanResolver(TypeNameMapper()),
            eventSlotDelegatePlanResolver = EventSlotDelegatePlanResolver(TypeNameMapper(), typeRegistry),
            runtimePropertyRenderer = RuntimePropertyRenderer(TypeNameMapper(), typeRegistry),
            runtimeMethodRenderer = RuntimeMethodRenderer(
                TypeNameMapper(),
                DelegateLambdaPlanResolver(TypeNameMapper()),
                typeRegistry,
                AsyncMethodRuleRegistry(TypeNameMapper(), AsyncMethodProjectionPlanner(TypeNameMapper(), WinRtSignatureMapper(typeRegistry))),
            ),
            runtimeCompanionRenderer = RuntimeCompanionRenderer(
                typeRegistry,
                TypeNameMapper(),
                DelegateLambdaPlanResolver(TypeNameMapper()),
                EventSlotDelegatePlanResolver(TypeNameMapper(), typeRegistry),
                WinRtSignatureMapper(typeRegistry),
                AsyncMethodRuleRegistry(TypeNameMapper(), AsyncMethodProjectionPlanner(TypeNameMapper(), WinRtSignatureMapper(typeRegistry))),
                WinRtProjectionTypeMapper(),
                KotlinCollectionProjectionMapper(),
            ),
            winRtSignatureMapper = WinRtSignatureMapper(typeRegistry),
            winRtProjectionTypeMapper = WinRtProjectionTypeMapper(),
        )

        val binding = renderer.render(typeRegistry.findType("Widget", "Example.Xaml")!!).toString()

        assertTrue(binding.contains("IWidget"))
        assertFalse(binding.contains("IWidgetOverrides"))
    }
}
