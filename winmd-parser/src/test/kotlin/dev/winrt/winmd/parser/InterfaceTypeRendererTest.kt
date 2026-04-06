package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.encodeValueTypeName
import dev.winrt.winmd.plugin.WinMdField
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
        assertTrue(binding.contains("valitemsAbi=IntArray(items.size){index->items[index].value}"))
        assertTrue(binding.contains("PlatformComInterop.invokeMethodWithResultKind(pointer,6,ComMethodResultKind.UINT32,startIndex.value,items.size,itemsAbi).getOrThrow().requireUInt32()"))
        assertTrue(binding.contains("itemsAbi.forEachIndexed{index,value->items[index]=Int32(value)}"))
    }

    @Test
    fun renders_int32_pass_array_interface_methods_with_scalar_inputs() {
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValueStatics.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateRange(startIndex:UInt32,value:Array<Int32>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,startIndex.value,value.size,IntArray(value.size){index->value[index].value}).getOrThrow()"))
    }

    @Test
    fun renders_int32_pass_array_interface_methods_with_value_type_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.UI",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.UI",
                            name = "Color",
                            kind = WinMdTypeKind.Struct,
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Windows.Devices.Lights",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Devices.Lights",
                            name = "ILampArray",
                            kind = WinMdTypeKind.Interface,
                            guid = "22222222-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetSingleColorForIndices",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter("desiredColor", "Windows.UI.Color"),
                                        WinMdParameter("lampIndices", "Int32[]", isIn = true),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Devices/Lights/ILampArray.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funsetSingleColorForIndices(desiredColor:Color,lampIndices:Array<Int32>)"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,6,desiredColor.toAbi(),lampIndices.size,IntArray(lampIndices.size){index->lampIndices[index].value}).getOrThrow()"))
    }

    @Test
    fun renders_uint8_receive_array_interface_methods_with_value_type_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.UI",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.UI",
                            name = "Color",
                            kind = WinMdTypeKind.Struct,
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Windows.Devices.Lights",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Devices.Lights",
                            name = "ILampArrayDataSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "23232323-2323-2323-2323-232323232323",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GenerateData",
                                    returnType = "UInt8[]",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter("desiredColor", "Windows.UI.Color"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Devices/Lights/ILampArrayDataSource.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungenerateData(desiredColor:Color):Array<UByte>"))
        assertTrue(binding.contains("invokeUInt8ReceiveArrayMethod(pointer,6,desiredColor.toAbi()).getOrThrow().map{it.toUByte()}.toTypedArray()"))
    }

    @Test
    fun renders_uint8_receive_array_interface_methods_with_guid_and_encoded_enum_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IPropertySet",
                            kind = WinMdTypeKind.Interface,
                            guid = "34343434-3434-3434-3434-343434343434",
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Windows.Media.Protection.PlayReady",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Media.Protection.PlayReady",
                            name = "PlayReadyITADataFormat",
                            kind = WinMdTypeKind.Enum,
                            enumUnderlyingType = "Int32",
                        ),
                        WinMdType(
                            namespace = "Windows.Media.Protection.PlayReady",
                            name = "IPlayReadyITADataGenerator",
                            kind = WinMdTypeKind.Interface,
                            guid = "35353535-3535-3535-3535-353535353535",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GenerateData",
                                    returnType = "UInt8[]",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter("guidCPSystemId", encodeValueTypeName("System.Guid")),
                                        WinMdParameter("countOfStreams", "UInt32"),
                                        WinMdParameter("configuration", "Windows.Foundation.Collections.IPropertySet"),
                                        WinMdParameter("format", encodeValueTypeName("Windows.Media.Protection.PlayReady.PlayReadyITADataFormat")),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Media/Protection/PlayReady/IPlayReadyITADataGenerator.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding, binding.contains("fungenerateData(guidCPSystemId:Uuid,countOfStreams:UInt32,configuration:IPropertySet,format:PlayReadyITADataFormat,):Array<UByte>"))
        assertTrue(binding.contains("invokeUInt8ReceiveArrayMethod(pointer,6,guidOf(guidCPSystemId.toString()),countOfStreams.value,"))
        assertTrue(binding.contains(",format.value).getOrThrow().map{it.toUByte()}.toTypedArray()"))
    }

    @Test
    fun renders_string_pass_array_interface_methods_with_count_and_buffer_arguments() {
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
                            guid = "12121212-1212-1212-1212-121212121212",
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValueStatics.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateStringArray(value:Array<String>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,value).getOrThrow()"))
    }

    @Test
    fun renders_string_pass_array_interface_methods_with_scalar_inputs() {
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
                            guid = "13131313-1313-1313-1313-131313131313",
                            methods = listOf(
                                WinMdMethod(
                                    name = "CreateRange",
                                    returnType = "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter("startIndex", "UInt32"),
                                        WinMdParameter("value", "String[]", isIn = true),
                                    ),
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

        assertTrue(binding.contains("funcreateRange(startIndex:UInt32,value:Array<String>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,startIndex.value,value.size,value).getOrThrow()"))
    }

    @Test
    fun renders_object_pass_array_interface_methods_with_count_and_buffer_arguments() {
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
                            guid = "15151515-1515-1515-1515-151515151515",
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValueStatics.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateInspectableArray(value:Array<Inspectable>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,Array(value.size){index->value[index].pointer}).getOrThrow()"))
    }

    @Test
    fun renders_guid_and_struct_pass_array_interface_methods_with_count_and_buffer_arguments() {
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
                            guid = "15151515-1515-1515-1515-151515151515",
                            methods = listOf(
                                WinMdMethod("CreateGuidArray", "Object", vtableIndex = 6, parameters = listOf(WinMdParameter("value", "Guid[]", isIn = true))),
                                WinMdMethod("CreatePointArray", "Object", vtableIndex = 7, parameters = listOf(WinMdParameter("value", "Point[]", isIn = true))),
                                WinMdMethod("CreateSizeArray", "Object", vtableIndex = 8, parameters = listOf(WinMdParameter("value", "Size[]", isIn = true))),
                                WinMdMethod("CreateRectArray", "Object", vtableIndex = 9, parameters = listOf(WinMdParameter("value", "Rect[]", isIn = true))),
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

        assertTrue(binding.contains("funcreateGuidArray(value:Array<Uuid>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,ByteArray(value.size*16){index->valguid=guidOf(value[index/16].toString());when(index%16){0->guid.data1.toByte();1->(guid.data1shr8).toByte();2->(guid.data1shr16).toByte();3->(guid.data1shr24).toByte();4->guid.data2.toByte();5->(guid.data2.toInt()shr8).toByte();6->guid.data3.toByte();7->(guid.data3.toInt()shr8).toByte();else->guid.data4[index%16-8]}}).getOrThrow()"))
        assertTrue(binding.contains("funcreatePointArray(value:Array<Point>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,7,value.size,FloatArray(value.size*2){index->valitem=value[index/2];if(index%2==0)item.xelseitem.y}).getOrThrow()"))
        assertTrue(binding.contains("funcreateSizeArray(value:Array<Size>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,8,value.size,FloatArray(value.size*2){index->valitem=value[index/2];if(index%2==0)item.widthelseitem.height}).getOrThrow()"))
        assertTrue(binding.contains("funcreateRectArray(value:Array<Rect>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,9,value.size,FloatArray(value.size*4){index->valitem=value[index/4];when(index%4){0->item.x;1->item.y;2->item.width;else->item.height}}).getOrThrow()"))
    }

    @Test
    fun renders_small_primitive_pass_array_interface_methods_with_count_and_buffer_arguments() {
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
                            guid = "16161616-1616-1616-1616-161616161616",
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValueStatics.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateBooleanArray(value:Array<WinRtBoolean>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,ByteArray(value.size){index->if(value[index].value)1.toByte()else0.toByte()}).getOrThrow()"))
        assertTrue(binding.contains("funcreateUInt8Array(value:Array<UByte>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,7,value.size,ByteArray(value.size){index->value[index].toByte()}).getOrThrow()"))
        assertTrue(binding.contains("funcreateInt16Array(value:Array<Short>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,8,value.size,ShortArray(value.size){index->value[index]}).getOrThrow()"))
        assertTrue(binding.contains("funcreateUInt16Array(value:Array<UShort>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,9,value.size,ShortArray(value.size){index->value[index].toShort()}).getOrThrow()"))
        assertTrue(binding.contains("funcreateChar16Array(value:Array<Char>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,10,value.size,CharArray(value.size){index->value[index]}).getOrThrow()"))
    }

    @Test
    fun renders_float32_pass_array_interface_methods_with_count_and_buffer_arguments() {
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
                            guid = "19191919-1919-1919-1919-191919191919",
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValueStatics.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateSingleArray(value:Array<Float32>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,FloatArray(value.size){index->value[index].value}).getOrThrow()"))
    }

    @Test
    fun renders_float64_pass_array_interface_methods_with_count_and_buffer_arguments() {
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
                            guid = "20202020-2020-2020-2020-202020202020",
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValueStatics.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateDoubleArray(value:Array<Float64>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,DoubleArray(value.size){index->value[index].value}).getOrThrow()"))
    }

    @Test
    fun renders_uint32_pass_array_interface_methods_with_count_and_buffer_arguments() {
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
                            guid = "16161616-1616-1616-1616-161616161616",
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValueStatics.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateUInt32Array(value:Array<UInt32>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,IntArray(value.size){index->value[index].value.toInt()}).getOrThrow()"))
    }

    @Test
    fun renders_int64_pass_array_interface_methods_with_count_and_buffer_arguments() {
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
                            guid = "17171717-1717-1717-1717-171717171717",
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValueStatics.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateInt64Array(value:Array<Int64>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,LongArray(value.size){index->value[index].value}).getOrThrow()"))
    }

    @Test
    fun renders_uint64_pass_array_interface_methods_with_count_and_buffer_arguments() {
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
                            guid = "18181818-1818-1818-1818-181818181818",
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValueStatics.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateUInt64Array(value:Array<UInt64>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,LongArray(value.size){index->value[index].value.toLong()}).getOrThrow()"))
    }

    @Test
    fun renders_datetime_pass_array_interface_methods_with_count_and_buffer_arguments() {
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
                            guid = "14141414-1414-1414-1414-141414141414",
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValueStatics.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateDateTimeArray(value:Array<Instant>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,LongArray(value.size){index->(((value[index].epochSeconds*10000000L)+(value[index].nanosecondsOfSecond/100))+116_444_736_000_000_000)}).getOrThrow()"))
    }

    @Test
    fun renders_timespan_pass_array_interface_methods_with_scalar_inputs() {
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
                            guid = "15151515-1515-1515-1515-151515151515",
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

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IPropertyValueStatics.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateRange(startIndex:UInt32,value:Array<Duration>):Inspectable"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,startIndex.value,value.size,LongArray(value.size){index->(value[index].inWholeNanoseconds/100)}).getOrThrow()"))
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
    fun renders_uint32_receive_array_interface_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorViewUInt32",
                            kind = WinMdTypeKind.Interface,
                            guid = "22222222-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "UInt32[]",
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
            .first { it.relativePath == "Windows/Foundation/Collections/IVectorViewUInt32.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<UInt32>"))
        assertTrue(binding.contains("invokeUInt32ReceiveArrayMethod(pointer,7,startIndex.value).getOrThrow().map{UInt32(it)}.toTypedArray()"))
    }

    @Test
    fun renders_int64_receive_array_interface_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorViewInt64",
                            kind = WinMdTypeKind.Interface,
                            guid = "33333333-3333-3333-3333-333333333333",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "Int64[]",
                                    vtableIndex = 8,
                                    parameters = listOf(WinMdParameter("startIndex", "UInt32")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Collections/IVectorViewInt64.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<Int64>"))
        assertTrue(binding.contains("invokeInt64ReceiveArrayMethod(pointer,8,startIndex.value).getOrThrow().map{Int64(it)}.toTypedArray()"))
    }

    @Test
    fun renders_uint64_receive_array_interface_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorViewUInt64",
                            kind = WinMdTypeKind.Interface,
                            guid = "44444444-4444-4444-4444-444444444444",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "UInt64[]",
                                    vtableIndex = 9,
                                    parameters = listOf(WinMdParameter("startIndex", "UInt32")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Collections/IVectorViewUInt64.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<UInt64>"))
        assertTrue(binding.contains("invokeUInt64ReceiveArrayMethod(pointer,9,startIndex.value).getOrThrow().map{UInt64(it.toULong())}.toTypedArray()"))
    }

    @Test
    fun renders_string_receive_array_interface_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorViewString",
                            kind = WinMdTypeKind.Interface,
                            guid = "55555555-5555-5555-5555-555555555555",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "String[]",
                                    vtableIndex = 10,
                                    parameters = listOf(WinMdParameter("startIndex", "UInt32")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Collections/IVectorViewString.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<String>"))
        assertTrue(binding.contains("invokeStringReceiveArrayMethod(pointer,10,startIndex.value).getOrThrow()"))
    }

    @Test
    fun renders_float32_receive_array_interface_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorViewFloat32",
                            kind = WinMdTypeKind.Interface,
                            guid = "66666666-6666-6666-6666-666666666666",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "Float32[]",
                                    vtableIndex = 11,
                                    parameters = listOf(WinMdParameter("startIndex", "UInt32")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Collections/IVectorViewFloat32.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<Float32>"))
        assertTrue(binding.contains("invokeFloat32ReceiveArrayMethod(pointer,11,startIndex.value).getOrThrow().map{Float32(it)}.toTypedArray()"))
    }

    @Test
    fun renders_float64_receive_array_interface_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorViewFloat64",
                            kind = WinMdTypeKind.Interface,
                            guid = "77777777-7777-7777-7777-777777777777",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetRange",
                                    returnType = "Float64[]",
                                    vtableIndex = 12,
                                    parameters = listOf(WinMdParameter("startIndex", "UInt32")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Collections/IVectorViewFloat64.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<Float64>"))
        assertTrue(binding.contains("invokeFloat64ReceiveArrayMethod(pointer,12,startIndex.value).getOrThrow().map{Float64(it)}.toTypedArray()"))
    }

    @Test
    fun renders_small_primitive_receive_array_interface_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorViewSmall",
                            kind = WinMdTypeKind.Interface,
                            guid = "88888888-8888-8888-8888-888888888888",
                            methods = listOf(
                                WinMdMethod("GetBytes", "UInt8[]", vtableIndex = 13, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetShorts", "Int16[]", vtableIndex = 14, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetUShorts", "UInt16[]", vtableIndex = 15, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetChars", "Char16[]", vtableIndex = 16, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetFlags", "Boolean[]", vtableIndex = 17, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Collections/IVectorViewSmall.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetBytes(startIndex:UInt32):Array<UByte>"))
        assertTrue(binding.contains("invokeUInt8ReceiveArrayMethod(pointer,13,startIndex.value).getOrThrow().map{it.toUByte()}.toTypedArray()"))
        assertTrue(binding.contains("fungetShorts(startIndex:UInt32):Array<Short>"))
        assertTrue(binding.contains("invokeInt16ReceiveArrayMethod(pointer,14,startIndex.value).getOrThrow().toTypedArray()"))
        assertTrue(binding.contains("fungetUShorts(startIndex:UInt32):Array<UShort>"))
        assertTrue(binding.contains("invokeUInt16ReceiveArrayMethod(pointer,15,startIndex.value).getOrThrow().map{it.toUShort()}.toTypedArray()"))
        assertTrue(binding.contains("fungetChars(startIndex:UInt32):Array<Char>"))
        assertTrue(binding.contains("invokeChar16ReceiveArrayMethod(pointer,16,startIndex.value).getOrThrow().toTypedArray()"))
        assertTrue(binding.contains("fungetFlags(startIndex:UInt32):Array<WinRtBoolean>"))
        assertTrue(binding.contains("invokeBooleanReceiveArrayMethod(pointer,17,startIndex.value).getOrThrow().map{WinRtBoolean(it)}.toTypedArray()"))
    }

    @Test
    fun renders_value_conversion_receive_array_interface_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorViewConverted",
                            kind = WinMdTypeKind.Interface,
                            guid = "99999999-9999-9999-9999-999999999999",
                            methods = listOf(
                                WinMdMethod("GetGuids", "Guid[]", vtableIndex = 18, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetDates", "DateTime[]", vtableIndex = 19, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetDurations", "TimeSpan[]", vtableIndex = 20, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Collections/IVectorViewConverted.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetGuids(startIndex:UInt32):Array<Uuid>"))
        assertTrue(binding.contains("invokeGuidReceiveArrayMethod(pointer,18,startIndex.value).getOrThrow().map{Uuid.parse(it.toString())}.toTypedArray()"))
        assertTrue(binding.contains("fungetDates(startIndex:UInt32):Array<Instant>"))
        assertTrue(binding.contains("invokeDateTimeReceiveArrayMethod(pointer,19,startIndex.value).getOrThrow().map{Instant.fromEpochSeconds((it-116_444_736_000_000_000)/10000000L,((it-116_444_736_000_000_000)%10000000L*100).toInt())}.toTypedArray()"))
        assertTrue(binding.contains("fungetDurations(startIndex:UInt32):Array<Duration>"))
        assertTrue(binding.contains("invokeTimeSpanReceiveArrayMethod(pointer,20,startIndex.value).getOrThrow().map{Duration(it)}.toTypedArray()"))
    }

    @Test
    fun renders_object_receive_array_interface_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorViewInspectable",
                            kind = WinMdTypeKind.Interface,
                            guid = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                            methods = listOf(
                                WinMdMethod("GetInspectables", "Object[]", vtableIndex = 21, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Collections/IVectorViewInspectable.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetInspectables(startIndex:UInt32):Array<Inspectable>"))
        assertTrue(binding.contains("invokeObjectReceiveArrayMethod(pointer,21,startIndex.value).getOrThrow().map{Inspectable(it)}.toTypedArray()"))
    }

    @Test
    fun renders_supported_struct_receive_array_interface_methods_with_scalar_inputs() {
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
                            name = "IStructArraySource",
                            kind = WinMdTypeKind.Interface,
                            guid = "cccccccc-cccc-cccc-cccc-cccccccccccc",
                            methods = listOf(
                                WinMdMethod("GetPoints", "Point[]", vtableIndex = 22, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetSizes", "Size[]", vtableIndex = 23, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                                WinMdMethod("GetRects", "Rect[]", vtableIndex = 24, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IStructArraySource.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetPoints(startIndex:UInt32):Array<Point>"))
        assertTrue(binding.contains("invokeStructReceiveArrayMethod(pointer,22,Point.ABI_LAYOUT,startIndex.value).getOrThrow().map{Point.fromAbi(it)}.toTypedArray()"))
        assertTrue(binding.contains("fungetSizes(startIndex:UInt32):Array<Size>"))
        assertTrue(binding.contains("invokeStructReceiveArrayMethod(pointer,23,Size.ABI_LAYOUT,startIndex.value).getOrThrow().map{Size.fromAbi(it)}.toTypedArray()"))
        assertTrue(binding.contains("fungetRects(startIndex:UInt32):Array<Rect>"))
        assertTrue(binding.contains("invokeStructReceiveArrayMethod(pointer,24,Rect.ABI_LAYOUT,startIndex.value).getOrThrow().map{Rect.fromAbi(it)}.toTypedArray()"))
    }

    @Test
    fun renders_temporal_receive_array_interface_methods_with_temporal_array_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "ITemporalArrayTransformer",
                            kind = WinMdTypeKind.Interface,
                            guid = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee",
                            methods = listOf(
                                WinMdMethod("GetAndSetDateTimes", "DateTime[]", vtableIndex = 25, parameters = listOf(WinMdParameter("dateTimes", "DateTime[]", isIn = true))),
                                WinMdMethod("GetAndSetDurations", "TimeSpan[]", vtableIndex = 26, parameters = listOf(WinMdParameter("durations", "TimeSpan[]", isIn = true))),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/ITemporalArrayTransformer.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetAndSetDateTimes(dateTimes:Array<Instant>):Array<Instant>"))
        assertTrue(binding.contains("invokeDateTimeReceiveArrayMethod(pointer,25,dateTimes.size,LongArray(dateTimes.size){index->(((dateTimes[index].epochSeconds*10000000L)+(dateTimes[index].nanosecondsOfSecond/100))+116_444_736_000_000_000)}).getOrThrow().map{Instant.fromEpochSeconds((it-116_444_736_000_000_000)/10000000L,((it-116_444_736_000_000_000)%10000000L*100).toInt())}.toTypedArray()"))
        assertTrue(binding.contains("fungetAndSetDurations(durations:Array<Duration>):Array<Duration>"))
        assertTrue(binding.contains("invokeTimeSpanReceiveArrayMethod(pointer,26,durations.size,LongArray(durations.size){index->(durations[index].inWholeNanoseconds/100)}).getOrThrow().map{Duration(it)}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_class_receive_array_interface_methods_with_runtime_class_array_inputs() {
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
                            name = "IUriArrayTransformer",
                            kind = WinMdTypeKind.Interface,
                            guid = "12121212-1212-1212-1212-121212121212",
                            methods = listOf(
                                WinMdMethod("GetAndSetUris", "Uri[]", vtableIndex = 27, parameters = listOf(WinMdParameter("uris", "Uri[]", isIn = true))),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IUriArrayTransformer.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetAndSetUris(uris:Array<Uri>):Array<Uri>"))
        assertTrue(binding.contains("invokeObjectReceiveArrayMethod(pointer,27,uris.size,Array(uris.size){index->uris[index].pointer}).getOrThrow().map{Uri(it)}.toTypedArray()"))
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
    fun renders_hresult_interface_members_as_exceptions() {
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
                                    "LastError",
                                    "Windows.Foundation.HResult",
                                    mutable = true,
                                    getterVtableIndex = 6,
                                    setterVtableIndex = 7,
                                ),
                                WinMdProperty(
                                    "OptionalError",
                                    "Windows.Foundation.IReference`1<Windows.Foundation.HResult>",
                                    mutable = true,
                                    getterVtableIndex = 8,
                                    setterVtableIndex = 9,
                                ),
                            ),
                            methods = listOf(
                                WinMdMethod("GetError", "Windows.Foundation.HResult", vtableIndex = 10),
                                WinMdMethod(
                                    "SetError",
                                    "Unit",
                                    vtableIndex = 11,
                                    parameters = listOf(WinMdParameter("error", "Windows.Foundation.HResult")),
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

        assertTrue(binding.contains("lastError"))
        assertTrue(binding.contains("exceptionFromHResult(PlatformComInterop.invokeInt32Method(pointer,6).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeInt32Setter(pointer,7,hResultOfException(value)).getOrThrow()"))
        assertTrue(binding.contains("optionalError"))
        assertTrue(binding.contains("IReference.from<"))
        assertTrue(binding.contains("Inspectable(it),\"struct(Windows.Foundation.HResult;i4)\""))
        assertTrue(binding.contains("projectedObjectArgumentPointer("))
        assertTrue(binding.contains("\"pinterface({61c17706-2d65-11e0-9ae8-d48564015472};struct(Windows.Foundation.HResult;i4))\""))
        assertTrue(binding.contains("funsetError("))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer,11,hResultOfException(error)).getOrThrow()"))
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
