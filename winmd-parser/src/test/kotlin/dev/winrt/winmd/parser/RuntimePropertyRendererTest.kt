package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.encodeValueTypeName
import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimePropertyRendererTest {
    @Test
    fun renders_ireference_object_properties_as_nullable_inspectable_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IInspectableCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Target",
                                    type = "IReference<Object>",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IInspectableCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valtarget:Inspectable?"))
        assertTrue(binding.contains("if(pointer.isNull)nullelsePlatformComInterop.invokeObjectMethod(pointer,6).getOrThrow().let{if(it.isNull)nullelseInspectable(it)}"))
    }

    @Test
    fun renders_string_properties_as_string_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "StringCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Title",
                                    type = "String",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/StringCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valtitle:String"))
        assertTrue(binding.contains("run{valvalue=PlatformComInterop.invokeHStringMethod(pointer,6).getOrThrow()try{value.toKotlinString()}finally{value.close()}}"))
    }

    @Test
    fun renders_ireference_string_properties_as_nullable_string_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "NullableStringCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Title",
                                    type = "IReference<String>",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/NullableStringCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valtitle:String?"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethod(pointer,6).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getString()}"))
    }

    @Test
    fun renders_ireference_known_external_struct_properties_as_nullable_struct_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Graphics",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Graphics",
                            name = "NullableVectorCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Offset",
                                    type = "IReference<Windows.Foundation.Numerics.Vector2>",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Example/Graphics/NullableVectorCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valoffset:Vector2?"))
        assertTrue(binding.contains("IReference.from<Vector2>(Inspectable(it),\"struct(Windows.Foundation.Numerics.Vector2;f4;f4)\",\"Windows.Foundation.Numerics.Vector2\")"))
        assertTrue(binding.contains("Vector2.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(reference.pointer,6,Vector2.ABI_LAYOUT).getOrThrow())"))
    }

    @Test
    fun renders_ireference_marked_external_struct_properties_as_nullable_struct_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Xaml",
                            name = "NullableFontWeightCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "FontWeight",
                                    type = "Windows.Foundation.IReference`1<${encodeValueTypeName("Windows.UI.Text.FontWeight")}>",
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
            .first { it.relativePath == "Example/Xaml/NullableFontWeightCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("varfontWeight:FontWeight?"))
        assertTrue(binding.contains("IReference.from<FontWeight>(Inspectable(it),\"struct(Windows.UI.Text.FontWeight;u2)\",\"Windows.UI.Text.FontWeight\")"))
        assertTrue(binding.contains("FontWeight.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(reference.pointer,6,FontWeight.ABI_LAYOUT).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithObjectArg(pointer,7,if(value==null)ComPtr.NULLelse"))
        assertTrue(binding.contains("projectedObjectArgumentPointer("))
        assertTrue(binding.contains("\"pinterface({61c17706-2d65-11e0-9ae8-d48564015472};struct(Windows.UI.Text.FontWeight;u2))\""))
    }

    @Test
    fun renders_object_properties_as_inspectable_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "ObjectCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Payload",
                                    type = "Object",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/ObjectCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valpayload:Inspectable"))
        assertTrue(binding.contains("Inspectable(PlatformComInterop.invokeObjectMethod(pointer,6).getOrThrow())"))
    }

    @Test
    fun renders_enum_properties_as_enum_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "EnumCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "State",
                                    type = "AsyncStatus",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "AsyncStatus",
                            kind = WinMdTypeKind.Enum,
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/EnumCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valstate:AsyncStatus"))
        assertTrue(binding.contains("AsyncStatus.fromValue(PlatformComInterop.invokeInt32Method(pointer,6).getOrThrow())"))
    }

    @Test
    fun renders_signed_enum_properties_as_int32_backed_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Contracts",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Contracts",
                            name = "QualifiedEnumCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Mode",
                                    type = "Example.Contracts.Mode",
                                    mutable = true,
                                    getterVtableIndex = 6,
                                    setterVtableIndex = 7,
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Contracts",
                            name = "Mode",
                            kind = WinMdTypeKind.Enum,
                            enumUnderlyingType = "Int32",
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Example/Contracts/QualifiedEnumCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("varmode:Mode"))
        assertTrue(binding.contains("Mode.fromValue(PlatformComInterop.invokeInt32Method(pointer,6).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer,7,value.value).getOrThrow()"))
    }

    @Test
    fun renders_qualified_enum_properties_as_uint32_backed_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Contracts",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Contracts",
                            name = "QualifiedEnumCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Mode",
                                    type = "Example.Contracts.Mode",
                                    mutable = true,
                                    getterVtableIndex = 6,
                                    setterVtableIndex = 7,
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Contracts",
                            name = "Mode",
                            kind = WinMdTypeKind.Enum,
                            enumUnderlyingType = "UInt32",
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Example/Contracts/QualifiedEnumCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("varmode:Mode"))
        assertTrue(binding.contains("Mode.fromValue(PlatformComInterop.invokeUInt32Method(pointer,6).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer,7,value.value).getOrThrow()"))
    }

    @Test
    fun renders_ireference_scalar_properties_as_nullable_scalar_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IntCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Count",
                                    type = "IReference<Int32>",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/IntCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valcount:Int32?"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethod(pointer,6).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getInt32()}"))
    }

    @Test
    fun renders_ireference_boolean_properties_as_nullable_boolean_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "BoolCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Enabled",
                                    type = "IReference<Boolean>",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/BoolCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valenabled:WinRtBoolean?"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethod(pointer,6).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getBoolean()}"))
    }

    @Test
    fun renders_ireference_uint32_properties_as_nullable_uint_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "UIntCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Count",
                                    type = "IReference<UInt32>",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/UIntCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valcount:UInt32?"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethod(pointer,6).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getUInt32()}"))
    }

    @Test
    fun renders_ireference_int64_properties_as_nullable_long_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "LongCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Count",
                                    type = "IReference<Int64>",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/LongCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valcount:Int64?"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethod(pointer,6).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getInt64()}"))
    }

    @Test
    fun renders_ireference_uint64_properties_as_nullable_ulong_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "ULongCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Count",
                                    type = "IReference<UInt64>",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/ULongCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valcount:UInt64?"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethod(pointer,6).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getUInt64()}"))
    }

    @Test
    fun renders_ireference_float32_properties_as_nullable_float_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "FloatCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Ratio",
                                    type = "IReference<Float32>",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/FloatCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valratio:Float32?"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethod(pointer,6).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getSingle()}"))
    }

    @Test
    fun renders_ireference_float64_properties_as_nullable_double_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "DoubleCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Ratio",
                                    type = "IReference<Float64>",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/DoubleCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valratio:Float64?"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethod(pointer,6).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getDouble()}"))
    }

    @Test
    fun renders_struct_properties_as_struct_abi_accessors() {
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
                            name = "RectCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Bounds",
                                    type = "Rect",
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
            .first { it.relativePath == "Windows/Foundation/RectCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("RuntimeProperty<Rect>(Rect.fromAbi(ComStructValue(Rect.ABI_LAYOUT,ByteArray(Rect.ABI_LAYOUT.byteSize))))"))
        assertTrue(binding.contains("Rect.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,6,Rect.ABI_LAYOUT).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,7,value.toAbi()).getOrThrow()"))
    }

    @Test
    fun renders_nullable_struct_properties_via_property_value_boxing() {
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
                            name = "NullableRectCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Bounds",
                                    type = "Windows.Foundation.IReference`1<Windows.Foundation.Rect>",
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
            .first { it.relativePath == "Windows/Foundation/NullableRectCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valbounds:Rect?") || binding.contains("varbounds:Rect?"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethod(pointer,6).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getRect()}"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithObjectArg(pointer,7,if(value==null)ComPtr.NULLelsePropertyValue.createRect(value).pointer).getOrThrow()"))
    }

    @Test
    fun renders_small_scalar_properties_and_nullable_references_via_generic_abi_and_property_value() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "SmallScalarCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Priority",
                                    type = "Int16",
                                    mutable = true,
                                    getterVtableIndex = 6,
                                    setterVtableIndex = 7,
                                ),
                                WinMdProperty(
                                    name = "OptionalPriority",
                                    type = "Windows.Foundation.IReference`1<Int16>",
                                    mutable = true,
                                    getterVtableIndex = 8,
                                    setterVtableIndex = 9,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/SmallScalarCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("varpriority:Short"))
        assertTrue(binding.contains("PlatformComInterop.invokeMethodWithResultKind(pointer,6,ComMethodResultKind.INT16).getOrThrow().requireInt16()"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,7,value).getOrThrow()"))
        assertTrue(binding.contains("varoptionalPriority:Short?"))
        assertTrue(binding.contains("PlatformComInterop.invokeObjectMethod(pointer,8).getOrThrow().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getInt16()}"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithObjectArg(pointer,9,if(value==null)ComPtr.NULLelsePropertyValue.createInt16(value).pointer).getOrThrow()"))
    }

    @Test
    fun renders_nullable_enum_properties_via_generic_ireference_projection() {
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
                            name = "NullableModeCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Mode",
                                    type = "Windows.Foundation.IReference`1<Example.Contracts.Mode>",
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
            .first { it.relativePath == "Example/Contracts/NullableModeCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("varmode:Mode?"))
        assertTrue(binding.contains("IReference.from<Mode>(Inspectable(it),\"enum(Example.Contracts.Mode;u4)\",\"Example.Contracts.Mode\")"))
        assertTrue(binding.contains("Mode.fromValue(PlatformComInterop.invokeUInt32Method(reference.pointer,6).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithObjectArg(pointer,7,if(value==null)ComPtr.NULLelse"))
        assertTrue(binding.contains("projectedObjectArgumentPointer("))
        assertTrue(binding.contains("\"pinterface({61c17706-2d65-11e0-9ae8-d48564015472};enum(Example.Contracts.Mode;u4))\""))
    }

    @Test
    fun renders_hresult_properties_as_exceptions() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "ErrorCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "LastError",
                                    type = "Windows.Foundation.HResult",
                                    mutable = true,
                                    getterVtableIndex = 6,
                                    setterVtableIndex = 7,
                                ),
                                WinMdProperty(
                                    name = "OptionalError",
                                    type = "Windows.Foundation.IReference`1<Windows.Foundation.HResult>",
                                    mutable = true,
                                    getterVtableIndex = 8,
                                    setterVtableIndex = 9,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/ErrorCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("lastError"))
        assertTrue(binding.contains("exceptionFromHResult(PlatformComInterop.invokeInt32Method(pointer,6).getOrThrow())"))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer,7,hResultOfException(value)).getOrThrow()"))
        assertTrue(binding.contains("optionalError"))
        assertTrue(binding.contains("IReference.from<"))
        assertTrue(binding.contains("Inspectable(it),\"struct(Windows.Foundation.HResult;i4)\""))
        assertTrue(binding.contains("PlatformComInterop.invokeUnitMethodWithObjectArg(pointer,9,if(value==null)ComPtr.NULLelse"))
        assertTrue(binding.contains("\"pinterface({61c17706-2d65-11e0-9ae8-d48564015472};struct(Windows.Foundation.HResult;i4))\""))
    }

    @Test
    fun renders_event_registration_token_properties_as_token_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "TokenCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Cookie",
                                    type = "EventRegistrationToken",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/TokenCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valcookie:EventRegistrationToken"))
        assertTrue(binding.contains("EventRegistrationToken(PlatformComInterop.invokeInt64Getter(pointer,6).getOrThrow())"))
    }

    @Test
    fun renders_guid_properties_as_uuid_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "GuidCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Identifier",
                                    type = "Guid",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/GuidCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("validentifier:Uuid"))
        assertTrue(binding.contains("Uuid.parse(PlatformComInterop.invokeGuidGetter(pointer,6).getOrThrow().toString())"))
    }

    @Test
    fun renders_timespan_properties_as_duration_accessors() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "SpanCarrier",
                            kind = WinMdTypeKind.RuntimeClass,
                            properties = listOf(
                                WinMdProperty(
                                    name = "Duration",
                                    type = "TimeSpan",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/SpanCarrier.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valduration:Duration"))
        assertTrue(binding.contains("Duration(PlatformComInterop.invokeInt64Getter(pointer,6).getOrThrow())"))
    }

}
