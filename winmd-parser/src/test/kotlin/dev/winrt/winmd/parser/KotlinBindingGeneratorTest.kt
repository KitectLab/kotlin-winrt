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

        assertTrue(files.any { it.relativePath == "Windows/Foundation/Bindings.kt" })
        assertTrue(files.any { it.relativePath == "Microsoft/UI/Xaml/Bindings.kt" })

        val xamlBindings = files.first { it.relativePath == "Microsoft/UI/Xaml/Bindings.kt" }.content
        val foundationBindings = files.first { it.relativePath == "Windows/Foundation/Bindings.kt" }.content
        assertTrue(xamlBindings.contains("open class Window"))
        assertTrue(xamlBindings.contains("fun asIStringable(): IStringable = IStringable.from(this)"))
        assertTrue(xamlBindings.contains("var title: String"))
        assertTrue(xamlBindings.contains("companion object : WinRtRuntimeClassMetadata"))
        assertTrue(xamlBindings.contains("defaultInterfaceName: String? = \"Windows.Foundation.IStringable\""))
        assertTrue(foundationBindings.contains("open class IStringable(pointer: ComPtr) : WinRtInterfaceProjection(pointer)"))
        assertTrue(foundationBindings.contains("fun from(Inspectable: Inspectable): IStringable = Inspectable.projectInterface(this, ::IStringable)"))
        assertTrue(foundationBindings.contains("PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()"))
        assertTrue(foundationBindings.contains("companion object : WinRtInterfaceMetadata"))
        assertTrue(foundationBindings.contains("guidOf(\"96369f54-8eb6-48f0-abce-c1b211e627c3\")"))
        assertFalse(xamlBindings.contains("Unit as"))
    }
}
