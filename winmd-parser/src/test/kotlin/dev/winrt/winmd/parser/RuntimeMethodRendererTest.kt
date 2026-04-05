package dev.winrt.winmd.parser

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
                                WinMdMethod("GetWindowId", "Microsoft.UI.WindowId", vtableIndex = 6),
                                WinMdMethod("GetKeyStatus", "Windows.UI.Core.CorePhysicalKeyStatus", vtableIndex = 7),
                                WinMdMethod(
                                    "Initialize",
                                    "Unit",
                                    vtableIndex = 8,
                                    parameters = listOf(WinMdParameter("parentWindowId", "Microsoft.UI.WindowId")),
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
