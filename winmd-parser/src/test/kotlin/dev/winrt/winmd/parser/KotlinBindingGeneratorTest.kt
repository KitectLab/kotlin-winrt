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

        val model = WinMdModelFactory.minimalModel(listOf(tempFile))
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
        assertTrue(asyncStatusBinding.contains("enum class AsyncStatus(val value: Int)"))
        assertTrue(asyncStatusBinding.contains("Started(0)"))
        assertTrue(asyncStatusBinding.contains("Completed(1)"))
        assertTrue(applicationBinding.contains("open class Application"))
        assertTrue(applicationBinding.contains("fun getLaunchCount(): UInt32 = UInt32(0u)"))
        assertTrue(windowBinding.contains("open class Window"))
        assertTrue(windowBinding.contains("fun asIStringable(): IStringable = IStringable.from(this)"))
        assertTrue(windowBinding.contains("var title: String"))
        assertTrue(windowBinding.contains("val isVisible: WinRtBoolean"))
        assertTrue(windowBinding.contains("val createdAt: DateTime"))
        assertTrue(windowBinding.contains("val lifetime: TimeSpan"))
        assertTrue(windowBinding.contains("val lastToken: EventRegistrationToken"))
        assertTrue(windowBinding.contains("val stableId: GuidValue"))
        assertTrue(windowBinding.contains("val optionalTitle: IReference<String>"))
        assertTrue(windowBinding.contains("companion object : WinRtRuntimeClassMetadata"))
        assertTrue(windowBinding.contains("defaultInterfaceName: String? = \"Windows.Foundation.IStringable\""))
        assertTrue(iStringableBinding.contains("open class IStringable(pointer: ComPtr) : WinRtInterfaceProjection(pointer)"))
        assertTrue(iStringableBinding.contains("fun from(Inspectable: Inspectable): IStringable = Inspectable.projectInterface(this, ::IStringable)"))
        assertTrue(iStringableBinding.contains("PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()"))
        assertTrue(iStringableBinding.contains("companion object : WinRtInterfaceMetadata"))
        assertTrue(iStringableBinding.contains("guidOf(\"96369f54-8eb6-48f0-abce-c1b211e627c3\")"))
        assertFalse(windowBinding.contains("Unit as"))
    }
}
