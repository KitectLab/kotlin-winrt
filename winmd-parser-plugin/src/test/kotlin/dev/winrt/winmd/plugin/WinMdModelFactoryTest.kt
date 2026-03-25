package dev.winrt.winmd.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class WinMdModelFactoryTest {
    @Test
    fun merges_real_metadata_with_sample_supplemental_types() {
        val tempFile = Files.createTempFile("sample", ".winmd")
        Files.write(tempFile, byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()))

        val merged = WinMdModelFactory.merge(
            primary = WinMdModelFactory.minimalModel(listOf(tempFile)),
            supplemental = WinMdModelFactory.sampleSupplementalModel(),
        )

        val namespaceNames = merged.namespaces.map { it.name }
        assertTrue(namespaceNames.contains("Windows.Foundation"))
        assertTrue(namespaceNames.contains("Microsoft.UI.Xaml"))

        val xamlTypes = merged.namespaces.first { it.name == "Microsoft.UI.Xaml" }.types
        assertEquals(listOf("Application", "Window"), xamlTypes.map { it.name })
    }
}
