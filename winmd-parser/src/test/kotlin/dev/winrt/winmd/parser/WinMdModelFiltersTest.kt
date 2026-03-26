package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModelFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class WinMdModelFiltersTest {
    @Test
    fun filters_model_to_requested_namespaces() {
        val tempFile = Files.createTempFile("sample", ".winmd")
        Files.write(tempFile, byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()))

        val model = WinMdModelFactory.merge(
            primary = WinMdModelFactory.minimalModel(listOf(tempFile)),
            supplemental = WinMdModelFactory.sampleSupplementalModel(),
        )

        val filtered = WinMdModelFilters.filterNamespaces(
            model = model,
            namespaceFilters = listOf("Windows.Data.Json"),
        )

        assertEquals(listOf("Windows.Data.Json"), filtered.namespaces.map { it.name })
        assertTrue(filtered.namespaces.first().types.isNotEmpty())
    }

    @Test
    fun matches_namespace_prefixes() {
        val model = WinMdModelFactory.sampleSupplementalModel().copy(
            files = emptyList(),
            namespaces = WinMdModelFactory.sampleSupplementalModel().namespaces + listOf(
                dev.winrt.winmd.plugin.WinMdNamespace(
                    name = "Windows.Data.Json.Experimental",
                    types = emptyList(),
                ),
            ),
        )

        val filtered = WinMdModelFilters.filterNamespaces(
            model = model,
            namespaceFilters = listOf("Windows.Data.Json"),
        )

        assertEquals(
            listOf("Windows.Data.Json", "Windows.Data.Json.Experimental"),
            filtered.namespaces.map { it.name },
        )
    }

    @Test
    fun leaves_model_unchanged_without_filters() {
        val tempFile = Files.createTempFile("sample", ".winmd")
        Files.write(tempFile, byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()))

        val model = WinMdModelFactory.merge(
            primary = WinMdModelFactory.minimalModel(listOf(tempFile)),
            supplemental = WinMdModelFactory.sampleSupplementalModel(),
        )

        val filtered = WinMdModelFilters.filterNamespaces(
            model = model,
            namespaceFilters = emptyList(),
        )

        assertEquals(model.namespaces.map { it.name }, filtered.namespaces.map { it.name })
    }
}
