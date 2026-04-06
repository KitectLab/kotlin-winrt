package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.encodeValueTypeName
import dev.winrt.winmd.plugin.WinMdField
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdParameter
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeMethodRendererTest {
    @Test
    fun renders_runtime_value_type_methods_with_generic_abi_calls() {
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
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod("GetBounds", "Rect", vtableIndex = 6),
                                WinMdMethod(
                                    "CreateOverlay",
                                    "Object",
                                    vtableIndex = 7,
                                    parameters = listOf(WinMdParameter("bounds", "Rect")),
                                ),
                                WinMdMethod(
                                    "SetBounds",
                                    "Unit",
                                    vtableIndex = 8,
                                    parameters = listOf(WinMdParameter("bounds", "Rect")),
                                ),
                                WinMdMethod(
                                    "TryGetBounds",
                                    "Windows.Foundation.IReference`1<Windows.Foundation.Rect>",
                                    vtableIndex = 9,
                                ),
                                WinMdMethod(
                                    "ShowBounds",
                                    "Unit",
                                    vtableIndex = 10,
                                    parameters = listOf(
                                        WinMdParameter("bounds", "Windows.Foundation.IReference`1<Windows.Foundation.Rect>"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("Rect.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,6,Rect.ABI_LAYOUT).getOrThrow())"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,7,bounds.toAbi()).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,8,bounds.toAbi()).getOrThrow()"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,9).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getRect()}"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,10,if(bounds==null)ComPtr.NULLelsePropertyValue.createRect(bounds).pointer).getOrThrow()"))
    }

    @Test
    fun renders_runtime_small_scalar_methods_with_generic_result_kind_calls() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod("GetPriority", "Int16", vtableIndex = 6),
                                WinMdMethod(
                                    "CreateChild",
                                    "Object",
                                    vtableIndex = 7,
                                    parameters = listOf(WinMdParameter("priority", "Int16")),
                                ),
                                WinMdMethod(
                                    "SetPriority",
                                    "Unit",
                                    vtableIndex = 8,
                                    parameters = listOf(WinMdParameter("priority", "Int16")),
                                ),
                                WinMdMethod(
                                    "TryGetPriority",
                                    "Windows.Foundation.IReference`1<Int16>",
                                    vtableIndex = 9,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("PlatformComInterop.invokeMethodWithResultKind(pointer,6,ComMethodResultKind.INT16).getOrThrow().requireInt16()"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,7,priority).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,8,priority).getOrThrow()"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethodWithArgs(pointer,9).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getInt16()}"))
    }

    @Test
    fun renders_runtime_index_of_out_uint32_methods_as_nullable_uint32_helpers() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Collections",
                            name = "StringLookup",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "IndexOf",
                                    returnType = "Boolean",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter("value", "String"),
                                        WinMdParameter("index", "UInt32", byRef = true, isOut = true),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Example/Collections/StringLookup.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funwinRtIndexOf(value:String):UInt32?"))
        assertTrue(binding.contains("if(pointer.isNull){returnnull}"))
        assertTrue(binding.contains("invokeIndexOfMethod(pointer,6,value).getOrThrow()"))
        assertTrue(binding.contains("returnif(found)UInt32(index)elsenull"))
    }

    @Test
    fun renders_runtime_int32_pass_array_methods_with_count_and_buffer_arguments() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "CreateValues",
                                    "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("value", "Int32[]", isIn = true)),
                                ),
                                WinMdMethod(
                                    "SetValues",
                                    "Unit",
                                    vtableIndex = 7,
                                    parameters = listOf(WinMdParameter("value", "Int32[]", isIn = true)),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,IntArray(value.size){index->value[index].value}).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,7,value.size,IntArray(value.size){index->value[index].value}).getOrThrow()"))
    }

    @Test
    fun renders_runtime_int32_receive_array_methods_via_receive_array_helper() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "GetValues",
                                    "Int32[]",
                                    vtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetValues():Array<Int32>"))
        assertTrue(binding.contains("invokeInt32ReceiveArrayMethod(pointer,6).getOrThrow().map{Int32(it)}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_int32_fill_array_methods_with_count_buffer_and_scalar_return() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "Int32Vector",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "GetMany",
                                    "UInt32",
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
            .first { it.relativePath == "Windows/Foundation/Collections/Int32Vector.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetMany(startIndex:UInt32,items:Array<Int32>):UInt32"))
        assertTrue(binding.contains("valitemsAbi=IntArray(items.size){index->items[index].value}"))
        assertTrue(binding.contains("PlatformComInterop.invokeMethodWithResultKind(pointer,6,ComMethodResultKind.UINT32,startIndex.value,items.size,itemsAbi).getOrThrow().requireUInt32()"))
        assertTrue(binding.contains("itemsAbi.forEachIndexed{index,value->items[index]=Int32(value)}"))
    }

    @Test
    fun renders_runtime_int32_pass_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "CreateRange",
                                    "Object",
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
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateRange(startIndex:UInt32,value:Array<Int32>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,startIndex.value,value.size,IntArray(value.size){index->value[index].value}).getOrThrow())"))
    }

    @Test
    fun renders_runtime_int32_pass_array_methods_with_value_type_inputs() {
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
                            name = "LampArray",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "SetSingleColorForIndices",
                                    "Unit",
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
            .first { it.relativePath == "Windows/Devices/Lights/LampArray.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funsetSingleColorForIndices(desiredColor:Color,lampIndices:Array<Int32>)"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,6,desiredColor.toAbi(),lampIndices.size,IntArray(lampIndices.size){index->lampIndices[index].value}).getOrThrow()"))
    }

    @Test
    fun renders_runtime_uint8_pass_array_methods_with_value_type_inputs() {
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
                            name = "LampArrayMessage",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "SendMessage",
                                    "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter("desiredColor", "Windows.UI.Color"),
                                        WinMdParameter("messageBuffer", "UInt8[]", isIn = true),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Devices/Lights/LampArrayMessage.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funsendMessage(desiredColor:Color,messageBuffer:Array<UByte>)"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,6,desiredColor.toAbi(),messageBuffer.size,ByteArray(messageBuffer.size){index->messageBuffer[index].toByte()}).getOrThrow()"))
    }

    @Test
    fun renders_runtime_uint8_receive_array_methods_with_value_type_inputs() {
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
                            name = "LampArrayDataSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "GenerateData",
                                    "UInt8[]",
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
            .first { it.relativePath == "Windows/Devices/Lights/LampArrayDataSource.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungenerateData(desiredColor:Color):Array<UByte>"))
        assertTrue(binding.contains("invokeUInt8ReceiveArrayMethod(pointer,6,desiredColor.toAbi()).getOrThrow().map{it.toUByte()}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_uint8_receive_array_methods_with_guid_and_encoded_enum_inputs() {
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
                            guid = "36363636-3636-3636-3636-363636363636",
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
                            name = "PlayReadyITADataGenerator",
                            kind = WinMdTypeKind.RuntimeClass,
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
            .first { it.relativePath == "Windows/Media/Protection/PlayReady/PlayReadyITADataGenerator.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding, binding.contains("fungenerateData(guidCPSystemId:Uuid,countOfStreams:UInt32,configuration:IPropertySet,format:PlayReadyITADataFormat,):Array<UByte>"))
        assertTrue(binding.contains("invokeUInt8ReceiveArrayMethod(pointer,6,guidOf(guidCPSystemId.toString()),countOfStreams.value,"))
        assertTrue(binding.contains(",format.value).getOrThrow().map{it.toUByte()}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_string_pass_array_methods_with_count_and_buffer_arguments() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "CreateStringArray",
                                    "Object",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("value", "String[]", isIn = true)),
                                ),
                                WinMdMethod(
                                    "SetLabels",
                                    "Unit",
                                    vtableIndex = 7,
                                    parameters = listOf(WinMdParameter("value", "String[]", isIn = true)),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,value).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,7,value.size,value).getOrThrow()"))
    }

    @Test
    fun renders_runtime_string_pass_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "CreateRange",
                                    "Object",
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
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateRange(startIndex:UInt32,value:Array<String>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,startIndex.value,value.size,value).getOrThrow())"))
    }

    @Test
    fun renders_runtime_object_pass_array_methods_with_count_and_buffer_arguments() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "CreateInspectableArray",
                                    "Object",
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
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateInspectableArray(value:Array<Inspectable>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,Array(value.size){index->value[index].pointer}).getOrThrow())"))
    }

    @Test
    fun renders_runtime_guid_and_struct_pass_array_methods_with_count_and_buffer_arguments() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
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
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateGuidArray(value:Array<Uuid>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,ByteArray(value.size*16){index->valguid=guidOf(value[index/16].toString());when(index%16){0->guid.data1.toByte();1->(guid.data1shr8).toByte();2->(guid.data1shr16).toByte();3->(guid.data1shr24).toByte();4->guid.data2.toByte();5->(guid.data2.toInt()shr8).toByte();6->guid.data3.toByte();7->(guid.data3.toInt()shr8).toByte();else->guid.data4[index%16-8]}}).getOrThrow())"))
        assertTrue(binding.contains("funcreatePointArray(value:Array<Point>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,7,value.size,FloatArray(value.size*2){index->valitem=value[index/2];if(index%2==0)item.xelseitem.y}).getOrThrow())"))
        assertTrue(binding.contains("funcreateSizeArray(value:Array<Size>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,8,value.size,FloatArray(value.size*2){index->valitem=value[index/2];if(index%2==0)item.widthelseitem.height}).getOrThrow())"))
        assertTrue(binding.contains("funcreateRectArray(value:Array<Rect>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,9,value.size,FloatArray(value.size*4){index->valitem=value[index/4];when(index%4){0->item.x;1->item.y;2->item.width;else->item.height}}).getOrThrow())"))
    }

    @Test
    fun renders_runtime_small_primitive_pass_array_methods_with_count_and_buffer_arguments() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
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
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateBooleanArray(value:Array<WinRtBoolean>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,ByteArray(value.size){index->if(value[index].value)1.toByte()else0.toByte()}).getOrThrow())"))
        assertTrue(binding.contains("funcreateUInt8Array(value:Array<UByte>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,7,value.size,ByteArray(value.size){index->value[index].toByte()}).getOrThrow())"))
        assertTrue(binding.contains("funcreateInt16Array(value:Array<Short>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,8,value.size,ShortArray(value.size){index->value[index]}).getOrThrow())"))
        assertTrue(binding.contains("funcreateUInt16Array(value:Array<UShort>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,9,value.size,ShortArray(value.size){index->value[index].toShort()}).getOrThrow())"))
        assertTrue(binding.contains("funcreateChar16Array(value:Array<Char>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,10,value.size,CharArray(value.size){index->value[index]}).getOrThrow())"))
    }

    @Test
    fun renders_runtime_float32_pass_array_methods_with_count_and_buffer_arguments() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "CreateSingleArray",
                                    "Object",
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
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateSingleArray(value:Array<Float32>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,FloatArray(value.size){index->value[index].value}).getOrThrow())"))
    }

    @Test
    fun renders_runtime_float64_pass_array_methods_with_count_and_buffer_arguments() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "CreateDoubleArray",
                                    "Object",
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
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateDoubleArray(value:Array<Float64>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,DoubleArray(value.size){index->value[index].value}).getOrThrow())"))
    }

    @Test
    fun renders_runtime_uint32_pass_array_methods_with_count_and_buffer_arguments() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "CreateUInt32Array",
                                    "Object",
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
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateUInt32Array(value:Array<UInt32>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,IntArray(value.size){index->value[index].value.toInt()}).getOrThrow())"))
    }

    @Test
    fun renders_runtime_int64_pass_array_methods_with_count_and_buffer_arguments() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "CreateInt64Array",
                                    "Object",
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
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateInt64Array(value:Array<Int64>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,LongArray(value.size){index->value[index].value}).getOrThrow())"))
    }

    @Test
    fun renders_runtime_uint64_pass_array_methods_with_count_and_buffer_arguments() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "CreateUInt64Array",
                                    "Object",
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
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateUInt64Array(value:Array<UInt64>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,LongArray(value.size){index->value[index].value.toLong()}).getOrThrow())"))
    }

    @Test
    fun renders_runtime_datetime_pass_array_methods_with_count_and_buffer_arguments() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "CreateDateTimeArray",
                                    "Object",
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
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateDateTimeArray(value:Array<Instant>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,value.size,LongArray(value.size){index->(((value[index].epochSeconds*10000000L)+(value[index].nanosecondsOfSecond/100))+116_444_736_000_000_000)}).getOrThrow())"))
    }

    @Test
    fun renders_runtime_timespan_pass_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "CreateRange",
                                    "Object",
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
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("funcreateRange(startIndex:UInt32,value:Array<Duration>):Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,6,startIndex.value,value.size,LongArray(value.size){index->(value[index].inWholeNanoseconds/100)}).getOrThrow())"))
    }

    @Test
    fun renders_runtime_int32_receive_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "Int32Vector",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "GetRange",
                                    "Int32[]",
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
            .first { it.relativePath == "Windows/Foundation/Collections/Int32Vector.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<Int32>"))
        assertTrue(binding.contains("invokeInt32ReceiveArrayMethod(pointer,7,startIndex.value).getOrThrow().map{Int32(it)}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_uint32_receive_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "UInt32Vector",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "GetRange",
                                    "UInt32[]",
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
            .first { it.relativePath == "Windows/Foundation/Collections/UInt32Vector.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<UInt32>"))
        assertTrue(binding.contains("invokeUInt32ReceiveArrayMethod(pointer,7,startIndex.value).getOrThrow().map{UInt32(it)}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_int64_receive_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "Int64Vector",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "GetRange",
                                    "Int64[]",
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
            .first { it.relativePath == "Windows/Foundation/Collections/Int64Vector.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<Int64>"))
        assertTrue(binding.contains("invokeInt64ReceiveArrayMethod(pointer,8,startIndex.value).getOrThrow().map{Int64(it)}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_uint64_receive_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "UInt64Vector",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "GetRange",
                                    "UInt64[]",
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
            .first { it.relativePath == "Windows/Foundation/Collections/UInt64Vector.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<UInt64>"))
        assertTrue(binding.contains("invokeUInt64ReceiveArrayMethod(pointer,9,startIndex.value).getOrThrow().map{UInt64(it.toULong())}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_string_receive_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "StringVector",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "GetRange",
                                    "String[]",
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
            .first { it.relativePath == "Windows/Foundation/Collections/StringVector.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<String>"))
        assertTrue(binding.contains("invokeStringReceiveArrayMethod(pointer,10,startIndex.value).getOrThrow()"))
    }

    @Test
    fun renders_runtime_float32_receive_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "Float32Vector",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "GetRange",
                                    "Float32[]",
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
            .first { it.relativePath == "Windows/Foundation/Collections/Float32Vector.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<Float32>"))
        assertTrue(binding.contains("invokeFloat32ReceiveArrayMethod(pointer,11,startIndex.value).getOrThrow().map{Float32(it)}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_float64_receive_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "Float64Vector",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "GetRange",
                                    "Float64[]",
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
            .first { it.relativePath == "Windows/Foundation/Collections/Float64Vector.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetRange(startIndex:UInt32):Array<Float64>"))
        assertTrue(binding.contains("invokeFloat64ReceiveArrayMethod(pointer,12,startIndex.value).getOrThrow().map{Float64(it)}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_small_primitive_receive_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "SmallVector",
                            kind = WinMdTypeKind.RuntimeClass,
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
            .first { it.relativePath == "Windows/Foundation/Collections/SmallVector.kt" }
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
    fun renders_runtime_value_conversion_receive_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "ConvertedVector",
                            kind = WinMdTypeKind.RuntimeClass,
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
            .first { it.relativePath == "Windows/Foundation/Collections/ConvertedVector.kt" }
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
    fun renders_runtime_object_receive_array_methods_with_scalar_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "InspectableVector",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod("GetInspectables", "Object[]", vtableIndex = 21, parameters = listOf(WinMdParameter("startIndex", "UInt32"))),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Collections/InspectableVector.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetInspectables(startIndex:UInt32):Array<Inspectable>"))
        assertTrue(binding.contains("invokeObjectReceiveArrayMethod(pointer,21,startIndex.value).getOrThrow().map{Inspectable(it)}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_supported_struct_receive_array_methods_with_scalar_inputs() {
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
                            name = "StructArraySource",
                            kind = WinMdTypeKind.RuntimeClass,
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
            .first { it.relativePath == "Windows/Foundation/StructArraySource.kt" }
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
    fun renders_runtime_temporal_receive_array_methods_with_temporal_array_inputs() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "TemporalArrayTransformer",
                            kind = WinMdTypeKind.RuntimeClass,
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
            .first { it.relativePath == "Windows/Foundation/TemporalArrayTransformer.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetAndSetDateTimes(dateTimes:Array<Instant>):Array<Instant>"))
        assertTrue(binding.contains("invokeDateTimeReceiveArrayMethod(pointer,25,dateTimes.size,LongArray(dateTimes.size){index->(((dateTimes[index].epochSeconds*10000000L)+(dateTimes[index].nanosecondsOfSecond/100))+116_444_736_000_000_000)}).getOrThrow().map{Instant.fromEpochSeconds((it-116_444_736_000_000_000)/10000000L,((it-116_444_736_000_000_000)%10000000L*100).toInt())}.toTypedArray()"))
        assertTrue(binding.contains("fungetAndSetDurations(durations:Array<Duration>):Array<Duration>"))
        assertTrue(binding.contains("invokeTimeSpanReceiveArrayMethod(pointer,26,durations.size,LongArray(durations.size){index->(durations[index].inWholeNanoseconds/100)}).getOrThrow().map{Duration(it)}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_class_receive_array_methods_with_runtime_class_array_inputs() {
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
                            name = "UriArrayTransformer",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod("GetAndSetUris", "Uri[]", vtableIndex = 27, parameters = listOf(WinMdParameter("uris", "Uri[]", isIn = true))),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/UriArrayTransformer.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetAndSetUris(uris:Array<Uri>):Array<Uri>"))
        assertTrue(binding.contains("invokeObjectReceiveArrayMethod(pointer,27,uris.size,Array(uris.size){index->uris[index].pointer}).getOrThrow().map{Uri(it)}.toTypedArray()"))
    }

    @Test
    fun renders_runtime_nullable_enum_methods_via_generic_ireference_projection() {
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
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    "TryGetMode",
                                    "Windows.Foundation.IReference`1<Example.Contracts.Mode>",
                                    vtableIndex = 6,
                                ),
                                WinMdMethod(
                                    "SetMode",
                                    "Unit",
                                    vtableIndex = 7,
                                    parameters = listOf(
                                        WinMdParameter("mode", "Windows.Foundation.IReference`1<Example.Contracts.Mode>"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Example/Contracts/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("IReference.from<Mode>(Inspectable(it),\"enum(Example.Contracts.Mode;u4)\",\"Example.Contracts.Mode\")"))
        assertTrue(binding.contains("Mode.fromValue(PlatformComInterop.invokeUInt32Method(reference.pointer,6).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,7,if(mode==null)ComPtr.NULLelse"))
        assertTrue(binding.contains("projectedObjectArgumentPointer("))
        assertTrue(binding.contains("\"pinterface({61c17706-2d65-11e0-9ae8-d48564015472};enum(Example.Contracts.Mode;u4))\""))
    }

    @Test
    fun renders_runtime_hresult_methods_as_exceptions() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod("GetError", "Windows.Foundation.HResult", vtableIndex = 6),
                                WinMdMethod(
                                    "SetError",
                                    "Unit",
                                    vtableIndex = 7,
                                    parameters = listOf(WinMdParameter("error", "Windows.Foundation.HResult")),
                                ),
                                WinMdMethod(
                                    "TryGetError",
                                    "Windows.Foundation.IReference`1<Windows.Foundation.HResult>",
                                    vtableIndex = 8,
                                ),
                                WinMdMethod(
                                    "SetOptionalError",
                                    "Unit",
                                    vtableIndex = 9,
                                    parameters = listOf(
                                        WinMdParameter("error", "Windows.Foundation.IReference`1<Windows.Foundation.HResult>"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("fungetError()"))
        assertTrue(binding.contains("exceptionFromHResult(PlatformComInterop.invokeInt32Method(pointer,6).getOrThrow())"))
        assertTrue(binding.contains("funsetError("))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer,7,hResultOfException(error)).getOrThrow()"))
        assertTrue(binding.contains("IReference.from<"))
        assertTrue(binding.contains("Inspectable(it),\"struct(Windows.Foundation.HResult;i4)\""))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,9,if(error==null)ComPtr.NULLelse"))
        assertTrue(binding.contains("\"pinterface({61c17706-2d65-11e0-9ae8-d48564015472};struct(Windows.Foundation.HResult;i4))\""))
    }

    @Test
    fun renders_runtime_external_struct_methods_with_generic_abi_calls() {
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
                            methods = listOf(
                                WinMdMethod("GetWindowId", encodeValueTypeName("Microsoft.UI.WindowId"), vtableIndex = 6),
                                WinMdMethod("GetKeyStatus", encodeValueTypeName("Windows.UI.Core.CorePhysicalKeyStatus"), vtableIndex = 7),
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
            .first { it.relativePath == "Example/Xaml/Widget.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("WindowId.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,6,WindowId.ABI_LAYOUT).getOrThrow())"))
        assertTrue(binding.contains("CorePhysicalKeyStatus.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,7,CorePhysicalKeyStatus.ABI_LAYOUT).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,8,parentWindowId.toAbi()).getOrThrow()"))
    }
}
