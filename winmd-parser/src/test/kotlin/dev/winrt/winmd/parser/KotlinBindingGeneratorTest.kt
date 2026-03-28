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
        assertTrue(binding.contains("WinRtTypeSignature.parameterizedInterface("))
        assertTrue(binding.contains("\"00000000-0000-0000-0000-000000000001\""))
        assertTrue(binding.contains("arg0Signature"))
        assertTrue(binding.contains("fun iidOf(vararg argumentSignatures: String): Guid"))
        assertTrue(binding.contains("ParameterizedInterfaceId.createFromSignature(signatureOf(*argumentSignatures))"))
        assertTrue(binding.contains("fun metadataOf(arg0Signature: String): WinRtInterfaceMetadata"))
        assertTrue(binding.contains("override val iid: Guid = iidOf(arg0Signature)"))
        assertTrue(binding.contains("fun <T> from(inspectable: Inspectable, arg0Signature: String): `IVector`1`<T>"))
        assertTrue(binding.contains("inspectable.projectInterface(metadataOf(arg0Signature),"))
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
        assertTrue(dispatcherQueueBinding.contains("invokeBooleanMethodWithObjectArg(pointer, 7,"))
        assertTrue(dispatcherQueueBinding.contains("callback.pointer"))
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
        assertTrue(callbackBinding.contains(": WinRtInterfaceProjection(pointer)"))
        assertTrue(callbackBinding.contains("override val iid: Guid = guidOf(\"d8eef1c9-1234-56f1-9963-45dd9c80a661\")"))
        assertFalse(callbackBinding.contains("WinRtRuntimeClassMetadata"))
        assertFalse(callbackBinding.contains("RuntimeClassId"))
    }
}
