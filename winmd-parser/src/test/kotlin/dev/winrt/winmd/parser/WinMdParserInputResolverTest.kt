package dev.winrt.winmd.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class WinMdParserInputResolverTest {
    @Test
    fun resolves_explicit_winmd_file_inputs() {
        val source = Files.createTempFile("sample", ".winmd")
        Files.write(source, byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()))

        val inputs = WinMdParserInputResolver.resolve(
            arrayOf("build/generated", source.toString()),
        )

        assertEquals(Path.of("build/generated"), inputs.outputDir)
        assertEquals(listOf(source), inputs.sources)
    }

    @Test
    fun resolves_sources_from_contract_configuration() {
        val inputs = WinMdParserInputResolver.resolve(
            arrayOf(
                "build/generated",
                "--windows-kits-root=D:/Windows Kits/10",
                "--sdk-version=10.0.22621.0",
                "--contract=Windows.Foundation.UniversalApiContract",
                "--contract=Windows.Foundation.FoundationContract",
            ),
        )

        assertEquals(Path.of("build/generated"), inputs.outputDir)
        assertEquals(2, inputs.sources.size)
        assertTrue(inputs.sources.any { it.toString().contains("Windows.Foundation.UniversalApiContract.winmd") })
        assertTrue(inputs.sources.any { it.toString().contains("Windows.Foundation.FoundationContract.winmd") })
    }

    @Test
    fun resolves_namespace_filters() {
        val inputs = WinMdParserInputResolver.resolve(
            arrayOf(
                "build/generated",
                "--windows-kits-root=D:/Windows Kits/10",
                "--sdk-version=10.0.22621.0",
                "--contract=Windows.Foundation.UniversalApiContract",
                "--namespace=Windows.Globalization",
                "--namespace=Windows.Data.Json",
            ),
        )

        assertEquals(
            listOf("Windows.Globalization", "Windows.Data.Json"),
            inputs.namespaceFilters,
        )
    }

    @Test
    fun allows_explicit_winmd_files_with_namespace_filters() {
        val inputs = WinMdParserInputResolver.resolve(
            arrayOf(
                "build/generated",
                "D:/Windows Kits/10/References/sample.winmd",
                "--namespace=Windows.Globalization",
            ),
        )

        assertEquals(listOf("Windows.Globalization"), inputs.namespaceFilters)
        assertEquals(listOf("sample.winmd"), inputs.sources.map { it.fileName.toString() })
    }

    @Test
    fun resolves_sources_from_nuget_package_configuration() {
        val root = Files.createTempDirectory("nuget-root")
        val packageRoot = root.resolve("microsoft.windowsappsdk").resolve("1.6.0")
        Files.createDirectories(packageRoot.resolve("lib").resolve("uap10.0"))
        Files.write(
            packageRoot.resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd"),
            byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()),
        )

        val inputs = WinMdParserInputResolver.resolve(
            arrayOf(
                "build/generated",
                "--nuget-root=${root}",
                "--nuget-package=Microsoft.WindowsAppSDK",
                "--nuget-version=1.6.0",
                "--namespace=Microsoft.UI.Xaml",
            ),
        )

        assertEquals(listOf("Microsoft.UI.Xaml"), inputs.namespaceFilters)
        assertEquals(listOf("Microsoft.UI.Xaml.winmd"), inputs.sources.map { it.fileName.toString() })
    }
}
