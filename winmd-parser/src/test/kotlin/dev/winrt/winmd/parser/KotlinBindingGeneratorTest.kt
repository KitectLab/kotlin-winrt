package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModelFactory
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
        val jsonEnumBinding = files.first { it.relativePath == "Windows/Data/Json/JsonValueType.kt" }.content

        assertTrue(jsonInterfaceBinding.contains("fun getNamedString(name: String): String"))
        assertTrue(jsonInterfaceBinding.contains("PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 10, name).getOrThrow()"))
        assertTrue(jsonInterfaceBinding.contains("fun getNamedNumber(name: String): Float64"))
        assertTrue(jsonInterfaceBinding.contains("invokeFloat64MethodWithStringArg(pointer, 11, name).getOrThrow()"))
        assertTrue(jsonInterfaceBinding.contains("fun getNamedBoolean(name: String): WinRtBoolean"))
        assertTrue(jsonInterfaceBinding.contains("invokeBooleanMethodWithStringArg(pointer, 12,"))
        assertTrue(jsonInterfaceBinding.contains("name).getOrThrow()"))
        assertTrue(jsonValueBinding.contains("fun getNumber(): Float64"))
        assertTrue(jsonValueBinding.contains("PlatformComInterop.invokeFloat64Method(pointer,"))
        assertTrue(jsonValueBinding.contains("9).getOrThrow()"))
        assertTrue(jsonValueBinding.contains("fun getBoolean(): WinRtBoolean"))
        assertTrue(jsonValueBinding.contains("PlatformComInterop.invokeBooleanGetter(pointer, 10).getOrThrow()"))
        assertTrue(jsonEnumBinding.contains("enum class JsonValueType"))
        assertTrue(jsonEnumBinding.contains("Object"))
    }
}
