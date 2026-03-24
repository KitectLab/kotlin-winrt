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
        assertTrue(xamlBindings.contains("open class Window"))
        assertTrue(xamlBindings.contains("var title: String"))
        assertFalse(xamlBindings.contains("Unit as"))
    }
}
