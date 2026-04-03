package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeCompanionRendererTest {
    @Test
    fun resolves_event_slot_delegate_plans_for_event_handler_and_typed_event_handler() {
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
                        ),
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidgetOpenedEventArgs",
                            kind = WinMdTypeKind.Interface,
                            guid = "55555555-5555-5555-5555-555555555555",
                        ),
                    ),
                ),
            ),
        )
        val typeRegistry = TypeRegistry(model)
        val resolver = EventSlotDelegatePlanResolver(TypeNameMapper(), typeRegistry)

        val eventHandlerPlan = resolver.resolve(
            "Windows.Foundation.EventHandler<Example.Xaml.IWidgetOpenedEventArgs>",
            "Example.Xaml",
        )!!
        val typedEventHandlerPlan = resolver.resolve(
            "Windows.Foundation.TypedEventHandler<Example.Xaml.Widget, Example.Xaml.IWidgetOpenedEventArgs>",
            "Example.Xaml",
        )!!

        assertTrue(eventHandlerPlan.argumentKindsLiteral().contains("OBJECT"))
        assertTrue(eventHandlerPlan.lambdaType.toString().contains("ComPtr"))
        assertTrue(eventHandlerPlan.delegateType.toString().contains("EventHandler"))
        assertTrue(typedEventHandlerPlan.argumentKindsLiteral().contains("OBJECT"))
        assertTrue(typedEventHandlerPlan.delegateType.toString().contains("TypedEventHandler"))
    }
}
