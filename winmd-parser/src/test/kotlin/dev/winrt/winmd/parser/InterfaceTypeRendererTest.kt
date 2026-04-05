package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.encodeValueTypeName
import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdParameter
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterfaceTypeRendererTest {
    private fun asyncRegistry(typeRegistry: TypeRegistry): AsyncMethodRuleRegistry {
        return AsyncMethodRuleRegistry(
            TypeNameMapper(),
            AsyncMethodProjectionPlanner(TypeNameMapper(), WinRtSignatureMapper(typeRegistry)),
            ProjectedObjectArgumentLowering(typeRegistry, WinRtSignatureMapper(typeRegistry), WinRtProjectionTypeMapper()),
        )
    }

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
            winRtSignatureMapper = WinRtSignatureMapper(typeRegistry),
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
            winRtSignatureMapper = WinRtSignatureMapper(typeRegistry),
            winRtProjectionTypeMapper = WinRtProjectionTypeMapper(),
        )

        val binding = renderer.render(typeRegistry.findType("IWidget", "Example.Core")!!).single().toString()

        assertTrue(binding.contains("visible"))
        assertTrue(binding.contains("invokeBooleanGetter(pointer, 6).getOrThrow()"))
    }

    @Test
    fun omits_explicit_property_accessor_methods_when_matching_property_is_projected() {
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
            winRtSignatureMapper = WinRtSignatureMapper(typeRegistry),
            winRtProjectionTypeMapper = WinRtProjectionTypeMapper(),
        )

        val binding = renderer.render(typeRegistry.findType("IWidget", "Example.Core")!!).single().toString()

        assertTrue(binding.contains("var title"))
        assertFalse(binding.contains("fun get_Title("))
        assertFalse(binding.contains("fun put_Title("))
    }

    @Test
    fun synthesizes_interface_properties_from_accessor_methods_when_property_metadata_is_missing() {
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
                            guid = "22222222-2222-2222-2222-222222222222",
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
            winRtSignatureMapper = WinRtSignatureMapper(typeRegistry),
            winRtProjectionTypeMapper = WinRtProjectionTypeMapper(),
        )

        val binding = renderer.render(typeRegistry.findType("IWidget", "Example.Core")!!).single().toString()

        assertTrue(binding.contains("var title"))
        assertTrue(binding.contains("val subtitle"))
        assertFalse(binding.contains("fun get_Title("))
        assertFalse(binding.contains("fun put_Title("))
        assertFalse(binding.contains("fun get_Subtitle("))
    }

    @Test
    fun does_not_project_enum_array_methods_as_scalar_enum_calls() {
        val model = WinMdModel(
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
                        WinMdType(
                            namespace = "Example.Core",
                            name = "ISpreadsheetItemProvider",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetAnnotationTypes",
                                    returnType = "Example.Core.AnnotationType[]",
                                    vtableIndex = 6,
                                ),
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
            winRtSignatureMapper = WinRtSignatureMapper(typeRegistry),
            winRtProjectionTypeMapper = WinRtProjectionTypeMapper(),
        )

        val binding = renderer.render(typeRegistry.findType("ISpreadsheetItemProvider", "Example.Core")!!).single().toString()

        assertFalse(binding.contains("fun getAnnotationTypes("))
        assertFalse(binding.contains("Array<AnnotationType>.fromValue"))
        assertFalse(binding.contains("invokeInt32Method(pointer, 6)"))
    }

    @Test
    fun renders_int32_pass_array_interface_methods_with_count_and_buffer_arguments() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValueStatics.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateInt32Array(value:Array<Int32>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,IntArray(value.size){index->value[index].value}).getOrThrow()"))
    }

    @Test
    fun renders_int32_receive_array_interface_methods_via_receive_array_helper() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IPropertyValue",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetInt32Array",
                                    returnType = "Int32[]",
                                    vtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValue.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetInt32Array():Array<Int32>"))
        assertTrue(binding.contains("invokeInt32ReceiveArrayMethod(pointer,6).getOrThrow().map{Int32(it)}.toTypedArray()"))
    }

    @Test
    fun renders_int32_fill_array_interface_methods_with_count_buffer_and_scalar_return() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorViewInt32",
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Collections/IVectorViewInt32.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetMany(startIndex:UInt32,items:Array<Int32>):UInt32"))
        assertTrue(binding.contains("UInt32(PlatformComInterop.invokeMethodWithResultKind(pointer,6,ComMethodResultKind.UINT32,startIndex.value,items.size,IntArray(items.size){index->items[index].value}).getOrThrow().requireUInt32())"))
    }

    @Test
    fun renders_int32_receive_array_interface_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorViewInt32",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "Int32[]",
                                    vtableIndex = 7,
                                    parameters = listOf(WinMdParameter("startIndex", "UInt32")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Collections/IVectorViewInt32.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<Int32>"))
        assertTrue(binding.contains("invokeInt32ReceiveArrayMethod(pointer,7,startIndex.value).getOrThrow().map{Int32(it)}.toTypedArray()"))
    }

    @Test
    fun unsubscribes_event_slots_with_finally_to_close_delegate_handles() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidget",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
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
                            name = "IWidgetOpenedEventArgs",
                            kind = WinMdTypeKind.Interface,
                            guid = "55555555-5555-5555-5555-555555555555",
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
            winRtSignatureMapper = WinRtSignatureMapper(typeRegistry),
            winRtProjectionTypeMapper = WinRtProjectionTypeMapper(),
        )

        val binding = renderer.render(typeRegistry.findType("IWidget", "Example.Xaml")!!).single().toString()

        assertTrue(binding.contains("finally"))
        assertTrue(binding.contains("delegateHandles.remove(token)?.close()"))
    }

    @Test
    fun renders_value_type_interface_properties_and_methods_with_struct_abi_calls() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Rect",
                            kind = WinMdTypeKind.Struct,
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IWidget",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            properties = listOf(
                                WinMdProperty("Bounds", "Rect", mutable = true, getterVtableIndex = 6, setterVtableIndex = 7),
                            ),
                            methods = listOf(
                                WinMdMethod("TransformBounds", "Rect", vtableIndex = 8),
                                WinMdMethod(
                                    "CreateOverlay",
                                    "Object",
                                    vtableIndex = 9,
                                    parameters = listOf(WinMdParameter("bounds", "Rect")),
                                ),
                                WinMdMethod(
                                    "SetBounds",
                                    "Unit",
                                    vtableIndex = 10,
                                    parameters = listOf(WinMdParameter("bounds", "Rect")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IWidget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("Rect.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,6,Rect.ABI_LAYOUT).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,7,value.toAbi()).getOrThrow()"))
        assertTrue(binding.contains("Rect.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,8,Rect.ABI_LAYOUT).getOrThrow())"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,9,bounds.toAbi()).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,10,bounds.toAbi()).getOrThrow()"))
    }

    @Test
    fun renders_nullable_value_type_interface_properties_via_property_value_boxing() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Rect",
                            kind = WinMdTypeKind.Struct,
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IWidget",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            properties = listOf(
                                WinMdProperty(
                                    "Bounds",
                                    "Windows.Foundation.IReference`1<Windows.Foundation.Rect>",
                                    mutable = true,
                                    getterVtableIndex = 6,
                                    setterVtableIndex = 7,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IWidget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("varbounds:Rect?"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethod(pointer,6).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getRect()}"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectSetter(pointer,7,if(value==null)ComPtr.NULLelsePropertyValue.createRect(value).pointer).getOrThrow()"))
    }

    @Test
    fun renders_small_scalar_interface_properties_and_methods_via_generic_abi() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IWidget",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            properties = listOf(
                                WinMdProperty(
                                    "Priority",
                                    "Int16",
                                    mutable = true,
                                    getterVtableIndex = 6,
                                    setterVtableIndex = 7,
                                ),
                                WinMdProperty(
                                    "OptionalPriority",
                                    "Windows.Foundation.IReference`1<Int16>",
                                    mutable = true,
                                    getterVtableIndex = 8,
                                    setterVtableIndex = 9,
                                ),
                            ),
                            methods = listOf(
                                WinMdMethod("GetShortcut", "Char16", vtableIndex = 10),
                                WinMdMethod(
                                    "CreateChild",
                                    "Object",
                                    vtableIndex = 11,
                                    parameters = listOf(WinMdParameter("priority", "Int16")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IWidget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("varpriority:Short"))
        assertTrue(binding.contains("PlatformComInterop.invokeMethodWithResultKind(pointer,6,ComMethodResultKind.INT16).getOrThrow().requireInt16()"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,7,value).getOrThrow()"))
        assertTrue(binding.contains("varoptionalPriority:Short?"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethod(pointer,8).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getInt16()}"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectSetter(pointer,9,if(value==null)ComPtr.NULLelsePropertyValue.createInt16(value).pointer).getOrThrow()"))
        assertTrue(binding.contains("fungetShortcut():Char"))
        assertTrue(binding.contains("PlatformComInterop.invokeMethodWithResultKind(pointer,10,ComMethodResultKind.CHAR16).getOrThrow().requireChar16()"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,11,priority).getOrThrow())"))
    }

    @Test
    fun renders_nullable_enum_interface_properties_via_generic_ireference_projection() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Contracts",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Contracts",
                            name = "Mode",
                            kind = WinMdTypeKind.Enum,
                            enumUnderlyingType = "UInt32",
                        ),
                        WinMdType(
                            namespace = "Example.Contracts",
                            name = "IWidget",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            properties = listOf(
                                WinMdProperty(
                                    "Mode",
                                    "Windows.Foundation.IReference`1<Example.Contracts.Mode>",
                                    mutable = true,
                                    getterVtableIndex = 6,
                                    setterVtableIndex = 7,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Example/Contracts/IWidget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("varmode:Mode?"))
        assertTrue(binding.contains("IReference.from<Mode>(Inspectable(it),\"enum(Example.Contracts.Mode;u4)\",\"Example.Contracts.Mode\")"))
        assertTrue(binding.contains("Mode.fromValue(PlatformComInterop.invokeUInt32Method(reference.pointer,6).getOrThrow())"))
        assertTrue(binding.contains("projectedObjectArgumentPointer("))
        assertTrue(binding.contains("\"pinterface({61c17706-2d65-11e0-9ae8-d48564015472};enum(Example.Contracts.Mode;u4))\""))
    }

    @Test
    fun renders_nullable_marked_external_struct_interface_properties_via_generic_ireference_projection() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidget",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            properties = listOf(
                                WinMdProperty(
                                    "FontWeight",
                                    "Windows.Foundation.IReference`1<${encodeValueTypeName("Windows.UI.Text.FontWeight")}>",
                                    mutable = true,
                                    getterVtableIndex = 6,
                                    setterVtableIndex = 7,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Example/Xaml/IWidget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("varfontWeight:FontWeight?"))
        assertTrue(binding.contains("IReference.from<FontWeight>(Inspectable(it),\"struct(Windows.UI.Text.FontWeight;u2)\",\"Windows.UI.Text.FontWeight\")"))
        assertTrue(binding.contains("FontWeight.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(reference.pointer,6,FontWeight.ABI_LAYOUT).getOrThrow())"))
        assertTrue(binding.contains("projectedObjectArgumentPointer("))
        assertTrue(binding.contains("\"pinterface({61c17706-2d65-11e0-9ae8-d48564015472};struct(Windows.UI.Text.FontWeight;u2))\""))
    }

    @Test
    fun renders_known_external_struct_interface_members_with_struct_abi_calls() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidget",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                            properties = listOf(
                                WinMdProperty(
                                    "KeyStatus",
                                    encodeValueTypeName("Windows.UI.Core.CorePhysicalKeyStatus"),
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                            methods = listOf(
                                WinMdMethod("GetWindowId", encodeValueTypeName("Microsoft.UI.WindowId"), vtableIndex = 7),
                                WinMdMethod(
                                    "Initialize",
                                    "Unit",
                                    vtableIndex = 8,
                                    parameters = listOf(WinMdParameter("parentWindowId", encodeValueTypeName("Microsoft.UI.WindowId"))),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Example/Xaml/IWidget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valkeyStatus:CorePhysicalKeyStatus"))
        assertTrue(binding.contains("CorePhysicalKeyStatus.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,6,CorePhysicalKeyStatus.ABI_LAYOUT).getOrThrow())"))
        assertTrue(binding.contains("WindowId.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,7,WindowId.ABI_LAYOUT).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,8,parentWindowId.toAbi()).getOrThrow()"))
    }
}
