package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WindowsSdkReferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class WinMdParserInputResolverTest {
    private val discoveredKitsRoot: String? by lazy {
        WindowsSdkReferences.discoverReferencesRootCandidates().firstOrNull()?.parent?.toString()
    }

    private val discoveredSdkVersion: String? by lazy {
        val referencesRoot = WindowsSdkReferences.discoverReferencesRootCandidates().firstOrNull()
            ?: return@lazy null
        WindowsSdkReferences.discoverInstalledSdkVersions(referencesRoot).lastOrNull()
    }

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
        val kitsRoot = discoveredKitsRoot ?: return
        val sdkVersion = discoveredSdkVersion ?: return
        val inputs = WinMdParserInputResolver.resolve(
            arrayOf(
                "build/generated",
                "--windows-kits-root=$kitsRoot",
                "--sdk-version=$sdkVersion",
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
        val kitsRoot = discoveredKitsRoot ?: return
        val sdkVersion = discoveredSdkVersion ?: return
        val inputs = WinMdParserInputResolver.resolve(
            arrayOf(
                "build/generated",
                "--windows-kits-root=$kitsRoot",
                "--sdk-version=$sdkVersion",
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
                "--nuget-component=Microsoft.WindowsAppSDK@1.6.0",
                "--namespace=Microsoft.UI.Xaml",
            ),
        )

        assertEquals(listOf("Microsoft.UI.Xaml"), inputs.namespaceFilters)
        assertEquals(listOf("Microsoft.UI.Xaml.winmd"), inputs.sources.map { it.fileName.toString() })
    }

    @Test
    fun resolves_sources_from_combined_nuget_and_contract_configuration() {
        val kitsRoot = discoveredKitsRoot ?: return
        val sdkVersion = discoveredSdkVersion ?: return
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
                "--nuget-component=Microsoft.WindowsAppSDK@1.6.0",
                "--windows-kits-root=$kitsRoot",
                "--sdk-version=$sdkVersion",
                "--contract=Windows.Foundation.UniversalApiContract",
                "--namespace=Microsoft.UI.Xaml",
            ),
        )

        assertEquals(listOf("Microsoft.UI.Xaml"), inputs.namespaceFilters)
        assertTrue(inputs.sources.any { it.fileName.toString() == "Microsoft.UI.Xaml.winmd" })
        assertTrue(inputs.sources.any { it.fileName.toString() == "Windows.Foundation.UniversalApiContract.winmd" })
    }

    @Test
    fun resolves_sources_from_multiple_nuget_components() {
        val root = Files.createTempDirectory("nuget-root")
        val appSdkRoot = root.resolve("microsoft.windowsappsdk").resolve("1.6.0")
        Files.createDirectories(appSdkRoot.resolve("lib").resolve("uap10.0"))
        Files.write(
            appSdkRoot.resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd"),
            byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()),
        )
        val windowsSdkRoot = root.resolve("microsoft.windows.sdk.net.ref").resolve("10.0.22621.0")
        Files.createDirectories(windowsSdkRoot.resolve("lib").resolve("uap10.0"))
        Files.write(
            windowsSdkRoot.resolve("lib").resolve("uap10.0").resolve("Windows.Foundation.winmd"),
            byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()),
        )

        val inputs = WinMdParserInputResolver.resolve(
            arrayOf(
                "build/generated",
                "--nuget-root=${root}",
                "--nuget-component=Microsoft.WindowsAppSDK@1.6.0",
                "--nuget-component=Microsoft.Windows.SDK.NET.Ref@10.0.22621.0",
                "--namespace=Microsoft.UI.Xaml",
            ),
        )

        assertEquals(
            listOf("Microsoft.UI.Xaml"),
            inputs.namespaceFilters,
        )
        assertTrue(inputs.sources.any { it.fileName.toString() == "Microsoft.UI.Xaml.winmd" })
        assertTrue(inputs.sources.any { it.fileName.toString() == "Windows.Foundation.winmd" })
    }

    @Test
    fun resolves_sources_from_configured_nuget_source_roots() {
        val sourceRoot = Files.createTempDirectory("nuget-source")
        val packageRoot = sourceRoot.resolve("microsoft.windowsappsdk").resolve("1.6.0")
        Files.createDirectories(packageRoot.resolve("lib").resolve("uap10.0"))
        Files.write(
            packageRoot.resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd"),
            byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()),
        )

        val inputs = WinMdParserInputResolver.resolve(
            arrayOf(
                "build/generated",
                "--nuget-source=${sourceRoot}",
                "--nuget-component=Microsoft.WindowsAppSDK@1.6.0",
                "--namespace=Microsoft.UI.Xaml",
            ),
        )

        assertTrue(inputs.sources.any { it.fileName.toString() == "Microsoft.UI.Xaml.winmd" })
    }
}
