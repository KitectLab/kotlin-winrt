package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdField
import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertTrue
import org.junit.Test

class ValueTypeRendererTest {
    @Test
    fun generates_struct_abi_helpers_for_foundation_point() {
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
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Windows/Foundation/Point.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("dataclassPoint("))
        assertTrue(binding.contains("internalfuntoAbi():ComStructValue"))
        assertTrue(binding.contains("valwriter=ComStructWriter(ABI_LAYOUT)"))
        assertTrue(binding.contains("writer.writeFloat(x)"))
        assertTrue(binding.contains("writer.writeFloat(y)"))
        assertTrue(binding.contains("internalvalABI_LAYOUT:ComStructLayout=ComStructLayout(buildList{add(ComStructFieldKind.FLOAT32)add(ComStructFieldKind.FLOAT32)})"))
        assertTrue(binding.contains("internalfunfromAbi(value:ComStructValue):Point"))
        assertTrue(binding.contains("returnPoint(reader.readFloat(),reader.readFloat())"))
    }

    @Test
    fun generates_nested_struct_abi_helpers_and_unsigned_byte_fields() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Graphics",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Graphics",
                            name = "Point",
                            kind = WinMdTypeKind.Struct,
                            fields = listOf(
                                WinMdField("X", "Float32"),
                                WinMdField("Y", "Float32"),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Graphics",
                            name = "BrushSample",
                            kind = WinMdTypeKind.Struct,
                            fields = listOf(
                                WinMdField("Origin", "Example.Graphics.Point"),
                                WinMdField("Alpha", "UInt8"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Example/Graphics/BrushSample.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valorigin:Point"))
        assertTrue(binding.contains("valalpha:UByte"))
        assertTrue(binding.contains("addAll(Point.ABI_LAYOUT.fields)"))
        assertTrue(binding.contains("add(ComStructFieldKind.UINT8)"))
        assertTrue(binding.contains("writer.writeStruct(origin.toAbi())"))
        assertTrue(binding.contains("writer.writeUByte(alpha)"))
        assertTrue(binding.contains("Point.fromAbi(reader.readStruct(Point.ABI_LAYOUT))"))
        assertTrue(binding.contains("reader.readUByte()"))
    }

    @Test
    fun generates_struct_abi_helpers_for_hstring_fields() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Markup",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Markup",
                            name = "XmlnsDefinition",
                            kind = WinMdTypeKind.Struct,
                            fields = listOf(
                                WinMdField("XmlNamespace", "String"),
                                WinMdField("Namespace", "String"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Microsoft/UI/Xaml/Markup/XmlnsDefinition.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valxmlNamespace:String"))
        assertTrue(binding.contains("valnamespace:String"))
        assertTrue(binding.contains("add(ComStructFieldKind.HSTRING)"))
        assertTrue(binding.contains("writer.writeHString(xmlNamespace)"))
        assertTrue(binding.contains("writer.writeHString(namespace)"))
        assertTrue(binding.contains("returnXmlnsDefinition(reader.readHString(),reader.readHString())"))
    }

    @Test
    fun generates_struct_abi_helpers_for_direct_scalar_projections() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Foundation",
                            name = "TelemetryStamp",
                            kind = WinMdTypeKind.Struct,
                            fields = listOf(
                                WinMdField("OccurredAt", "DateTime"),
                                WinMdField("Elapsed", "TimeSpan"),
                                WinMdField("Token", "EventRegistrationToken"),
                                WinMdField("Id", "Guid"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Example/Foundation/TelemetryStamp.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valoccurredAt:Instant"))
        assertTrue(binding.contains("valelapsed:Duration"))
        assertTrue(binding.contains("valtoken:EventRegistrationToken"))
        assertTrue(binding.contains("valid:Uuid"))
        assertTrue(
            binding.contains(
                "add(ComStructFieldKind.INT64)add(ComStructFieldKind.INT64)add(ComStructFieldKind.INT64)add(ComStructFieldKind.GUID)",
            ),
        )
        assertTrue(binding.contains("writer.writeLong("))
        assertTrue(binding.contains("occurredAt.epochSeconds*10000000L"))
        assertTrue(binding.contains("occurredAt.nanosecondsOfSecond/100"))
        assertTrue(binding.contains("116_444_736_000_000_000L"))
        assertTrue(binding.contains("writer.writeLong(elapsed.inWholeNanoseconds/100)"))
        assertTrue(binding.contains("writer.writeLong(token.value)"))
        assertTrue(binding.contains("writer.writeGuid(id)"))
        assertTrue(binding.contains("Instant.fromEpochSeconds("))
        assertTrue(binding.contains("(ticks-116_444_736_000_000_000L)/10000000L"))
        assertTrue(binding.contains("((ticks-116_444_736_000_000_000L)%10000000L)*100"))
        assertTrue(binding.contains("Duration(reader.readLong())"))
        assertTrue(binding.contains("EventRegistrationToken(reader.readLong())"))
        assertTrue(binding.contains("reader.readGuid()"))
    }

    @Test
    fun generates_struct_abi_helpers_for_nullable_ireference_fields() {
        val model = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Contracts",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Contracts",
                            name = "WidgetInfo",
                            kind = WinMdTypeKind.Struct,
                            fields = listOf(
                                WinMdField("OptionalVersion", "Windows.Foundation.IReference`1<UInt64>"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val binding = KotlinBindingGenerator().generate(model)
            .first { it.relativePath == "Example/Contracts/WidgetInfo.kt" }
            .content
            .replace(Regex("\\s+"), "")

        assertTrue(binding.contains("valoptionalVersion:UInt64?"))
        assertTrue(binding.contains("add(ComStructFieldKind.OBJECT)"))
        assertTrue(binding.contains("writer.writeObjectPointer("))
        assertTrue(binding.contains("PropertyValue.createUInt64(optionalVersion).pointer"))
        assertTrue(binding.contains("reader.readObjectPointer().let{if(it.isNull)nullelseIPropertyValue.from(Inspectable(it)).getUInt64()}"))
    }
}
