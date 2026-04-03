package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdParameter
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

    @Test
    fun unsubscribes_runtime_event_slots_with_finally_to_close_delegate_handles() {
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
                            methods = listOf(
                                dev.winrt.winmd.plugin.WinMdMethod(
                                    name = "add_Opened",
                                    returnType = "EventRegistrationToken",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        dev.winrt.winmd.plugin.WinMdParameter(
                                            name = "handler",
                                            type = "Windows.Foundation.EventHandler<Example.Xaml.IWidgetOpenedEventArgs>",
                                        ),
                                    ),
                                ),
                                dev.winrt.winmd.plugin.WinMdMethod(
                                    name = "remove_Opened",
                                    returnType = "Unit",
                                    vtableIndex = 7,
                                    parameters = listOf(
                                        dev.winrt.winmd.plugin.WinMdParameter(
                                            name = "token",
                                            type = "EventRegistrationToken",
                                        ),
                                    ),
                                ),
                            ),
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

        assertTrue(binding.contains("finally"))
        assertTrue(binding.contains("delegateHandles.remove(token)?.close()"))
    }

    @Test
    fun renders_composable_runtime_classes_without_activate_instance_constructor() {
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
                            name = "IWidgetFactory",
                            kind = WinMdTypeKind.Interface,
                            guid = "22222222-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateInstance",
                                    returnType = "Example.Xaml.Widget",
                                    vtableIndex = 6,
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

        assertFalse(binding.contains("Companion.activate().pointer"))
        assertTrue(binding.contains("factoryCreateInstance(label).pointer"))
        assertFalse(binding.contains("baseInterface"))
        assertFalse(binding.contains("innerInterface"))
        assertTrue(binding.contains("WinRtActivationKind.Composable"))
        assertFalse(binding.contains("fun activate(): Widget"))
        assertTrue(binding.contains("WinRtRuntime.compose("))
        assertTrue(binding.contains("guidOf(\"22222222-2222-2222-2222-222222222222\")"))
        assertTrue(binding.contains("::Widget"))
        assertTrue(binding.contains("ComPtr.NULL"))
    }

    @Test
    fun renders_derived_runtime_classes_with_inherited_composable_constructor() {
        val model = WinMdModel(
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
                                    vtableIndex = 6,
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

        val binding = renderer.render(typeRegistry.findType("DerivedWidget", "Example.Xaml")!!).toString()

        assertFalse(binding.contains("Companion.activate().pointer"))
        assertTrue(binding.contains("constructor() : this(Companion.factoryCreateInstance().pointer)"))
        assertTrue(binding.contains("WinRtActivationKind.Composable"))
        assertFalse(binding.contains("fun activate(): DerivedWidget"))
        assertTrue(binding.contains("WinRtRuntime.compose("))
        assertTrue(binding.contains("guidOf(\"33333333-3333-3333-3333-333333333333\")"))
        assertTrue(binding.contains("guidOf(\"22222222-2222-2222-2222-222222222222\")"))
        assertTrue(binding.contains("::DerivedWidget"))
        assertTrue(binding.contains("ComPtr.NULL"))
    }
}
