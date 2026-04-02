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
}
