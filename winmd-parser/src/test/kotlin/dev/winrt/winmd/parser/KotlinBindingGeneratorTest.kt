package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModelFactory
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdParameter
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import dev.winrt.winmd.plugin.WindowsSdkReferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class KotlinBindingGeneratorTest {
    @Test
    fun generates_bindings_for_known_namespaces() {
        val tempFile = Files.createTempFile("sample", ".winmd")
        Files.write(tempFile, byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()))

        val model = WinMdModelFactory.merge(
            primary = WinMdModelFactory.minimalModel(listOf(tempFile)),
            supplemental = WinMdModelFactory.sampleSupplementalModel(),
        )
        val files = KotlinBindingGenerator().generate(model)

        assertTrue(files.any { it.relativePath == "Windows/Foundation/IStringable.kt" })
        assertTrue(files.any { it.relativePath == "Windows/Foundation/Point.kt" })
        assertTrue(files.any { it.relativePath == "Windows/Foundation/AsyncStatus.kt" })
        assertTrue(files.any { it.relativePath == "Microsoft/UI/Xaml/Application.kt" })
        assertTrue(files.any { it.relativePath == "Microsoft/UI/Xaml/Window.kt" })

        val iStringableBinding = files.first { it.relativePath == "Windows/Foundation/IStringable.kt" }.content
        val pointBinding = files.first { it.relativePath == "Windows/Foundation/Point.kt" }.content
        val asyncStatusBinding = files.first { it.relativePath == "Windows/Foundation/AsyncStatus.kt" }.content
        val applicationBinding = files.first { it.relativePath == "Microsoft/UI/Xaml/Application.kt" }.content
        val windowBinding = files.first { it.relativePath == "Microsoft/UI/Xaml/Window.kt" }.content
        assertTrue(pointBinding.contains("data class Point("))
        assertTrue(pointBinding.contains("val x: Float64"))
        assertTrue(pointBinding.contains("val y: Float64"))
        assertTrue(asyncStatusBinding.contains("enum class AsyncStatus"))
        assertTrue(asyncStatusBinding.contains("Started"))
        assertTrue(asyncStatusBinding.contains("Completed"))
        assertTrue(applicationBinding.contains("open class Application"))
        assertTrue(applicationBinding.contains("PlatformComInterop.invokeUnitMethod(pointer, 6).getOrThrow()"))
        assertTrue(applicationBinding.contains("fun getLaunchCount(): UInt32 {"))
        assertTrue(applicationBinding.contains("return UInt32(PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())"))
        assertTrue(windowBinding.contains("open class Window"))
        assertTrue(windowBinding.contains("PlatformComInterop.invokeUnitMethod(pointer, 13).getOrThrow()"))
        assertTrue(windowBinding.contains("fun asIStringable(): IStringable = IStringable.from(this)"))
        assertTrue(windowBinding.contains("var title: String"))
        assertTrue(windowBinding.contains("PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()"))
        assertTrue(windowBinding.contains("PlatformComInterop.invokeStringSetter(pointer, 7, value).getOrThrow()"))
        assertTrue(windowBinding.contains("val isVisible: WinRtBoolean"))
        assertTrue(windowBinding.contains("return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 8).getOrThrow())"))
        assertTrue(windowBinding.contains("val createdAt: DateTime"))
        assertTrue(windowBinding.contains("return DateTime(PlatformComInterop.invokeInt64Getter(pointer, 10).getOrThrow())"))
        assertTrue(windowBinding.contains("val lifetime: TimeSpan"))
        assertTrue(windowBinding.contains("return TimeSpan(PlatformComInterop.invokeInt64Getter(pointer, 11).getOrThrow())"))
        assertTrue(windowBinding.contains("val lastToken: EventRegistrationToken"))
        assertTrue(windowBinding.contains("return EventRegistrationToken(PlatformComInterop.invokeInt64Getter(pointer, 12).getOrThrow())"))
        assertTrue(windowBinding.contains("val stableId: GuidValue"))
        assertTrue(windowBinding.contains("return GuidValue(PlatformComInterop.invokeGuidGetter(pointer, 9).getOrThrow().toString())"))
        assertTrue(windowBinding.contains("val optionalTitle: IReference<String>"))
        assertTrue(windowBinding.contains("val value = PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow()"))
        assertTrue(windowBinding.contains("WinRtStrings.toKotlin(value)"))
        assertTrue(windowBinding.contains("companion object : WinRtRuntimeClassMetadata"))
        assertTrue(applicationBinding.contains("override val activationKind"))
        assertTrue(applicationBinding.contains("WinRtActivationKind.Factory"))
        assertTrue(windowBinding.contains("override val activationKind"))
        assertTrue(windowBinding.contains("WinRtActivationKind.Factory"))
        assertTrue(applicationBinding.contains("constructor() : this(Companion.activate().pointer)"))
        assertTrue(windowBinding.contains("constructor() : this(Companion.activateInstance().pointer)"))
        assertTrue(applicationBinding.contains("fun activate(): Application = WinRtRuntime.activate(this, ::Application)"))
        assertTrue(windowBinding.contains("fun activateInstance(): Window = WinRtRuntime.activate(this, ::Window)"))
        assertTrue(windowBinding.contains("defaultInterfaceName: String? = \"Windows.Foundation.IStringable\""))
        assertTrue(iStringableBinding.contains("open class IStringable"))
        assertTrue(iStringableBinding.contains("WinRtInterfaceProjection"))
        assertTrue(iStringableBinding.contains("fun from("))
        assertTrue(iStringableBinding.contains("projectInterface"))
        assertTrue(iStringableBinding.contains("::IStringable"))
        assertTrue(iStringableBinding.contains("PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()"))
        assertTrue(iStringableBinding.contains("companion object : WinRtInterfaceMetadata"))
        assertTrue(iStringableBinding.contains("guidOf(\"96369f54-8eb6-48f0-abce-c1b211e627c3\")"))
        assertFalse(windowBinding.contains("Unit as"))
    }

    @Test
    fun normalizes_real_metadata_type_names_to_kotlin_references() {
        val model = WinMdModelFactory.sampleSupplementalModel().copy(
            namespaces = listOf(
                dev.winrt.winmd.plugin.WinMdNamespace(
                    name = "Windows.Data.Json",
                    types = listOf(
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "IJsonValue",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                        ),
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "JsonValueType",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.Enum,
                            enumMembers = listOf(
                                dev.winrt.winmd.plugin.WinMdEnumMember("Null", 0),
                            ),
                        ),
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "JsonObject",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Data.Json.IJsonObject",
                            methods = listOf(
                                dev.winrt.winmd.plugin.WinMdMethod(
                                    name = "GetValueType",
                                    returnType = "Windows.Data.Json.JsonValueType",
                                ),
                            ),
                            properties = listOf(
                                dev.winrt.winmd.plugin.WinMdProperty(
                                    name = "ValueType",
                                    type = "Windows.Data.Json.JsonValueType",
                                    mutable = false,
                                ),
                                dev.winrt.winmd.plugin.WinMdProperty(
                                    name = "Nested",
                                    type = "Windows.Data.Json.IJsonValue",
                                    mutable = false,
                                ),
                            ),
                        ),
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "IJsonObject",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.Interface,
                            guid = "22222222-2222-2222-2222-222222222222",
                            methods = listOf(
                                dev.winrt.winmd.plugin.WinMdMethod(
                                    name = "GetObject",
                                    returnType = "Windows.Data.Json.JsonObject",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val jsonObjectBinding = files.first { it.relativePath == "Windows/Data/Json/JsonObject.kt" }.content
        val jsonInterfaceBinding = files.first { it.relativePath == "Windows/Data/Json/IJsonObject.kt" }.content

        assertTrue(jsonObjectBinding.contains("fun asIJsonObject(): IJsonObject = IJsonObject.from(this)"))
        assertTrue(jsonObjectBinding.contains("override val defaultInterfaceName: String? = \"Windows.Data.Json.IJsonObject\""))
        assertTrue(jsonInterfaceBinding.contains("override val qualifiedName: String = \"Windows.Data.Json.IJsonObject\""))
        assertFalse(jsonObjectBinding.contains("Windows.Data.Json.JsonValueType"))
        assertFalse(jsonObjectBinding.contains("Windows.Data.Json.IJsonValue"))
    }

    @Test
    fun generates_string_argument_interface_calls_for_json_bindings() {
        val model = WinMdModelFactory.sampleSupplementalModel()

        val files = KotlinBindingGenerator().generate(model)
        val jsonInterfaceBinding = files.first { it.relativePath == "Windows/Data/Json/IJsonObject.kt" }.content
        val jsonValueBinding = files.first { it.relativePath == "Windows/Data/Json/IJsonValue.kt" }.content
        val jsonArrayBinding = files.first { it.relativePath == "Windows/Data/Json/IJsonArray.kt" }.content
        val jsonEnumBinding = files.first { it.relativePath == "Windows/Data/Json/JsonValueType.kt" }.content

        assertTrue(jsonInterfaceBinding.contains("fun getNamedValue(name: String): IJsonValue"))
        assertTrue(jsonInterfaceBinding.contains("PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6, name).getOrThrow()"))
        assertTrue(jsonInterfaceBinding.contains("fun getNamedString(name: String): String"))
        assertTrue(jsonInterfaceBinding.contains("PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 10, name).getOrThrow()"))
        assertTrue(jsonInterfaceBinding.contains("fun getNamedObject(name: String): JsonObject"))
        assertTrue(jsonInterfaceBinding.contains("PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 8, name).getOrThrow()"))
        assertTrue(jsonInterfaceBinding.contains("fun getNamedArray(name: String): JsonArray"))
        assertTrue(jsonInterfaceBinding.contains("PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 9, name).getOrThrow()"))
        assertTrue(jsonInterfaceBinding.contains("fun getNamedNumber(name: String): Float64"))
        assertTrue(jsonInterfaceBinding.contains("invokeFloat64MethodWithStringArg(pointer, 11, name).getOrThrow()"))
        assertTrue(jsonInterfaceBinding.contains("fun getNamedBoolean(name: String): WinRtBoolean"))
        assertTrue(jsonInterfaceBinding.contains("invokeBooleanMethodWithStringArg(pointer, 12,"))
        assertTrue(jsonInterfaceBinding.contains("name).getOrThrow()"))
        assertTrue(jsonValueBinding.contains("fun getObject(): JsonObject"))
        assertTrue(jsonValueBinding.contains("PlatformComInterop.invokeObjectMethod(pointer,"))
        assertTrue(jsonValueBinding.contains("12).getOrThrow()"))
        assertTrue(jsonArrayBinding.contains("fun getObjectAt(index: UInt32): JsonObject"))
        assertTrue(jsonArrayBinding.contains("invokeObjectMethodWithUInt32Arg(pointer, 6,"))
        assertTrue(jsonArrayBinding.contains("fun getArrayAt(index: UInt32): JsonArray"))
        assertTrue(jsonArrayBinding.contains("invokeObjectMethodWithUInt32Arg(pointer, 7,"))
        assertTrue(jsonArrayBinding.contains("fun getStringAt(index: UInt32): String"))
        assertTrue(jsonArrayBinding.contains("invokeHStringMethodWithUInt32Arg(pointer, 8,"))
        assertTrue(jsonArrayBinding.contains("fun getNumberAt(index: UInt32): Float64"))
        assertTrue(jsonArrayBinding.contains("invokeFloat64MethodWithUInt32Arg(pointer, 9,"))
        assertTrue(jsonArrayBinding.contains("fun getBooleanAt(index: UInt32): WinRtBoolean"))
        assertTrue(jsonArrayBinding.contains("invokeBooleanMethodWithUInt32Arg(pointer, 10,"))
        assertTrue(jsonArrayBinding.contains("index.value).getOrThrow()"))
        assertFalse(jsonArrayBinding.contains("Stub method not implemented"))
        assertTrue(jsonEnumBinding.contains("enum class JsonValueType"))
        assertTrue(jsonEnumBinding.contains("Object"))
    }

    @Test
    fun generates_projection_type_keys_for_interface_metadata() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Interop",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableVector",
                            kind = WinMdTypeKind.Interface,
                            guid = "393de7de-6fd0-4c0d-bb71-47244a113e93",
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first { it.relativePath == "Microsoft/UI/Xaml/Interop/IBindableVector.kt" }.content

        assertTrue(binding.contains("override val qualifiedName: String = \"Microsoft.UI.Xaml.Interop.IBindableVector\""))
        assertTrue(binding.contains("override val projectionTypeKey: String = \"System.Collections.IList\""))
    }

    @Test
    fun generates_kotlin_mutable_list_shape_for_bindable_vector() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Interop",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableVector",
                            kind = WinMdTypeKind.Interface,
                            guid = "393de7de-6fd0-4c0d-bb71-47244a113e93",
                            methods = listOf(
                                WinMdMethod(name = "First", returnType = "Microsoft.UI.Xaml.Interop.IBindableIterator", vtableIndex = 6),
                                WinMdMethod(
                                    name = "GetAt",
                                    returnType = "Object",
                                    parameters = listOf(WinMdParameter(name = "index", type = "UInt32")),
                                    vtableIndex = 7,
                                ),
                                WinMdMethod(name = "get_Size", returnType = "UInt32", vtableIndex = 8),
                                WinMdMethod(name = "GetView", returnType = "Microsoft.UI.Xaml.Interop.IBindableVectorView", vtableIndex = 9),
                                WinMdMethod(
                                    name = "Append",
                                    returnType = "Unit",
                                    parameters = listOf(WinMdParameter(name = "value", type = "Object")),
                                    vtableIndex = 14,
                                ),
                                WinMdMethod(name = "Clear", returnType = "Unit", vtableIndex = 16),
                            ),
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableVectorView",
                            kind = WinMdTypeKind.Interface,
                            guid = "346dd6e7-976e-4bc3-815d-ece243bc0f33",
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first { it.relativePath == "Microsoft/UI/Xaml/Interop/IBindableVector.kt" }.content

        assertTrue(binding.contains("open class IBindableVector"))
        assertTrue(binding.contains("MutableList<Inspectable> by"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
        assertTrue(binding.contains("fun getAt(index: UInt32): Inspectable"))
        assertTrue(binding.contains("invokeObjectSetter(pointer, 14,"))
        assertTrue(binding.contains("invokeUnitMethod(pointer, 16).getOrThrow()"))
        assertTrue(binding.contains("WinRtMutableListProjection<Inspectable>"))
    }

    @Test
    fun generates_kotlin_iterable_shape_for_bindable_iterable() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Interop",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableIterable",
                            kind = WinMdTypeKind.Interface,
                            guid = "036d2c08-df29-41af-8aa2-d774be62ba6f",
                            methods = listOf(
                                WinMdMethod(name = "First", returnType = "Microsoft.UI.Xaml.Interop.IBindableIterator", vtableIndex = 6),
                            ),
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a1d6c07-076d-49f2-8314-f52c9c9a8331",
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first { it.relativePath == "Microsoft/UI/Xaml/Interop/IBindableIterable.kt" }.content

        assertTrue(binding.contains("Iterable<Inspectable>"))
        assertTrue(binding.contains("override fun iterator(): Iterator<Inspectable>"))
        assertTrue(binding.contains("first()"))
    }

    @Test
    fun generates_kotlin_iterator_shape_for_bindable_iterator() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Interop",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a1d6c07-076d-49f2-8314-f52c9c9a8331",
                            methods = listOf(
                                WinMdMethod(name = "get_Current", returnType = "Object", vtableIndex = 6),
                                WinMdMethod(name = "get_HasCurrent", returnType = "Boolean", vtableIndex = 7),
                                WinMdMethod(name = "MoveNext", returnType = "Boolean", vtableIndex = 8),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first { it.relativePath == "Microsoft/UI/Xaml/Interop/IBindableIterator.kt" }.content

        assertTrue(binding.contains("Iterator<Inspectable>"))
        assertTrue(binding.contains("val winRtCurrent: Inspectable"))
        assertTrue(binding.contains("val winRtHasCurrent: WinRtBoolean"))
        assertTrue(binding.contains("override fun hasNext(): Boolean"))
        assertTrue(binding.contains("override fun next(): Inspectable"))
        assertTrue(binding.contains("throw NoSuchElementException()"))
    }

    @Test
    fun generates_kotlin_iterable_shape_for_runtime_class_implementing_bindable_iterable() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Interop",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableIterable",
                            kind = WinMdTypeKind.Interface,
                            guid = "036d2c08-df29-41af-8aa2-d774be62ba6f",
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Controls",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Controls",
                            name = "BindableItems",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Microsoft.UI.Xaml.Interop.IBindableIterable",
                            implementedInterfaces = listOf("Microsoft.UI.Xaml.Interop.IBindableIterable"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Microsoft/UI/Xaml/Controls/BindableItems.kt"
        }.content

        assertTrue(binding.contains("Iterable<Inspectable> by IBindableIterable.from(Inspectable(pointer))"))
    }

    @Test
    fun generates_kotlin_iterator_shape_for_runtime_class_implementing_bindable_iterator() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Interop",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a1d6c07-076d-49f2-8314-f52c9c9a8331",
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Controls",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Controls",
                            name = "BindableItemsIterator",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Microsoft.UI.Xaml.Interop.IBindableIterator",
                            implementedInterfaces = listOf("Microsoft.UI.Xaml.Interop.IBindableIterator"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Microsoft/UI/Xaml/Controls/BindableItemsIterator.kt"
        }.content

        assertTrue(binding.contains("Iterator<Inspectable> by IBindableIterator.from(Inspectable(pointer))"))
    }

    @Test
    fun generates_kotlin_iterable_shape_for_runtime_class_implementing_closed_generic_iterable() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterable",
                            kind = WinMdTypeKind.Interface,
                            guid = "faa585ea-6214-4217-afda-7f46de5869b3",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "IUIElement",
                            kind = WinMdTypeKind.Interface,
                            guid = "22222222-2222-2222-2222-222222222222",
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "UIElement",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Microsoft.UI.Xaml.IUIElement",
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Controls",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Controls",
                            name = "UiElementIterableHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IIterable<Microsoft.UI.Xaml.UIElement>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IIterable<Microsoft.UI.Xaml.UIElement>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Microsoft/UI/Xaml/Controls/UiElementIterableHost.kt"
        }.content

        assertTrue(binding.contains("Iterable<UIElement> by object : Iterable<UIElement>"))
        assertTrue(binding.contains("IIterable.from(Inspectable(pointer),"))
        assertTrue(binding.contains("IIterator.from(Inspectable("))
    }

    @Test
    fun generates_kotlin_iterator_shape_for_runtime_class_implementing_closed_generic_iterator() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "IUIElement",
                            kind = WinMdTypeKind.Interface,
                            guid = "22222222-2222-2222-2222-222222222222",
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "UIElement",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Microsoft.UI.Xaml.IUIElement",
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Controls",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Controls",
                            name = "UiElementIteratorHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IIterator<Microsoft.UI.Xaml.UIElement>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IIterator<Microsoft.UI.Xaml.UIElement>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Microsoft/UI/Xaml/Controls/UiElementIteratorHost.kt"
        }.content

        assertTrue(binding.contains("Iterator<UIElement> by object : Iterator<UIElement>"))
        assertTrue(binding.contains("IIterator.from(Inspectable(pointer),"))
        assertTrue(binding.contains("override fun hasNext(): Boolean"))
        assertTrue(binding.contains("override fun next(): UIElement"))
    }

    @Test
    fun generates_kotlin_iterable_shape_for_runtime_class_implementing_closed_generic_string_iterable() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterable",
                            kind = WinMdTypeKind.Interface,
                            guid = "faa585ea-6214-4217-afda-7f46de5869b3",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "StringIterableHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IIterable<String>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IIterable<String>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/StringIterableHost.kt"
        }.content

        assertTrue(binding.contains("Iterable<String> by object : Iterable<String>"))
        assertTrue(binding.contains("invokeHStringMethod("))
        assertTrue(binding.contains("WinRtStrings.toKotlin(value)"))
    }

    @Test
    fun generates_kotlin_iterator_shape_for_runtime_class_implementing_closed_generic_string_iterator() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "StringIteratorHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IIterator<String>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IIterator<String>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/StringIteratorHost.kt"
        }.content

        assertTrue(binding.contains("Iterator<String> by object : Iterator<String>"))
        assertTrue(binding.contains("invokeHStringMethod("))
        assertTrue(binding.contains("WinRtStrings.release(value)"))
    }

    @Test
    fun generates_kotlin_iterable_shape_for_runtime_class_implementing_closed_generic_boolean_iterable() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterable",
                            kind = WinMdTypeKind.Interface,
                            guid = "faa585ea-6214-4217-afda-7f46de5869b3",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "BooleanIterableHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IIterable<Boolean>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IIterable<Boolean>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/BooleanIterableHost.kt"
        }.content

        assertTrue(binding.contains("Iterable<Boolean> by object : Iterable<Boolean>"))
        assertTrue(binding.contains("invokeBooleanGetter("))
        assertTrue(binding.contains("WinRtBoolean("))
    }

    @Test
    fun generates_kotlin_iterator_shape_for_runtime_class_implementing_closed_generic_boolean_iterator() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "BooleanIteratorHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IIterator<Boolean>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IIterator<Boolean>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/BooleanIteratorHost.kt"
        }.content

        assertTrue(binding.contains("Iterator<Boolean> by object : Iterator<Boolean>"))
        assertTrue(binding.contains("invokeBooleanGetter("))
        assertTrue(binding.contains("override fun next(): Boolean"))
    }

    @Test
    fun generates_runtime_class_projections_for_implemented_interfaces() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Interop",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableVector",
                            kind = WinMdTypeKind.Interface,
                            guid = "393de7de-6fd0-4c0d-bb71-47244a113e93",
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Controls",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Controls",
                            name = "IUIElementCollection",
                            kind = WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Controls",
                            name = "UIElementCollection",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Microsoft.UI.Xaml.Controls.IUIElementCollection",
                            implementedInterfaces = listOf(
                                "Microsoft.UI.Xaml.Controls.IUIElementCollection",
                                "Microsoft.UI.Xaml.Interop.IBindableVector",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Microsoft/UI/Xaml/Controls/UIElementCollection.kt"
        }.content

        assertTrue(binding.contains("fun asIUIElementCollection(): IUIElementCollection = IUIElementCollection.from(this)"))
        assertTrue(binding.contains("fun asIBindableVector(): IBindableVector = IBindableVector.from(this)"))
    }

    @Test
    fun generates_kotlin_mutable_list_shape_for_runtime_class_implementing_bindable_vector() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Interop",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableVector",
                            kind = WinMdTypeKind.Interface,
                            guid = "393de7de-6fd0-4c0d-bb71-47244a113e93",
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Controls",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Controls",
                            name = "UIElementCollection",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Microsoft.UI.Xaml.Interop.IBindableVector",
                            implementedInterfaces = listOf("Microsoft.UI.Xaml.Interop.IBindableVector"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Microsoft/UI/Xaml/Controls/UIElementCollection.kt"
        }.content

        assertTrue(binding.contains("MutableList<Inspectable> by IBindableVector.from(Inspectable(pointer))"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_list_shape_for_runtime_class_implementing_closed_generic_vector_view() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorView",
                            kind = WinMdTypeKind.Interface,
                            guid = "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "StringVectorViewLike",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVectorView<String>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVectorView<String>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/StringVectorViewLike.kt"
        }.content

        assertTrue(binding.contains("List<String> by IVectorView.from(Inspectable(pointer), \"string\", \"String\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_mutable_list_shape_for_runtime_class_implementing_closed_generic_vector() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVector",
                            kind = WinMdTypeKind.Interface,
                            guid = "913337e9-11a1-4345-a3a2-4e7f956e222d",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "StringVectorLike",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVector<String>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVector<String>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/StringVectorLike.kt"
        }.content

        assertTrue(binding.contains("MutableList<String> by IVector.from(Inspectable(pointer), \"string\", \"String\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_list_shape_for_runtime_class_implementing_closed_generic_string_vector_view() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorView",
                            kind = WinMdTypeKind.Interface,
                            guid = "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "StringVectorViewHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVectorView<String>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVectorView<String>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/StringVectorViewHost.kt"
        }.content

        assertTrue(binding.contains("List<String> by IVectorView.from(Inspectable(pointer), \"string\", \"String\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_mutable_list_shape_for_runtime_class_implementing_closed_generic_string_vector() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVector",
                            kind = WinMdTypeKind.Interface,
                            guid = "913337e9-11a1-4345-a3a2-4e7f956e222d",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "StringVectorHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVector<String>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVector<String>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/StringVectorHost.kt"
        }.content

        assertTrue(binding.contains("MutableList<String> by IVector.from(Inspectable(pointer), \"string\", \"String\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_list_shape_for_runtime_class_implementing_closed_generic_boolean_vector_view() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorView",
                            kind = WinMdTypeKind.Interface,
                            guid = "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "BooleanVectorViewHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVectorView<Boolean>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVectorView<Boolean>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/BooleanVectorViewHost.kt"
        }.content

        assertTrue(binding.contains("List<Boolean> by IVectorView.from(Inspectable(pointer), \"b1\", \"Boolean\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_mutable_list_shape_for_runtime_class_implementing_closed_generic_boolean_vector() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVector",
                            kind = WinMdTypeKind.Interface,
                            guid = "913337e9-11a1-4345-a3a2-4e7f956e222d",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "BooleanVectorHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVector<Boolean>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVector<Boolean>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/BooleanVectorHost.kt"
        }.content

        assertTrue(binding.contains("MutableList<Boolean> by IVector.from(Inspectable(pointer), \"b1\", \"Boolean\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_iterable_shape_for_runtime_class_implementing_closed_generic_int_iterable() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterable",
                            kind = WinMdTypeKind.Interface,
                            guid = "faa585ea-6214-4217-afda-7f46de5869b3",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IntIterableHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IIterable<Int32>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IIterable<Int32>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/IntIterableHost.kt"
        }.content

        assertTrue(binding.contains("Iterable<Int> by object : Iterable<Int>"))
        assertTrue(binding.contains("invokeInt32Method("))
        assertTrue(binding.contains("IIterable.from(Inspectable(pointer),"))
        assertTrue(binding.contains("\"i4\""))
        assertTrue(binding.contains("\"Int32\""))
    }

    @Test
    fun generates_kotlin_list_shape_for_runtime_class_implementing_closed_generic_int_vector_view() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorView",
                            kind = WinMdTypeKind.Interface,
                            guid = "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IntVectorViewHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVectorView<Int32>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVectorView<Int32>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/IntVectorViewHost.kt"
        }.content

        assertTrue(binding.contains("List<Int> by IVectorView.from(Inspectable(pointer), \"i4\", \"Int32\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_mutable_list_shape_for_runtime_class_implementing_closed_generic_int_vector() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVector",
                            kind = WinMdTypeKind.Interface,
                            guid = "913337e9-11a1-4345-a3a2-4e7f956e222d",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IntVectorHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVector<Int32>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVector<Int32>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/IntVectorHost.kt"
        }.content

        assertTrue(binding.contains("MutableList<Int> by IVector.from(Inspectable(pointer), \"i4\", \"Int32\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_iterable_shape_for_runtime_class_implementing_closed_generic_double_iterable() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterable",
                            kind = WinMdTypeKind.Interface,
                            guid = "faa585ea-6214-4217-afda-7f46de5869b3",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "DoubleIterableHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IIterable<Float64>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IIterable<Float64>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/DoubleIterableHost.kt"
        }.content

        assertTrue(binding.contains("Iterable<Double> by object : Iterable<Double>"))
        assertTrue(binding.contains("invokeFloat64Method("))
        assertTrue(binding.contains("\"f8\""))
        assertTrue(binding.contains("\"Float64\""))
    }

    @Test
    fun generates_kotlin_list_shape_for_runtime_class_implementing_closed_generic_double_vector_view() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorView",
                            kind = WinMdTypeKind.Interface,
                            guid = "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "DoubleVectorViewHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVectorView<Float64>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVectorView<Float64>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/DoubleVectorViewHost.kt"
        }.content

        assertTrue(binding.contains("List<Double> by IVectorView.from(Inspectable(pointer), \"f8\", \"Float64\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_mutable_list_shape_for_runtime_class_implementing_closed_generic_double_vector() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVector",
                            kind = WinMdTypeKind.Interface,
                            guid = "913337e9-11a1-4345-a3a2-4e7f956e222d",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "DoubleVectorHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVector<Float64>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVector<Float64>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/DoubleVectorHost.kt"
        }.content

        assertTrue(binding.contains("MutableList<Double> by IVector.from(Inspectable(pointer), \"f8\", \"Float64\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_iterable_shape_for_runtime_class_implementing_closed_generic_uint_iterable() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterable",
                            kind = WinMdTypeKind.Interface,
                            guid = "faa585ea-6214-4217-afda-7f46de5869b3",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "UIntIterableHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IIterable<UInt32>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IIterable<UInt32>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/UIntIterableHost.kt"
        }.content

        assertTrue(binding.contains("Iterable<UInt> by object : Iterable<UInt>"))
        assertTrue(binding.contains("invokeUInt32Method("))
        assertTrue(binding.contains("\"u4\""))
        assertTrue(binding.contains("\"UInt32\""))
    }

    @Test
    fun generates_kotlin_list_shape_for_runtime_class_implementing_closed_generic_uint_vector_view() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorView",
                            kind = WinMdTypeKind.Interface,
                            guid = "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "UIntVectorViewHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVectorView<UInt32>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVectorView<UInt32>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/UIntVectorViewHost.kt"
        }.content

        assertTrue(binding.contains("List<UInt> by IVectorView.from(Inspectable(pointer), \"u4\", \"UInt32\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_mutable_list_shape_for_runtime_class_implementing_closed_generic_uint_vector() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVector",
                            kind = WinMdTypeKind.Interface,
                            guid = "913337e9-11a1-4345-a3a2-4e7f956e222d",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "UIntVectorHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVector<UInt32>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVector<UInt32>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/UIntVectorHost.kt"
        }.content

        assertTrue(binding.contains("MutableList<UInt> by IVector.from(Inspectable(pointer), \"u4\", \"UInt32\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_iterable_shape_for_runtime_class_implementing_closed_generic_long_iterable() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterable",
                            kind = WinMdTypeKind.Interface,
                            guid = "faa585ea-6214-4217-afda-7f46de5869b3",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "LongIterableHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IIterable<Int64>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IIterable<Int64>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/LongIterableHost.kt"
        }.content

        assertTrue(binding.contains("Iterable<Long> by object : Iterable<Long>"))
        assertTrue(binding.contains("invokeInt64Getter("))
        assertTrue(binding.contains("\"i8\""))
        assertTrue(binding.contains("\"Int64\""))
    }

    @Test
    fun generates_kotlin_list_shape_for_runtime_class_implementing_closed_generic_long_vector_view() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorView",
                            kind = WinMdTypeKind.Interface,
                            guid = "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "LongVectorViewHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVectorView<Int64>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVectorView<Int64>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/LongVectorViewHost.kt"
        }.content

        assertTrue(binding.contains("List<Long> by IVectorView.from(Inspectable(pointer), \"i8\", \"Int64\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_mutable_list_shape_for_runtime_class_implementing_closed_generic_long_vector() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVector",
                            kind = WinMdTypeKind.Interface,
                            guid = "913337e9-11a1-4345-a3a2-4e7f956e222d",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "LongVectorHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVector<Int64>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVector<Int64>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/LongVectorHost.kt"
        }.content

        assertTrue(binding.contains("MutableList<Long> by IVector.from(Inspectable(pointer), \"i8\", \"Int64\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_iterable_shape_for_runtime_class_implementing_closed_generic_ulong_iterable() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterable",
                            kind = WinMdTypeKind.Interface,
                            guid = "faa585ea-6214-4217-afda-7f46de5869b3",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "ULongIterableHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IIterable<UInt64>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IIterable<UInt64>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/ULongIterableHost.kt"
        }.content

        assertTrue(binding.contains("Iterable<ULong> by object : Iterable<ULong>"))
        assertTrue(binding.contains("invokeInt64Getter("))
        assertTrue(binding.contains("toULong()"))
        assertTrue(binding.contains("\"u8\""))
        assertTrue(binding.contains("\"UInt64\""))
    }

    @Test
    fun generates_kotlin_list_shape_for_runtime_class_implementing_closed_generic_ulong_vector_view() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVectorView",
                            kind = WinMdTypeKind.Interface,
                            guid = "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "ULongVectorViewHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVectorView<UInt64>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVectorView<UInt64>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/ULongVectorViewHost.kt"
        }.content

        assertTrue(binding.contains("List<ULong> by IVectorView.from(Inspectable(pointer), \"u8\", \"UInt64\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_mutable_list_shape_for_runtime_class_implementing_closed_generic_ulong_vector() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVector",
                            kind = WinMdTypeKind.Interface,
                            guid = "913337e9-11a1-4345-a3a2-4e7f956e222d",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "ULongVectorHost",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.Collections.IVector<UInt64>",
                            implementedInterfaces = listOf("Windows.Foundation.Collections.IVector<UInt64>"),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/ULongVectorHost.kt"
        }.content

        assertTrue(binding.contains("MutableList<ULong> by IVector.from(Inspectable(pointer), \"u8\", \"UInt64\")"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
    }

    @Test
    fun generates_kotlin_list_shape_for_string_vector_view() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "StringVectorView",
                            kind = WinMdTypeKind.RuntimeClass,
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first { it.relativePath == "Windows/Foundation/Collections/StringVectorView.kt" }.content

        assertTrue(binding.contains("open class StringVectorView"))
        assertTrue(binding.contains("List<String> by"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
        assertTrue(binding.contains("fun getAt(index: UInt32): String"))
        assertTrue(binding.contains("WinRtListProjection<String>"))
    }

    @Test
    fun generates_kotlin_list_shape_for_bindable_vector_view() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Interop",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableVectorView",
                            kind = WinMdTypeKind.Interface,
                            guid = "346dd6e7-976e-4bc3-815d-ece243bc0f33",
                            methods = listOf(
                                WinMdMethod(name = "First", returnType = "Microsoft.UI.Xaml.Interop.IBindableIterator", vtableIndex = 6),
                                WinMdMethod(
                                    name = "GetAt",
                                    returnType = "Object",
                                    parameters = listOf(WinMdParameter(name = "index", type = "UInt32")),
                                    vtableIndex = 7,
                                ),
                                WinMdMethod(name = "get_Size", returnType = "UInt32", vtableIndex = 8),
                            ),
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Interop",
                            name = "IBindableIterator",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first { it.relativePath == "Microsoft/UI/Xaml/Interop/IBindableVectorView.kt" }.content

        assertTrue(binding.contains("open class IBindableVectorView"))
        assertTrue(binding.contains("List<Inspectable> by"))
        assertTrue(binding.contains("val winRtSize: UInt32"))
        assertTrue(binding.contains("fun getAt(index: UInt32): Inspectable"))
        assertTrue(binding.contains("WinRtListProjection<Inspectable>"))
    }

    @Test
    fun generates_json_array_uint32_object_call_from_real_metadata_model() {
        val universalContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.UniversalApiContract",
            sdkVersion = "10.0.22621.0",
        )
        val foundationContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.FoundationContract",
            sdkVersion = "10.0.22621.0",
        )
        val model = WinMdModelFactory.merge(
            primary = WinMdModelFactory.metadataModel(
                listOf(
                    universalContract.winmdPath,
                    foundationContract.winmdPath,
                ),
            ),
            supplemental = WinMdModelFactory.sampleSupplementalModel(),
        )

        val files = KotlinBindingGenerator().generate(model)
        val jsonArrayBinding = files.first { it.relativePath == "Windows/Data/Json/IJsonArray.kt" }.content

        assertTrue(jsonArrayBinding.contains("fun getObjectAt(index: UInt32): JsonObject"))
        assertTrue(jsonArrayBinding.contains("invokeObjectMethodWithUInt32Arg(pointer, 6,"))
        assertTrue(jsonArrayBinding.contains("index.value).getOrThrow()"))
    }

    @Test
    fun preserves_runtime_class_base_type_in_generated_binding() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "DependencyObject",
                            kind = WinMdTypeKind.RuntimeClass,
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml.Controls",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml.Controls",
                            name = "TextBlock",
                            kind = WinMdTypeKind.RuntimeClass,
                            baseClass = "Microsoft.UI.Xaml.DependencyObject",
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val textBlockBinding = files.first { it.relativePath == "Microsoft/UI/Xaml/Controls/TextBlock.kt" }.content

        assertTrue(textBlockBinding.contains("open class TextBlock"))
        assertTrue(textBlockBinding.contains(": DependencyObject(pointer)"))
        assertFalse(textBlockBinding.contains(": Inspectable(pointer)"))
    }

    @Test
    fun reads_runtime_class_base_type_from_real_metadata_model() {
        val universalContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.UniversalApiContract",
            sdkVersion = "10.0.22621.0",
        )
        val foundationContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.FoundationContract",
            sdkVersion = "10.0.22621.0",
        )
        val model = WinMdModelFactory.metadataModel(
            listOf(
                universalContract.winmdPath,
                foundationContract.winmdPath,
            ),
        )

        val jsonObject = model.namespaces
            .first { it.name == "Windows.Data.Json" }
            .types.first { it.name == "JsonObject" }

        assertTrue(jsonObject.baseClass == "Windows.Data.Json.JsonValue" || jsonObject.baseClass == "System.Object")
    }

    @Test
    fun reads_generic_parameter_names_from_real_metadata_model() {
        val universalContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.UniversalApiContract",
            sdkVersion = "10.0.22621.0",
        )
        val foundationContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.FoundationContract",
            sdkVersion = "10.0.22621.0",
        )
        val model = WinMdModelFactory.metadataModel(
            listOf(
                universalContract.winmdPath,
                foundationContract.winmdPath,
            ),
        )

        val vector = model.namespaces
            .first { it.name == "Windows.Foundation.Collections" }
            .types.first { it.name == "IVector`1" }

        assertTrue(vector.genericParameters.toString(), vector.genericParameters == listOf("T"))

        val renderedSignatures = vector.methods.joinToString(separator = "\n") { method ->
            "${method.name}(${method.parameters.joinToString(",") { it.type }}):${method.returnType}"
        }

        assertTrue(renderedSignatures, renderedSignatures.contains("GetAt(UInt32):T"))
        assertTrue(renderedSignatures, renderedSignatures.contains("Append(T):Unit"))
        assertTrue(renderedSignatures, renderedSignatures.contains("GetView():Windows.Foundation.Collections.IVectorView`1<T>"))
    }

    @Test
    fun expands_specialized_interface_members_from_real_metadata_model() {
        val universalContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.UniversalApiContract",
            sdkVersion = "10.0.22621.0",
        )
        val foundationContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.FoundationContract",
            sdkVersion = "10.0.22621.0",
        )
        val model = WinMdModelFactory.metadataModel(
            listOf(
                universalContract.winmdPath,
                foundationContract.winmdPath,
            ),
        )

        val propertySet = model.namespaces
            .first { it.name == "Windows.Foundation.Collections" }
            .types.first { it.name == "IPropertySet" }

        val renderedSignatures = propertySet.methods.joinToString(separator = "\n") { method ->
            "${method.name}(${method.parameters.joinToString(",") { it.type }}):${method.returnType}"
        }

        assertTrue(propertySet.baseInterfaces.toString(), propertySet.baseInterfaces.any { it.startsWith("Windows.Foundation.Collections.IMap`2<") })
        assertTrue(renderedSignatures, renderedSignatures.contains("Lookup(String):Object"))
        assertTrue(renderedSignatures, renderedSignatures.contains("Insert(String,Object):Boolean"))
        assertFalse(renderedSignatures, renderedSignatures.contains("ElementType0x13"))
    }

    @Test
    fun generates_specialized_object_methods_for_property_set_binding() {
        val universalContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.UniversalApiContract",
            sdkVersion = "10.0.22621.0",
        )
        val foundationContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.FoundationContract",
            sdkVersion = "10.0.22621.0",
        )
        val model = WinMdModelFactory.metadataModel(
            listOf(
                universalContract.winmdPath,
                foundationContract.winmdPath,
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val propertySetBinding = files.first {
            it.relativePath == "Windows/Foundation/Collections/IPropertySet.kt"
        }.content

        assertTrue(propertySetBinding.contains("fun lookup(key: String): Inspectable"))
        assertTrue(propertySetBinding.contains("fun hasKey(key: String): WinRtBoolean"))
        assertTrue(propertySetBinding.contains("invokeObjectMethodWithStringArg(pointer, 6, key).getOrThrow()"))
        assertTrue(propertySetBinding.contains("invokeBooleanMethodWithStringArg(pointer, 8,"))
    }

    @Test
    fun generates_type_parameters_for_open_generic_interfaces() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation.Collections",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IVector`1",
                            kind = WinMdTypeKind.Interface,
                            guid = "00000000-0000-0000-0000-000000000001",
                            genericParameters = listOf("T"),
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetView",
                                    returnType = "Windows.Foundation.Collections.IVectorView`1<T>",
                                    vtableIndex = 8,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Windows/Foundation/Collections/IVector`1.kt"
        }.content

        assertTrue(binding.contains("open class `IVector`1`<T>("))
        assertTrue(binding.contains("fun signatureOf(arg0Signature: String): String"))
        assertTrue(binding.contains("fun projectionTypeKeyOf(arg0ProjectionTypeKey: String): String"))
        assertTrue(binding.contains("System.Collections.Generic.IList<"))
        assertTrue(binding.contains("WinRtTypeSignature.parameterizedInterface("))
        assertTrue(binding.contains("\"00000000-0000-0000-0000-000000000001\""))
        assertTrue(binding.contains("arg0Signature"))
        assertTrue(binding.contains("fun iidOf(vararg argumentSignatures: String): Guid"))
        assertTrue(binding.contains("ParameterizedInterfaceId.createFromSignature(signatureOf(*argumentSignatures))"))
        assertTrue(binding.contains("fun metadataOf("))
        assertTrue(binding.contains("arg0Signature: String"))
        assertTrue(binding.contains("arg0ProjectionTypeKey: String"))
        assertTrue(binding.contains("WinRtInterfaceMetadata"))
        assertTrue(binding.contains("override val projectionTypeKey: String = projectionTypeKeyOf(arg0ProjectionTypeKey)"))
        assertTrue(binding.contains("override val iid: Guid = iidOf(arg0Signature)"))
        assertTrue(binding.contains("fun <T> from("))
        assertTrue(binding.contains("inspectable: Inspectable"))
        assertTrue(binding.contains("inspectable.projectInterface(metadataOf("))
        assertTrue(binding.contains("arg0ProjectionTypeKey"))
        assertTrue(binding.contains("IVector"))
    }

    @Test
    fun expands_winui_ui_element_collection_members_from_real_metadata_model_when_available() {
        val winUiRoot = java.nio.file.Path.of("F:/Dependencies/nuget/microsoft.windowsappsdk/1.6.240923002")
        val xamlWinmd = winUiRoot.resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd")
        if (!java.nio.file.Files.isRegularFile(xamlWinmd)) {
            return
        }

        val universalContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.UniversalApiContract",
            sdkVersion = "10.0.22621.0",
        )
        val foundationContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.FoundationContract",
            sdkVersion = "10.0.22621.0",
        )
        val model = WinMdModelFactory.metadataModel(
            listOf(
                xamlWinmd,
                universalContract.winmdPath,
                foundationContract.winmdPath,
            ),
        )

        val collection = model.namespaces
            .first { it.name == "Microsoft.UI.Xaml.Controls" }
            .types.first { it.name == "IUIElementCollection" }

        val renderedSignatures = collection.methods.joinToString(separator = "\n") { method ->
            "${method.name}(${method.parameters.joinToString(",") { it.type }}):${method.returnType}"
        }

        assertTrue(collection.baseInterfaces.toString(), collection.baseInterfaces.any { it.startsWith("Windows.Foundation.Collections.IVector`1<Microsoft.UI.Xaml.UIElement>") })
        assertTrue(renderedSignatures, renderedSignatures.contains("Append(Microsoft.UI.Xaml.UIElement):Unit"))
    }

    @Test
    fun generates_dispatcher_queue_try_enqueue_from_real_metadata_model_when_available() {
        val uiWinmd = java.nio.file.Path.of(
            "F:/Dependencies/nuget/microsoft.windowsappsdk/1.6.240923002/lib/uap10.0.18362/Microsoft.UI.winmd",
        )
        if (!java.nio.file.Files.isRegularFile(uiWinmd)) {
            return
        }

        val model = WinMdModelFactory.metadataModel(listOf(uiWinmd))
        val files = KotlinBindingGenerator().generate(model)
        val dispatcherQueueBinding = files.first {
            it.relativePath == "Microsoft/UI/Dispatching/IDispatcherQueue.kt"
        }.content

        assertTrue(dispatcherQueueBinding.contains("fun tryEnqueue(callback: DispatcherQueueHandler): WinRtBoolean"))
        assertTrue(dispatcherQueueBinding.contains("fun tryEnqueue("))
        assertTrue(dispatcherQueueBinding.contains("() -> Unit"))
        assertTrue(dispatcherQueueBinding.contains("WinRtDelegateBridge.createNoArgUnitDelegate"))
        assertTrue(dispatcherQueueBinding.contains("DispatcherQueueHandler.iid"))
        assertTrue(dispatcherQueueBinding.contains("invokeBooleanMethodWithObjectArg(pointer, 7,"))
        assertTrue(dispatcherQueueBinding.contains("callback.pointer"))
    }

    @Test
    fun generates_lambda_overload_for_object_arg_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "IApplicationInitializationCallbackParams",
                            kind = WinMdTypeKind.Interface,
                            guid = "bbbbbbbb-1111-2222-3333-444444444444",
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "ApplicationInitializationCallback",
                            kind = WinMdTypeKind.Delegate,
                            guid = "d8eef1c9-1234-56f1-9963-45dd9c80a661",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(
                                            name = "value",
                                            type = "Microsoft.UI.Xaml.IApplicationInitializationCallbackParams",
                                        ),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "IApplicationStatics",
                            kind = WinMdTypeKind.Interface,
                            guid = "4e0d09f5-4358-512c-a987-503b52848e95",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Start",
                                    returnType = "Unit",
                                    vtableIndex = 7,
                                    parameters = listOf(
                                        WinMdParameter(
                                            name = "callback",
                                            type = "Microsoft.UI.Xaml.ApplicationInitializationCallback",
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Microsoft/UI/Xaml/IApplicationStatics.kt"
        }.content

        assertTrue(binding.contains("fun start(callback: ApplicationInitializationCallback)"))
        assertTrue(binding.contains("fun start("))
        assertTrue(binding.contains("IApplicationInitializationCallbackParams"))
        assertTrue(binding.contains("-> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createObjectArgUnitDelegate"))
        assertTrue(binding.contains("ApplicationInitializationCallback.iid"))
        assertTrue(binding.contains("callback(IApplicationInitializationCallbackParams(arg))"))
        assertTrue(binding.contains("start(ApplicationInitializationCallback(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_no_arg_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "TickHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "11111111-1111-1111-1111-111111111111",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "Ticker",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.TickHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/Ticker.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: TickHandler)"))
        assertTrue(binding.contains("fun setHandler("))
        assertTrue(binding.contains("() -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createNoArgUnitDelegate(TickHandler.iid, callback)"))
        assertTrue(binding.contains("setHandler(TickHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_object_arg_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "Payload",
                            kind = WinMdTypeKind.RuntimeClass,
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "PayloadHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "22222222-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Example.Runtime.Payload"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "PayloadSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.PayloadHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/PayloadSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: PayloadHandler)"))
        assertTrue(binding.contains("WinRtDelegateBridge.createObjectArgUnitDelegate(PayloadHandler.iid)"))
        assertTrue(binding.contains("callback(Payload(arg))"))
        assertTrue(binding.contains("setHandler(PayloadHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_no_arg_boolean_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "ShouldContinueHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "33333333-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Boolean",
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "IController",
                            kind = WinMdTypeKind.Interface,
                            guid = "44444444-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.ShouldContinueHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/IController.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: ShouldContinueHandler)"))
        assertTrue(binding.contains("() -> Boolean"))
        assertTrue(binding.contains("WinRtDelegateBridge.createNoArgBooleanDelegate"))
        assertTrue(binding.contains("ShouldContinueHandler.iid"))
        assertTrue(binding.contains("setHandler(ShouldContinueHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_object_arg_boolean_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "Payload",
                            kind = WinMdTypeKind.RuntimeClass,
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "PayloadPredicate",
                            kind = WinMdTypeKind.Delegate,
                            guid = "55555555-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Boolean",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Example.Runtime.Payload"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "PayloadSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetPredicate",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.PayloadPredicate"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/PayloadSource.kt"
        }.content

        assertTrue(binding.contains("fun setPredicate(callback: PayloadPredicate)"))
        assertTrue(binding.contains("WinRtDelegateBridge.createObjectArgBooleanDelegate(PayloadPredicate.iid)"))
        assertTrue(binding.contains("callback(Payload(arg))"))
        assertTrue(binding.contains("setPredicate(PayloadPredicate(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_int32_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "CountHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "66666666-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Int32"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "ICounterSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "77777777-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.CountHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/ICounterSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: CountHandler)"))
        assertTrue(binding.contains("(Int) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createInt32ArgUnitDelegate(CountHandler.iid, callback)"))
        assertTrue(binding.contains("setHandler(CountHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_int32_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "CountHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "88888888-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Int32"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "CounterSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.CountHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/CounterSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: CountHandler)"))
        assertTrue(binding.contains("(Int) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createInt32ArgUnitDelegate(CountHandler.iid, callback)"))
        assertTrue(binding.contains("setHandler(CountHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_int32_boolean_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "CountPredicate",
                            kind = WinMdTypeKind.Delegate,
                            guid = "27272727-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Boolean",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Int32"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "ICountSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "28282828-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetPredicate",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.CountPredicate"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/ICountSource.kt"
        }.content

        assertTrue(binding.contains("fun setPredicate(callback: CountPredicate)"))
        assertTrue(binding.contains("(Int) -> Boolean"))
        assertTrue(binding.contains("WinRtDelegateBridge.createInt32ArgBooleanDelegate"))
        assertTrue(binding.contains("CountPredicate.iid"))
        assertTrue(binding.contains("setPredicate(CountPredicate(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_int32_boolean_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "CountPredicate",
                            kind = WinMdTypeKind.Delegate,
                            guid = "29292929-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Boolean",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Int32"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "CountSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetPredicate",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.CountPredicate"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/CountSource.kt"
        }.content

        assertTrue(binding.contains("fun setPredicate(callback: CountPredicate)"))
        assertTrue(binding.contains("(Int) -> Boolean"))
        assertTrue(binding.contains("WinRtDelegateBridge.createInt32ArgBooleanDelegate"))
        assertTrue(binding.contains("CountPredicate.iid"))
        assertTrue(binding.contains("setPredicate(CountPredicate(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_string_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "LabelHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "99999999-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "String"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "ILabelSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "aaaaaaaa-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.LabelHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/ILabelSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: LabelHandler)"))
        assertTrue(binding.contains("(String) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createStringArgUnitDelegate(LabelHandler.iid, callback)"))
        assertTrue(binding.contains("setHandler(LabelHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_string_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "LabelHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "bbbbbbbb-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "String"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "LabelSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.LabelHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/LabelSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: LabelHandler)"))
        assertTrue(binding.contains("(String) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createStringArgUnitDelegate(LabelHandler.iid, callback)"))
        assertTrue(binding.contains("setHandler(LabelHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_string_boolean_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "LabelPredicate",
                            kind = WinMdTypeKind.Delegate,
                            guid = "2a2a2a2a-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Boolean",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "String"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "ILabelSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "2b2b2b2b-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetPredicate",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.LabelPredicate"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/ILabelSource.kt"
        }.content

        assertTrue(binding.contains("fun setPredicate(callback: LabelPredicate)"))
        assertTrue(binding.contains("(String) -> Boolean"))
        assertTrue(binding.contains("WinRtDelegateBridge.createStringArgBooleanDelegate"))
        assertTrue(binding.contains("LabelPredicate.iid"))
        assertTrue(binding.contains("setPredicate(LabelPredicate(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_string_boolean_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "LabelPredicate",
                            kind = WinMdTypeKind.Delegate,
                            guid = "2c2c2c2c-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Boolean",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "String"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "LabelSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetPredicate",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.LabelPredicate"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/LabelSource.kt"
        }.content

        assertTrue(binding.contains("fun setPredicate(callback: LabelPredicate)"))
        assertTrue(binding.contains("(String) -> Boolean"))
        assertTrue(binding.contains("WinRtDelegateBridge.createStringArgBooleanDelegate"))
        assertTrue(binding.contains("LabelPredicate.iid"))
        assertTrue(binding.contains("setPredicate(LabelPredicate(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_uint32_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "IndexHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "cccccccc-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "UInt32"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "IIndexSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "dddddddd-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.IndexHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/IIndexSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: IndexHandler)"))
        assertTrue(binding.contains("(UInt) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createUInt32ArgUnitDelegate(IndexHandler.iid, callback)"))
        assertTrue(binding.contains("setHandler(IndexHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_uint32_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "IndexHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "eeeeeeee-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "UInt32"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "IndexSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.IndexHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/IndexSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: IndexHandler)"))
        assertTrue(binding.contains("(UInt) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createUInt32ArgUnitDelegate(IndexHandler.iid, callback)"))
        assertTrue(binding.contains("setHandler(IndexHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_uint32_boolean_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "IndexPredicate",
                            kind = WinMdTypeKind.Delegate,
                            guid = "2d2d2d2d-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Boolean",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "UInt32"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "IIndexSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "2e2e2e2e-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetPredicate",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.IndexPredicate"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/IIndexSource.kt"
        }.content

        assertTrue(binding.contains("fun setPredicate(callback: IndexPredicate)"))
        assertTrue(binding.contains("(UInt) -> Boolean"))
        assertTrue(binding.contains("WinRtDelegateBridge.createUInt32ArgBooleanDelegate"))
        assertTrue(binding.contains("IndexPredicate.iid"))
        assertTrue(binding.contains("setPredicate(IndexPredicate(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_uint32_boolean_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "IndexPredicate",
                            kind = WinMdTypeKind.Delegate,
                            guid = "2f2f2f2f-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Boolean",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "UInt32"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "IndexSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetPredicate",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.IndexPredicate"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/IndexSource.kt"
        }.content

        assertTrue(binding.contains("fun setPredicate(callback: IndexPredicate)"))
        assertTrue(binding.contains("(UInt) -> Boolean"))
        assertTrue(binding.contains("WinRtDelegateBridge.createUInt32ArgBooleanDelegate"))
        assertTrue(binding.contains("IndexPredicate.iid"))
        assertTrue(binding.contains("setPredicate(IndexPredicate(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_float32_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "ProgressHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "12121212-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Float32"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "IProgressSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "13131313-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.ProgressHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/IProgressSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: ProgressHandler)"))
        assertTrue(binding.contains("(Float) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createFloat32ArgUnitDelegate"))
        assertTrue(binding.contains("ProgressHandler.iid"))
        assertTrue(binding.contains("setHandler(ProgressHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_float32_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "ProgressHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "14141414-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Float32"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "ProgressSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.ProgressHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/ProgressSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: ProgressHandler)"))
        assertTrue(binding.contains("(Float) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createFloat32ArgUnitDelegate"))
        assertTrue(binding.contains("ProgressHandler.iid"))
        assertTrue(binding.contains("setHandler(ProgressHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_float64_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "DistanceHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "15151515-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Float64"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "IDistanceSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "16161616-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.DistanceHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/IDistanceSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: DistanceHandler)"))
        assertTrue(binding.contains("(Double) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createFloat64ArgUnitDelegate"))
        assertTrue(binding.contains("DistanceHandler.iid"))
        assertTrue(binding.contains("setHandler(DistanceHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_float64_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "DistanceHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "17171717-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Float64"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "DistanceSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.DistanceHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/DistanceSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: DistanceHandler)"))
        assertTrue(binding.contains("(Double) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createFloat64ArgUnitDelegate"))
        assertTrue(binding.contains("DistanceHandler.iid"))
        assertTrue(binding.contains("setHandler(DistanceHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_boolean_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "ToggleHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "18181818-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Boolean"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "IToggleSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "19191919-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.ToggleHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/IToggleSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: ToggleHandler)"))
        assertTrue(binding.contains("(Boolean) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createBooleanArgUnitDelegate"))
        assertTrue(binding.contains("ToggleHandler.iid"))
        assertTrue(binding.contains("setHandler(ToggleHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_boolean_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "ToggleHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "20202020-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Boolean"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "ToggleSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.ToggleHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/ToggleSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: ToggleHandler)"))
        assertTrue(binding.contains("(Boolean) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createBooleanArgUnitDelegate"))
        assertTrue(binding.contains("ToggleHandler.iid"))
        assertTrue(binding.contains("setHandler(ToggleHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_int64_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "TimestampHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "21212121-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Int64"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "ITimestampSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "22222222-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.TimestampHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/ITimestampSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: TimestampHandler)"))
        assertTrue(binding.contains("(Long) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createInt64ArgUnitDelegate"))
        assertTrue(binding.contains("TimestampHandler.iid"))
        assertTrue(binding.contains("setHandler(TimestampHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_int64_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "TimestampHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "23232323-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Int64"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "TimestampSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.TimestampHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/TimestampSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: TimestampHandler)"))
        assertTrue(binding.contains("(Long) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createInt64ArgUnitDelegate"))
        assertTrue(binding.contains("TimestampHandler.iid"))
        assertTrue(binding.contains("setHandler(TimestampHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_int64_boolean_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "TimestampPredicate",
                            kind = WinMdTypeKind.Delegate,
                            guid = "30303030-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Boolean",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Int64"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "ITimestampSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "31313131-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetPredicate",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.TimestampPredicate"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/ITimestampSource.kt"
        }.content

        assertTrue(binding.contains("fun setPredicate(callback: TimestampPredicate)"))
        assertTrue(binding.contains("(Long) -> Boolean"))
        assertTrue(binding.contains("WinRtDelegateBridge.createInt64ArgBooleanDelegate"))
        assertTrue(binding.contains("TimestampPredicate.iid"))
        assertTrue(binding.contains("setPredicate(TimestampPredicate(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_int64_boolean_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "TimestampPredicate",
                            kind = WinMdTypeKind.Delegate,
                            guid = "32323232-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Boolean",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "Int64"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "TimestampSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetPredicate",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.TimestampPredicate"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/TimestampSource.kt"
        }.content

        assertTrue(binding.contains("fun setPredicate(callback: TimestampPredicate)"))
        assertTrue(binding.contains("(Long) -> Boolean"))
        assertTrue(binding.contains("WinRtDelegateBridge.createInt64ArgBooleanDelegate"))
        assertTrue(binding.contains("TimestampPredicate.iid"))
        assertTrue(binding.contains("setPredicate(TimestampPredicate(delegateHandle.pointer))"))
    }

    @Test
    fun generates_lambda_overload_for_uint64_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "OffsetHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "24242424-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "UInt64"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "IOffsetSource",
                            kind = WinMdTypeKind.Interface,
                            guid = "25252525-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Callbacks.OffsetHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/IOffsetSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: OffsetHandler)"))
        assertTrue(binding.contains("(ULong) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createUInt64ArgUnitDelegate"))
        assertTrue(binding.contains("OffsetHandler.iid"))
        assertTrue(binding.contains("setHandler(OffsetHandler(delegateHandle.pointer))"))
    }

    @Test
    fun generates_runtime_lambda_overload_for_uint64_delegate_parameter() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Runtime",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "OffsetHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "26262626-2222-2222-2222-222222222222",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(name = "value", type = "UInt64"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Example.Runtime",
                            name = "OffsetSource",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod(
                                    name = "SetHandler",
                                    returnType = "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(
                                        WinMdParameter(name = "callback", type = "Example.Runtime.OffsetHandler"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Runtime/OffsetSource.kt"
        }.content

        assertTrue(binding.contains("fun setHandler(callback: OffsetHandler)"))
        assertTrue(binding.contains("(ULong) -> Unit"))
        assertTrue(binding.contains("WinRtDelegateBridge.createUInt64ArgUnitDelegate"))
        assertTrue(binding.contains("OffsetHandler.iid"))
        assertTrue(binding.contains("setHandler(OffsetHandler(delegateHandle.pointer))"))
    }

    @Test
    fun reads_winui_ui_element_collection_runtime_class_when_available() {
        val winUiRoot = java.nio.file.Path.of("F:/Dependencies/nuget/microsoft.windowsappsdk/1.6.240923002")
        val xamlWinmd = winUiRoot.resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd")
        if (!java.nio.file.Files.isRegularFile(xamlWinmd)) {
            return
        }

        val universalContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.UniversalApiContract",
            sdkVersion = "10.0.22621.0",
        )
        val foundationContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.FoundationContract",
            sdkVersion = "10.0.22621.0",
        )
        val model = WinMdModelFactory.metadataModel(
            listOf(
                xamlWinmd,
                universalContract.winmdPath,
                foundationContract.winmdPath,
            ),
        )

        val runtimeClass = model.namespaces
            .first { it.name == "Microsoft.UI.Xaml.Controls" }
            .types.first { it.name == "UIElementCollection" }

        assertEquals("Microsoft.UI.Xaml.Controls.IUIElementCollection", runtimeClass.defaultInterface)
        assertTrue(
            runtimeClass.implementedInterfaces.toString(),
            runtimeClass.implementedInterfaces.contains("Microsoft.UI.Xaml.Controls.IUIElementCollection"),
        )
        assertTrue(
            runtimeClass.implementedInterfaces.toString(),
            runtimeClass.implementedInterfaces.any {
                it.startsWith("Windows.Foundation.Collections.IVector`1<Microsoft.UI.Xaml.UIElement>")
            },
        )
        val renderedSignatures = runtimeClass.methods.joinToString(separator = "\n") { method ->
            "${method.name}(${method.parameters.joinToString(",") { it.type }}):${method.returnType}"
        }
        val appendMethod = runtimeClass.methods.firstOrNull { it.name == "Append" }
        assertTrue(runtimeClass.methods.toString(), appendMethod != null)
        assertTrue(
            runtimeClass.methods.toString(),
            appendMethod!!.sourceInterface == null ||
            appendMethod.sourceInterface == "Microsoft.UI.Xaml.Controls.IUIElementCollection" ||
            appendMethod.sourceInterface?.startsWith("Windows.Foundation.Collections.IVector`1<Microsoft.UI.Xaml.UIElement>") == true ||
                appendMethod.sourceInterface == "Microsoft.UI.Xaml.Interop.IBindableVector",
        )
        assertTrue(renderedSignatures, renderedSignatures.contains("Append(Microsoft.UI.Xaml.UIElement):Unit"))
        assertTrue(renderedSignatures, renderedSignatures.contains("GetAt(UInt32):Microsoft.UI.Xaml.UIElement"))
    }

    @Test
    fun merged_json_object_generation_keeps_verified_surface() {
        val universalContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.UniversalApiContract",
            sdkVersion = "10.0.22621.0",
        )
        val foundationContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.FoundationContract",
            sdkVersion = "10.0.22621.0",
        )
        val model = WinMdModelFactory.merge(
            primary = WinMdModelFactory.metadataModel(
                listOf(
                    universalContract.winmdPath,
                    foundationContract.winmdPath,
                ),
            ),
            supplemental = WinMdModelFactory.sampleSupplementalModel(),
        )

        val files = KotlinBindingGenerator().generate(model)
        val jsonObjectBinding = files.first { it.relativePath == "Windows/Data/Json/IJsonObject.kt" }.content

        assertTrue(jsonObjectBinding.contains("fun getNamedValue(name: String): IJsonValue"))
        assertTrue(jsonObjectBinding.contains("invokeObjectMethodWithStringArg(pointer, 6, name).getOrThrow()"))
        assertTrue(jsonObjectBinding.contains("fun getNamedObject(name: String): JsonObject"))
        assertTrue(jsonObjectBinding.contains("invokeObjectMethodWithStringArg(pointer, 8, name).getOrThrow()"))
        assertTrue(jsonObjectBinding.contains("fun getNamedArray(name: String): JsonArray"))
        assertTrue(jsonObjectBinding.contains("invokeObjectMethodWithStringArg(pointer, 9, name).getOrThrow()"))
        assertTrue(jsonObjectBinding.contains("fun getNamedString(name: String): String"))
        assertTrue(jsonObjectBinding.contains("invokeHStringMethodWithStringArg(pointer, 10, name).getOrThrow()"))
        assertTrue(jsonObjectBinding.contains("fun getNamedNumber(name: String): Float64"))
        assertTrue(jsonObjectBinding.contains("invokeFloat64MethodWithStringArg(pointer, 11, name).getOrThrow()"))
        assertTrue(jsonObjectBinding.contains("fun getNamedBoolean(name: String): WinRtBoolean"))
        assertTrue(jsonObjectBinding.contains("invokeBooleanMethodWithStringArg(pointer, 12,"))
        assertFalse(jsonObjectBinding.contains("invokeObjectMethodWithStringArg(pointer, 13, name).getOrThrow()"))
        assertFalse(jsonObjectBinding.contains("invokeObjectMethodWithStringArg(pointer, 15, name).getOrThrow()"))
        assertFalse(jsonObjectBinding.contains("invokeObjectMethodWithStringArg(pointer, 16, name).getOrThrow()"))
        assertFalse(jsonObjectBinding.contains("invokeHStringMethodWithStringArg(pointer, 17, name).getOrThrow()"))
        assertFalse(jsonObjectBinding.contains("invokeFloat64MethodWithStringArg(pointer, 18, name).getOrThrow()"))
        assertFalse(jsonObjectBinding.contains("invokeBooleanMethodWithStringArg(pointer, 19,"))
    }

    @Test
    fun generates_enum_return_calls_from_interface_methods() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                dev.winrt.winmd.plugin.WinMdNamespace(
                    name = "Windows.Data.Json",
                    types = listOf(
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "JsonValueType",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.Enum,
                            enumMembers = listOf(
                                dev.winrt.winmd.plugin.WinMdEnumMember("Null", 0),
                                dev.winrt.winmd.plugin.WinMdEnumMember("String", 3),
                            ),
                        ),
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "IJsonValue",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.Interface,
                            guid = "a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e",
                            properties = listOf(
                                dev.winrt.winmd.plugin.WinMdProperty(
                                    name = "ValueType",
                                    type = "Windows.Data.Json.JsonValueType",
                                    mutable = false,
                                    getterVtableIndex = 6,
                                ),
                            ),
                            methods = listOf(
                                dev.winrt.winmd.plugin.WinMdMethod(
                                    name = "Get_ValueType",
                                    returnType = "Windows.Data.Json.JsonValueType",
                                    vtableIndex = 6,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val jsonValueBinding = files.first { it.relativePath == "Windows/Data/Json/IJsonValue.kt" }.content
        val jsonValueTypeBinding = files.first { it.relativePath == "Windows/Data/Json/JsonValueType.kt" }.content

        assertTrue(jsonValueBinding.contains("fun get_ValueType(): JsonValueType"))
        assertTrue(jsonValueTypeBinding.contains("fromValue("))
    }

    @Test
    fun generates_winui_unit_interface_methods() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "UIElement",
                            kind = WinMdTypeKind.RuntimeClass,
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "LaunchActivatedEventArgs",
                            kind = WinMdTypeKind.RuntimeClass,
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "IWindow",
                            kind = WinMdTypeKind.Interface,
                            guid = "61f0ec79-5d52-56b5-86fb-40fa4af288b0",
                            methods = listOf(
                                WinMdMethod("Activate", "Unit", vtableIndex = 26),
                                WinMdMethod(
                                    "put_Title",
                                    "Unit",
                                    vtableIndex = 15,
                                    parameters = listOf(WinMdParameter("value", "String")),
                                ),
                                WinMdMethod(
                                    "put_Content",
                                    "Unit",
                                    vtableIndex = 9,
                                    parameters = listOf(WinMdParameter("value", "Microsoft.UI.Xaml.UIElement")),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "IApplicationOverrides",
                            kind = WinMdTypeKind.Interface,
                            guid = "a33e81ef-c665-503b-8827-d27ef1720a06",
                            methods = listOf(
                                WinMdMethod(
                                    "OnLaunched",
                                    "Unit",
                                    vtableIndex = 6,
                                    parameters = listOf(WinMdParameter("args", "Microsoft.UI.Xaml.LaunchActivatedEventArgs")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val iWindowBinding = files.first { it.relativePath == "Microsoft/UI/Xaml/IWindow.kt" }.content
        val iOverridesBinding = files.first { it.relativePath == "Microsoft/UI/Xaml/IApplicationOverrides.kt" }.content
        assertTrue(iWindowBinding.contains("fun activate()"))
        assertTrue(iWindowBinding.contains("PlatformComInterop.invokeUnitMethod(pointer, 26).getOrThrow()"))
        assertTrue(iWindowBinding.contains("fun put_Title("))
        assertTrue(iWindowBinding.contains("PlatformComInterop.invokeStringSetter(pointer, 15,"))
        assertTrue(iWindowBinding.contains("fun put_Content("))
        assertTrue(iWindowBinding.contains("PlatformComInterop.invokeObjectSetter(pointer, 9,"))
        assertTrue(iOverridesBinding.contains("fun onLaunched("))
        assertTrue(iOverridesBinding.contains("PlatformComInterop.invokeObjectSetter(pointer, 6,"))
    }

    @Test
    fun generates_delegate_types_as_projection_metadata() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "ApplicationInitializationCallback",
                            kind = WinMdTypeKind.Delegate,
                            guid = "d8eef1c9-1234-56f1-9963-45dd9c80a661",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                    parameters = listOf(
                                        WinMdParameter(
                                            name = "value",
                                            type = "Microsoft.UI.Xaml.IApplicationInitializationCallbackParams",
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val callbackBinding = files.first {
            it.relativePath == "Microsoft/UI/Xaml/ApplicationInitializationCallback.kt"
        }.content

        assertTrue(callbackBinding.contains("class ApplicationInitializationCallback("))
        assertTrue(callbackBinding.contains("typealias ApplicationInitializationCallbackHandler ="))
        assertTrue(callbackBinding.contains("IApplicationInitializationCallbackParams) -> Unit"))
        assertTrue(callbackBinding.contains(": WinRtInterfaceProjection(pointer)"))
        assertTrue(callbackBinding.contains("override val iid: Guid = guidOf(\"d8eef1c9-1234-56f1-9963-45dd9c80a661\")"))
        assertFalse(callbackBinding.contains("WinRtRuntimeClassMetadata"))
        assertFalse(callbackBinding.contains("RuntimeClassId"))
    }

    @Test
    fun generates_kotlin_lambda_alias_for_no_arg_unit_delegate() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Microsoft.UI.Dispatching",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Dispatching",
                            name = "DispatcherQueueHandler",
                            kind = WinMdTypeKind.Delegate,
                            guid = "aaaa1111-2222-3333-4444-555555555555",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Unit",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Microsoft/UI/Dispatching/DispatcherQueueHandler.kt"
        }.content

        assertTrue(binding.contains("typealias DispatcherQueueHandlerHandler = () -> Unit"))
        assertTrue(binding.contains("class DispatcherQueueHandler("))
    }

    @Test
    fun generates_kotlin_lambda_alias_for_scalar_delegate_signature() {
        val model = dev.winrt.winmd.plugin.WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Example.Callbacks",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Callbacks",
                            name = "ScalarCallback",
                            kind = WinMdTypeKind.Delegate,
                            guid = "11111111-2222-3333-4444-555555555555",
                            methods = listOf(
                                WinMdMethod(
                                    name = "Invoke",
                                    returnType = "Boolean",
                                    parameters = listOf(
                                        WinMdParameter(name = "count", type = "Int32"),
                                        WinMdParameter(name = "progress", type = "Float32"),
                                        WinMdParameter(name = "label", type = "String"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val files = KotlinBindingGenerator().generate(model)
        val binding = files.first {
            it.relativePath == "Example/Callbacks/ScalarCallback.kt"
        }.content

        assertTrue(binding.contains("typealias ScalarCallbackHandler ="))
        assertTrue(binding.contains("Int"))
        assertTrue(binding.contains("Float"))
        assertTrue(binding.contains("String"))
        assertTrue(binding.contains("-> Boolean"))
        assertTrue(binding.contains("class ScalarCallback("))
    }
}
