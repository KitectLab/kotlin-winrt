package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdActivationKind
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdParameter
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeTypeRendererTest {
    private fun asyncRegistry(typeRegistry: TypeRegistry): AsyncMethodRuleRegistry {
        return AsyncMethodRuleRegistry(
            TypeNameMapper(),
            AsyncMethodProjectionPlanner(TypeNameMapper(), WinRtSignatureMapper(typeRegistry)),
            ProjectedObjectArgumentLowering(typeRegistry, WinRtSignatureMapper(typeRegistry), WinRtProjectionTypeMapper()),
        )
    }

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
    fun does_not_forward_unsupported_string_array_methods_from_companion() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "PropertyValue",
                            kind = WinMdTypeKind.RuntimeClass,
                            staticInterfaces = listOf("Windows.Foundation.IPropertyValueStatics"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IPropertyValueStatics",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateStringArray",
                                    returnType = "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("value", "String[]", isIn = true)),
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
                asyncRegistry(typeRegistry),
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

        val binding = renderer.render(typeRegistry.findType("PropertyValue", "Windows.Foundation")!!).toString()

        assertFalse(binding.contains("fun createStringArray("))
        assertFalse(binding.contains("statics.createStringArray"))
    }

    @Test
    fun forwards_supported_int32_pass_array_methods_from_companion() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "PropertyValue",
                            kind = WinMdTypeKind.RuntimeClass,
                            staticInterfaces = listOf("Windows.Foundation.IPropertyValueStatics"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IPropertyValueStatics",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateInt32Array",
                                    returnType = "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("value", "Int32[]", isIn = true)),
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
                asyncRegistry(typeRegistry),
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

        val binding = renderer.render(typeRegistry.findType("PropertyValue", "Windows.Foundation")!!).toString()

        assertTrue(binding.contains("fun createInt32Array("))
        assertTrue(binding.contains("statics.createInt32Array(value)"))
    }

    @Test
    fun forwards_supported_int32_receive_array_static_methods_from_companion() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "PropertyValue",
                            kind = WinMdTypeKind.RuntimeClass,
                            staticInterfaces = listOf("Windows.Foundation.IPropertyValueStatics"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IPropertyValueStatics",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "Int32[]",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("startIndex", "UInt32")),
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
                asyncRegistry(typeRegistry),
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

        val binding = renderer.render(typeRegistry.findType("PropertyValue", "Windows.Foundation")!!).toString()

        assertTrue(binding.contains("fun getRange("))
        assertTrue(binding.contains("statics.getRange(startIndex)"))
    }

    @Test
    fun forwards_supported_uint32_receive_array_static_methods_from_companion() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "PropertyValue",
                            kind = WinMdTypeKind.RuntimeClass,
                            staticInterfaces = listOf("Windows.Foundation.IPropertyValueStatics"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IPropertyValueStatics",
                            kind = WinMdTypeKind.Interface,
                            guid = "22222222-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "UInt32[]",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("startIndex", "UInt32")),
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
                asyncRegistry(typeRegistry),
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

        val binding = renderer.render(typeRegistry.findType("PropertyValue", "Windows.Foundation")!!).toString()

        assertTrue(binding.contains("fun getRange("))
        assertTrue(binding.contains("statics.getRange(startIndex)"))
    }

    @Test
    fun forwards_supported_int32_fill_array_static_methods_from_companion() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "PropertyValue",
                            kind = WinMdTypeKind.RuntimeClass,
                            staticInterfaces = listOf("Windows.Foundation.IPropertyValueStatics"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IPropertyValueStatics",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetMany",
                                    returnType = "UInt32",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter("startIndex", "UInt32"),
                                        WinMdParameter("items", "Int32[]", isOut = true),
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
                asyncRegistry(typeRegistry),
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

        val binding = renderer.render(typeRegistry.findType("PropertyValue", "Windows.Foundation")!!).toString()

        assertTrue(binding.contains("fun getMany("))
        assertTrue(binding.contains("statics.getMany(startIndex, items)"))
    }

    @Test
    fun forwards_supported_int32_pass_array_static_methods_with_scalar_inputs_from_companion() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "PropertyValue",
                            kind = WinMdTypeKind.RuntimeClass,
                            staticInterfaces = listOf("Windows.Foundation.IPropertyValueStatics"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IPropertyValueStatics",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateRange",
                                    returnType = "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter("startIndex", "UInt32"),
                                        WinMdParameter("value", "Int32[]", isIn = true),
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
                asyncRegistry(typeRegistry),
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

        val binding = renderer.render(typeRegistry.findType("PropertyValue", "Windows.Foundation")!!).toString()

        assertTrue(binding.contains("fun createRange("))
        assertTrue(binding.contains("statics.createRange(startIndex, value)"))
    }

    @Test
    fun renders_runtime_factory_constructors_for_supported_int32_pass_array_parameters_with_scalars() {
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
                                    name = "CreateWithRange",
                                    returnType = "Example.Xaml.Widget",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "startIndex", type = "UInt32"),
                                        WinMdParameter(name = "values", type = "Int32[]", isIn = true),
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
                asyncRegistry(typeRegistry),
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
        val normalizedBinding = binding.replace(Regex("\\s+"), " ")

        assertTrue(
            normalizedBinding,
            normalizedBinding.contains(
                "constructor(startIndex: dev.winrt.core.UInt32, values: kotlin.Array<dev.winrt.core.Int32>)",
            ),
        )
        assertTrue(
            normalizedBinding,
            normalizedBinding.contains("factoryCreateWithRange(startIndex, values).pointer"),
        )
    }

    @Test
    fun omits_explicit_property_accessor_methods_when_matching_runtime_property_is_projected() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Core",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Core",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Example.Core.IWidget",
                            properties = listOf(
                                WinMdProperty("Title", "String", mutable = true, getterVtableIndex = 6, setterVtableIndex = 7),
                            ),
                            methods = listOf(
                                WinMdMethod("get_Title", "String", vtableIndex = 6),
                                WinMdMethod(
                                    "put_Title",
                                    "Unit",
                                    vtableIndex = 7,
                                    parameters = listOf(WinMdParameter("value", "String")),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Core",
                            name = "IWidget",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
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
                asyncRegistry(typeRegistry),
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

        val binding = renderer.render(typeRegistry.findType("Widget", "Example.Core")!!).toString()

        assertTrue(binding.contains("var title"))
        assertFalse(binding.contains("fun get_Title("))
        assertFalse(binding.contains("fun put_Title("))
    }

    @Test
    fun synthesizes_runtime_properties_from_accessor_methods_when_property_metadata_is_missing() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Core",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Core",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Example.Core.IWidget",
                            methods = listOf(
                                WinMdMethod("get_Title", "String", vtableIndex = 6),
                                WinMdMethod(
                                    "put_Title",
                                    "Unit",
                                    vtableIndex = 7,
                                    parameters = listOf(WinMdParameter("value", "String")),
                                ),
                                WinMdMethod("get_Subtitle", "String", vtableIndex = 8),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Core",
                            name = "IWidget",
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

        val binding = renderer.render(typeRegistry.findType("Widget", "Example.Core")!!).toString()

        assertTrue(binding.contains("var title"))
        assertTrue(binding.contains("val subtitle"))
        assertFalse(binding.contains("fun get_Title("))
        assertFalse(binding.contains("fun put_Title("))
        assertFalse(binding.contains("fun get_Subtitle("))
    }

    @Test
    fun renders_composable_runtime_classes_with_composable_default_constructor_and_helpers() {
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
                            activationKind = WinMdActivationKind.Composable,
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
                                        WinMdParameter(name = "baseInterface", type = "Object"),
                                        WinMdParameter(name = "innerInterface", type = "Object", byRef = true, isOut = true),
                                    ),
                                ),
                                WinMdMethod(
                                    name = "CreateInstance",
                                    returnType = "Example.Xaml.Widget",
                                    vtableIndex = 7,
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

        assertTrue(binding.contains("constructor() : this(Companion.factoryCreateInstance().pointer)"))
        assertTrue(binding.contains("factoryCreateInstance(label).pointer"))
        assertFalse(binding.contains("baseInterface"))
        assertFalse(binding.contains("innerInterface"))
        assertTrue(binding.contains("WinRtActivationKind.Composable"))
        assertFalse(binding.contains("Companion.activate().pointer"))
        assertFalse(binding.contains("WinRtRuntime.activate(this, ::Widget)"))
        assertTrue(binding.contains("WinRtRuntime.compose("))
        assertTrue(binding.contains("guidOf(\"22222222-2222-2222-2222-222222222222\")"))
        assertTrue(binding.contains("::Widget"))
        assertTrue(binding.contains("ComPtr.NULL"))
    }

    @Test
    fun does_not_render_derived_runtime_classes_with_inherited_composable_constructor() {
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

        assertFalse(binding.contains("constructor() : this("))
        assertTrue(binding.contains("WinRtActivationKind.Factory"))
        assertFalse(binding.contains("WinRtRuntime.activate(this, ::DerivedWidget)"))
        assertFalse(binding.contains("WinRtRuntime.compose("))
        assertFalse(binding.contains("BaseWidget.Companion"))
        assertFalse(binding.contains("guidOf(\"33333333-3333-3333-3333-333333333333\")"))
        assertFalse(binding.contains("::DerivedWidget"))
        assertFalse(binding.contains("ComPtr.NULL"))
    }

    @Test
    fun does_not_render_runtime_factory_constructors_for_unsupported_array_parameters() {
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
                                    name = "CreateWithLabels",
                                    returnType = "Example.Xaml.Widget",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "labels", type = "String[]", isIn = true),
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
                asyncRegistry(typeRegistry),
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

        assertFalse(binding.contains("constructor(labels: Array<String>)"))
        assertFalse(binding.contains("factoryCreateWithLabels"))
    }

    @Test
    fun renders_activatable_resource_dictionary_derived_runtime_classes_with_activate_constructor() {
        val model = WinMdModel(
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

        val binding = renderer.render(typeRegistry.findType("XamlControlsResources", "Microsoft.UI.Xaml.Controls")!!).toString()

        assertTrue(binding.contains("constructor() : this(Companion.activate().pointer)"))
        assertTrue(binding.contains("WinRtActivationKind.Factory"))
        assertTrue(binding.contains("WinRtRuntime.activate(this, ::XamlControlsResources)"))
        assertFalse(binding.contains("ResourceDictionary.Companion"))
        assertFalse(binding.contains("WinRtRuntime.compose("))
        assertFalse(binding.contains("guidOf(\"22222222-2222-2222-2222-222222222222\")"))
    }
}
