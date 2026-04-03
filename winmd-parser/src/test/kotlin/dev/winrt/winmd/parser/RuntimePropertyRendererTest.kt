package dev.winrt.winmd.parser

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
        assertTrue(binding.contains("PlatformComInterop.invokeHStringMethod(pointer,6).getOrThrow().use{it.toKotlinString()}"))
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
        assertTrue(binding.contains("AsyncStatus.fromValue(PlatformComInterop.invokeUInt32Method(pointer,6).getOrThrow().toInt())"))
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
        assertTrue(binding.contains("if(pointer.isNull)nullelseInt32(PlatformComInterop.invokeInt32Method(pointer,6).getOrThrow())"))
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
        assertTrue(binding.contains("if(pointer.isNull)nullelseWinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer,6).getOrThrow())"))
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
        assertTrue(binding.contains("if(pointer.isNull)nullelseUInt32(PlatformComInterop.invokeUInt32Method(pointer,6).getOrThrow())"))
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
        assertTrue(binding.contains("if(pointer.isNull)nullelseInt64(PlatformComInterop.invokeInt64Getter(pointer,6).getOrThrow())"))
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
        assertTrue(binding.contains("if(pointer.isNull)nullelseUInt64(PlatformComInterop.invokeInt64Getter(pointer,6).getOrThrow().toULong())"))
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
        assertTrue(binding.contains("if(pointer.isNull)nullelseFloat32(PlatformComInterop.invokeFloat32Method(pointer,6).getOrThrow())"))
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
        assertTrue(binding.contains("if(pointer.isNull)nullelseFloat64(PlatformComInterop.invokeFloat64Method(pointer,6).getOrThrow())"))
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
