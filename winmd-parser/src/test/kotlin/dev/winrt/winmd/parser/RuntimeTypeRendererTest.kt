package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdActivationKind
import dev.winrt.winmd.plugin.WinMdField
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdModel
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
    fun forwards_supported_string_array_methods_from_companion() {
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

        assertTrue(binding.contains("fun createStringArray("))
        assertTrue(binding.contains("statics.createStringArray(value)"))
    }

    @Test
    fun forwards_supported_object_pass_array_methods_from_companion() {
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
                            guid = "13131313-1313-1313-1313-131313131313",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateInspectableArray",
                                    returnType = "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("value", "Object[]", isIn = true)),
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

        assertTrue(binding.contains("fun createInspectableArray("))
        assertTrue(binding.contains("statics.createInspectableArray(value)"))
    }

    @Test
    fun forwards_supported_small_primitive_pass_array_methods_from_companion() {
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
                            guid = "14141414-1414-1414-1414-141414141414",
                            methods = listOf(
                                WinMdMethod("CreateBooleanArray", "Object", vtableIndex = 6, parameters = listOf(WinMdParameter("value", "Boolean[]", isIn = true))),
                                WinMdMethod("CreateUInt8Array", "Object", vtableIndex = 7, parameters = listOf(WinMdParameter("value", "UInt8[]", isIn = true))),
                                WinMdMethod("CreateInt16Array", "Object", vtableIndex = 8, parameters = listOf(WinMdParameter("value", "Int16[]", isIn = true))),
                                WinMdMethod("CreateUInt16Array", "Object", vtableIndex = 9, parameters = listOf(WinMdParameter("value", "UInt16[]", isIn = true))),
                                WinMdMethod("CreateChar16Array", "Object", vtableIndex = 10, parameters = listOf(WinMdParameter("value", "Char16[]", isIn = true))),
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

        assertTrue(binding.contains("fun createBooleanArray("))
        assertTrue(binding.contains("statics.createBooleanArray(value)"))
        assertTrue(binding.contains("fun createUInt8Array("))
        assertTrue(binding.contains("statics.createUInt8Array(value)"))
        assertTrue(binding.contains("fun createInt16Array("))
        assertTrue(binding.contains("statics.createInt16Array(value)"))
        assertTrue(binding.contains("fun createUInt16Array("))
        assertTrue(binding.contains("statics.createUInt16Array(value)"))
        assertTrue(binding.contains("fun createChar16Array("))
        assertTrue(binding.contains("statics.createChar16Array(value)"))
    }

    @Test
    fun forwards_supported_float32_pass_array_methods_from_companion() {
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
                            guid = "17171717-1717-1717-1717-171717171717",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateSingleArray",
                                    returnType = "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("value", "Float32[]", isIn = true)),
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

        assertTrue(binding.contains("fun createSingleArray("))
        assertTrue(binding.contains("statics.createSingleArray(value)"))
    }

    @Test
    fun forwards_supported_float64_pass_array_methods_from_companion() {
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
                            guid = "18181818-1818-1818-1818-181818181818",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateDoubleArray",
                                    returnType = "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("value", "Float64[]", isIn = true)),
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

        assertTrue(binding.contains("fun createDoubleArray("))
        assertTrue(binding.contains("statics.createDoubleArray(value)"))
    }

    @Test
    fun forwards_supported_uint32_pass_array_methods_from_companion() {
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
                            guid = "14141414-1414-1414-1414-141414141414",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateUInt32Array",
                                    returnType = "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("value", "UInt32[]", isIn = true)),
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

        assertTrue(binding.contains("fun createUInt32Array("))
        assertTrue(binding.contains("statics.createUInt32Array(value)"))
    }

    @Test
    fun forwards_supported_int64_pass_array_methods_from_companion() {
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
                            guid = "15151515-1515-1515-1515-151515151515",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateInt64Array",
                                    returnType = "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("value", "Int64[]", isIn = true)),
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

        assertTrue(binding.contains("fun createInt64Array("))
        assertTrue(binding.contains("statics.createInt64Array(value)"))
    }

    @Test
    fun forwards_supported_uint64_pass_array_methods_from_companion() {
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
                            guid = "16161616-1616-1616-1616-161616161616",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateUInt64Array",
                                    returnType = "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("value", "UInt64[]", isIn = true)),
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

        assertTrue(binding.contains("fun createUInt64Array("))
        assertTrue(binding.contains("statics.createUInt64Array(value)"))
    }

    @Test
    fun forwards_supported_datetime_pass_array_methods_from_companion() {
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
                            guid = "12121212-1212-1212-1212-121212121212",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateDateTimeArray",
                                    returnType = "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("value", "DateTime[]", isIn = true)),
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

        assertTrue(binding.contains("fun createDateTimeArray("))
        assertTrue(binding.contains("statics.createDateTimeArray(value)"))
    }

    @Test
    fun forwards_supported_timespan_pass_array_methods_with_scalar_inputs_from_companion() {
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
                            guid = "13131313-1313-1313-1313-131313131313",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateRange",
                                    returnType = "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter("startIndex", "UInt32"),
                                        WinMdParameter("value", "TimeSpan[]", isIn = true),
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
    fun forwards_supported_int64_receive_array_static_methods_from_companion() {
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
                            guid = "33333333-3333-3333-3333-333333333333",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "Int64[]",
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
    fun forwards_supported_uint64_receive_array_static_methods_from_companion() {
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
                            guid = "44444444-4444-4444-4444-444444444444",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "UInt64[]",
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
    fun forwards_supported_string_receive_array_static_methods_from_companion() {
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
                            guid = "55555555-5555-5555-5555-555555555555",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "String[]",
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
    fun forwards_supported_float32_receive_array_static_methods_from_companion() {
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
                            guid = "66666666-6666-6666-6666-666666666666",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "Float32[]",
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
    fun forwards_supported_float64_receive_array_static_methods_from_companion() {
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
                            guid = "77777777-7777-7777-7777-777777777777",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "Float64[]",
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
    fun forwards_supported_small_primitive_receive_array_static_methods_from_companion() {
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
                            guid = "88888888-8888-8888-8888-888888888888",
                            methods = listOf(
                                WinMdMethod("GetBytes", "UInt8[]", vtableIndex = 6, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetShorts", "Int16[]", vtableIndex = 7, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetUShorts", "UInt16[]", vtableIndex = 8, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetChars", "Char16[]", vtableIndex = 9, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetFlags", "Boolean[]", vtableIndex = 10, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
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

        assertTrue(binding.contains("fun getBytes("))
        assertTrue(binding.contains("statics.getBytes(startIndex)"))
        assertTrue(binding.contains("fun getShorts("))
        assertTrue(binding.contains("statics.getShorts(startIndex)"))
        assertTrue(binding.contains("fun getUShorts("))
        assertTrue(binding.contains("statics.getUShorts(startIndex)"))
        assertTrue(binding.contains("fun getChars("))
        assertTrue(binding.contains("statics.getChars(startIndex)"))
        assertTrue(binding.contains("fun getFlags("))
        assertTrue(binding.contains("statics.getFlags(startIndex)"))
    }

    @Test
    fun forwards_supported_value_conversion_receive_array_static_methods_from_companion() {
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
                            guid = "99999999-9999-9999-9999-999999999999",
                            methods = listOf(
                                WinMdMethod("GetGuids", "Guid[]", vtableIndex = 6, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetDates", "DateTime[]", vtableIndex = 7, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetDurations", "TimeSpan[]", vtableIndex = 8, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
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

        assertTrue(binding.contains("fun getGuids("))
        assertTrue(binding.contains("statics.getGuids(startIndex)"))
        assertTrue(binding.contains("fun getDates("))
        assertTrue(binding.contains("statics.getDates(startIndex)"))
        assertTrue(binding.contains("fun getDurations("))
        assertTrue(binding.contains("statics.getDurations(startIndex)"))
    }

    @Test
    fun forwards_supported_object_receive_array_static_methods_from_companion() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "InspectablePropertyValue",
                            kind = WinMdTypeKind.RuntimeClass,
                            staticInterfaces = listOf("Windows.Foundation.IInspectableArrayStatics"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IInspectableArrayStatics",
                            kind = WinMdTypeKind.Interface,
                            guid = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                            methods = listOf(
                                WinMdMethod("GetInspectables", "Object[]", vtableIndex = 6, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
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

        val binding = renderer.render(typeRegistry.findType("InspectablePropertyValue", "Windows.Foundation")!!).toString()

        assertTrue(binding.contains("fun getInspectables("))
        assertTrue(binding.contains("statics.getInspectables(startIndex)"))
    }

    @Test
    fun forwards_supported_struct_receive_array_static_methods_from_companion() {
        val model = WinMdModel(
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
                                WinMdField("X", "Float32"),
                                WinMdField("Y", "Float32"),
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Size",
                            kind = WinMdTypeKind.Struct,
                            fields = listOf(
                                WinMdField("Width", "Float32"),
                                WinMdField("Height", "Float32"),
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Rect",
                            kind = WinMdTypeKind.Struct,
                            fields = listOf(
                                WinMdField("X", "Float32"),
                                WinMdField("Y", "Float32"),
                                WinMdField("Width", "Float32"),
                                WinMdField("Height", "Float32"),
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "StructPropertyValue",
                            kind = WinMdTypeKind.RuntimeClass,
                            staticInterfaces = listOf("Windows.Foundation.IStructArrayStatics"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IStructArrayStatics",
                            kind = WinMdTypeKind.Interface,
                            guid = "dddddddd-dddd-dddd-dddd-dddddddddddd",
                            methods = listOf(
                                WinMdMethod("GetPoints", "Point[]", vtableIndex = 6, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetSizes", "Size[]", vtableIndex = 7, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetRects", "Rect[]", vtableIndex = 8, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
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

        val binding = renderer.render(typeRegistry.findType("StructPropertyValue", "Windows.Foundation")!!).toString()

        assertTrue(binding.contains("fun getPoints("))
        assertTrue(binding.contains("statics.getPoints(startIndex)"))
        assertTrue(binding.contains("fun getSizes("))
        assertTrue(binding.contains("statics.getSizes(startIndex)"))
        assertTrue(binding.contains("fun getRects("))
        assertTrue(binding.contains("statics.getRects(startIndex)"))
    }

    @Test
    fun forwards_supported_temporal_receive_array_static_methods_with_temporal_array_inputs_from_companion() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "TemporalPropertyValue",
                            kind = WinMdTypeKind.RuntimeClass,
                            staticInterfaces = listOf("Windows.Foundation.ITemporalArrayStatics"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "ITemporalArrayStatics",
                            kind = WinMdTypeKind.Interface,
                            guid = "ffffffff-ffff-ffff-ffff-ffffffffffff",
                            methods = listOf(
                                WinMdMethod("GetAndSetDateTimes", "DateTime[]", vtableIndex = 6, parameters = listOf(WinMdParameter("dateTimes", "DateTime[]", isIn = true))),
                                WinMdMethod("GetAndSetDurations", "TimeSpan[]", vtableIndex = 7, parameters = listOf(WinMdParameter("durations", "TimeSpan[]", isIn = true))),
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

        val binding = renderer.render(typeRegistry.findType("TemporalPropertyValue", "Windows.Foundation")!!).toString()

        assertTrue(binding.contains("fun getAndSetDateTimes("))
        assertTrue(binding.contains("statics.getAndSetDateTimes(dateTimes)"))
        assertTrue(binding.contains("fun getAndSetDurations("))
        assertTrue(binding.contains("statics.getAndSetDurations(durations)"))
    }

    @Test
    fun forwards_runtime_class_receive_array_static_methods_with_runtime_class_array_inputs_from_companion() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Uri",
                            kind = WinMdTypeKind.RuntimeClass,
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "UriPropertyValue",
                            kind = WinMdTypeKind.RuntimeClass,
                            staticInterfaces = listOf("Windows.Foundation.IUriArrayStatics"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IUriArrayStatics",
                            kind = WinMdTypeKind.Interface,
                            guid = "13131313-1313-1313-1313-131313131313",
                            methods = listOf(
                                WinMdMethod("GetAndSetUris", "Uri[]", vtableIndex = 6, parameters = listOf(WinMdParameter("uris", "Uri[]", isIn = true))),
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

        val binding = renderer.render(typeRegistry.findType("UriPropertyValue", "Windows.Foundation")!!).toString()

        assertTrue(binding.contains("fun getAndSetUris("))
        assertTrue(binding.contains("statics.getAndSetUris(uris)"))
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
    fun renders_runtime_factory_constructors_for_supported_uint32_array_parameters() {
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
                                    name = "CreateWithIds",
                                    returnType = "Example.Xaml.Widget",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "values", type = "UInt32[]", isIn = true),
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
            normalizedBinding.contains("constructor(values: kotlin.Array<dev.winrt.core.UInt32>)"),
        )
        assertTrue(
            normalizedBinding,
            normalizedBinding.contains("factoryCreateWithIds(values).pointer"),
        )
    }

    @Test
    fun renders_runtime_factory_constructors_for_supported_int64_array_parameters() {
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
                                    name = "CreateWithTicks",
                                    returnType = "Example.Xaml.Widget",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "values", type = "Int64[]", isIn = true),
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
            normalizedBinding.contains("constructor(values: kotlin.Array<dev.winrt.core.Int64>)"),
        )
        assertTrue(
            normalizedBinding,
            normalizedBinding.contains("factoryCreateWithTicks(values).pointer"),
        )
    }

    @Test
    fun renders_runtime_factory_constructors_for_supported_uint64_array_parameters() {
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
                                    name = "CreateWithUnsignedTicks",
                                    returnType = "Example.Xaml.Widget",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "values", type = "UInt64[]", isIn = true),
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
            normalizedBinding.contains("constructor(values: kotlin.Array<dev.winrt.core.UInt64>)"),
        )
        assertTrue(
            normalizedBinding,
            normalizedBinding.contains("factoryCreateWithUnsignedTicks(values).pointer"),
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
    fun renders_runtime_factory_constructors_for_supported_string_array_parameters() {
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
        val normalizedBinding = binding.replace(Regex("\\s+"), " ")

        assertTrue(
            normalizedBinding,
            normalizedBinding.contains("constructor(labels: kotlin.Array<kotlin.String>)"),
        )
        assertTrue(
            normalizedBinding,
            normalizedBinding.contains("factoryCreateWithLabels(labels).pointer"),
        )
    }

    @Test
    fun renders_runtime_factory_constructors_for_supported_object_array_parameters() {
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
                                    name = "CreateWithInspectables",
                                    returnType = "Example.Xaml.Widget",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "values", type = "Object[]", isIn = true),
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
            normalizedBinding.contains("constructor(values: kotlin.Array<dev.winrt.core.Inspectable>)"),
        )
        assertTrue(
            normalizedBinding,
            normalizedBinding.contains("factoryCreateWithInspectables(values).pointer"),
        )
    }

    @Test
    fun renders_runtime_factory_constructors_for_supported_small_primitive_array_parameters() {
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
                                WinMdMethod("CreateWithFlags", "Example.Xaml.Widget", vtableIndex = 6, parameters = listOf(WinMdParameter(name = "flags", type = "Boolean[]", isIn = true))),
                                WinMdMethod("CreateWithBytes", "Example.Xaml.Widget", vtableIndex = 7, parameters = listOf(WinMdParameter(name = "bytes", type = "UInt8[]", isIn = true))),
                                WinMdMethod("CreateWithShorts", "Example.Xaml.Widget", vtableIndex = 8, parameters = listOf(WinMdParameter(name = "shorts", type = "Int16[]", isIn = true))),
                                WinMdMethod("CreateWithUShorts", "Example.Xaml.Widget", vtableIndex = 9, parameters = listOf(WinMdParameter(name = "uShorts", type = "UInt16[]", isIn = true))),
                                WinMdMethod("CreateWithChars", "Example.Xaml.Widget", vtableIndex = 10, parameters = listOf(WinMdParameter(name = "chars", type = "Char16[]", isIn = true))),
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

        assertTrue(normalizedBinding.contains("constructor(flags: kotlin.Array<dev.winrt.core.WinRtBoolean>)"))
        assertTrue(normalizedBinding.contains("factoryCreateWithFlags(flags).pointer"))
        assertTrue(normalizedBinding.contains("constructor(bytes: kotlin.Array<kotlin.UByte>)"))
        assertTrue(normalizedBinding.contains("factoryCreateWithBytes(bytes).pointer"))
        assertTrue(normalizedBinding.contains("constructor(shorts: kotlin.Array<kotlin.Short>)"))
        assertTrue(normalizedBinding.contains("factoryCreateWithShorts(shorts).pointer"))
        assertTrue(normalizedBinding.contains("constructor(uShorts: kotlin.Array<kotlin.UShort>)"))
        assertTrue(normalizedBinding.contains("factoryCreateWithUShorts(uShorts).pointer"))
        assertTrue(normalizedBinding.contains("constructor(chars: kotlin.Array<kotlin.Char>)"))
        assertTrue(normalizedBinding.contains("factoryCreateWithChars(chars).pointer"))
    }

    @Test
    fun renders_runtime_factory_constructors_for_supported_float32_array_parameters() {
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
                                    name = "CreateWithSingles",
                                    returnType = "Example.Xaml.Widget",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "values", type = "Float32[]", isIn = true),
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
            normalizedBinding.contains("constructor(values: kotlin.Array<dev.winrt.core.Float32>)"),
        )
        assertTrue(
            normalizedBinding,
            normalizedBinding.contains("factoryCreateWithSingles(values).pointer"),
        )
    }

    @Test
    fun renders_runtime_factory_constructors_for_supported_float64_array_parameters() {
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
                                    name = "CreateWithDoubles",
                                    returnType = "Example.Xaml.Widget",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "values", type = "Float64[]", isIn = true),
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
            normalizedBinding.contains("constructor(values: kotlin.Array<dev.winrt.core.Float64>)"),
        )
        assertTrue(
            normalizedBinding,
            normalizedBinding.contains("factoryCreateWithDoubles(values).pointer"),
        )
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
