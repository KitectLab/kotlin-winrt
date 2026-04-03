package dev.winrt.winmd.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class WinMdConfigurationResolverTest {
    @Test
    fun official_adds_nuget_org_source() {
        val extension = WinMdExtension().apply {
            official()
        }

        val resolved = WinMdConfigurationResolver.resolve(extension)

        assertTrue(extension.nugetSources.contains(WinMdExtension.OFFICIAL_NUGET_SOURCE))
        assertTrue(resolved.sourceFiles.isNotEmpty())
    }

    @Test
    fun prefers_explicit_references_root_and_sdk_version() {
        val extension = WinMdExtension().apply {
            referencesRoot = "D:/Windows Kits/10/References"
            sdkVersion = "10.0.22621.0"
            contracts = listOf(
                "Windows.Foundation.UniversalApiContract",
                "Windows.Foundation.FoundationContract",
            )
        }

        val resolved = WinMdConfigurationResolver.resolve(extension)

        assertEquals("10.0.22621.0", resolved.sdkVersion)
        assertEquals(Path.of("D:/Windows Kits/10/References"), resolved.referencesRoot)
        assertEquals(2, resolved.contracts.size)
        assertEquals(2, resolved.sourceFiles.size)
        assertEquals("Windows.Foundation.UniversalApiContract", resolved.contracts[0].contractName)
        assertEquals("Windows.Foundation.FoundationContract", resolved.contracts[1].contractName)
    }

    @Test
    fun derives_references_root_from_windows_kits_root() {
        val extension = WinMdExtension().apply {
            windowsKitsRoot = "D:/Windows Kits/10"
            sdkVersion = "10.0.22621.0"
        }

        val resolved = WinMdConfigurationResolver.resolve(extension)

        assertEquals(Path.of("D:/Windows Kits/10/References"), resolved.referencesRoot)
        assertEquals("10.0.22621.0", resolved.sdkVersion)
        assertEquals(1, resolved.contracts.size)
        assertEquals(1, resolved.sourceFiles.size)
    }

    @Test
    fun falls_back_to_discovery_when_no_paths_are_configured() {
        val resolved = WinMdConfigurationResolver.resolve(WinMdExtension())

        assertTrue(resolved.referencesRoot.toString().endsWith("References"))
        assertTrue(resolved.contracts.isNotEmpty())
        assertEquals(resolved.contracts.map { it.winmdPath }, resolved.sourceFiles)
    }

    @Test
    fun combines_nuget_winmd_and_sdk_contract_sources() {
        val root = Files.createTempDirectory("nuget-root")
        val packageRoot = root.resolve("microsoft.windowsappsdk").resolve("1.6.0")
        Files.createDirectories(packageRoot.resolve("lib").resolve("uap10.0"))
        Files.write(
            packageRoot.resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd"),
            byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()),
        )

        val resolved = WinMdConfigurationResolver.resolve(
            WinMdExtension().apply {
                nugetRoot = root.toString()
                nugetPackageId = "Microsoft.WindowsAppSDK"
                nugetPackageVersion = "1.6.0"
                referencesRoot = "D:/Windows Kits/10/References"
                sdkVersion = "10.0.22621.0"
                contracts = listOf("Windows.Foundation.UniversalApiContract")
            },
        )

        assertTrue(resolved.sourceFiles.any { it.fileName.toString() == "Microsoft.UI.Xaml.winmd" })
        assertTrue(resolved.sourceFiles.any { it.fileName.toString() == "Windows.Foundation.UniversalApiContract.winmd" })
    }

    @Test
    fun combines_multiple_nuget_components_with_sdk_contract_sources() {
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

        val resolved = WinMdConfigurationResolver.resolve(
            WinMdExtension().apply {
                nugetRoot = root.toString()
                nugetComponent("Microsoft.WindowsAppSDK", "1.6.0")
                nugetComponent("Microsoft.Windows.SDK.NET.Ref", "10.0.22621.0")
                referencesRoot = "D:/Windows Kits/10/References"
                sdkVersion = "10.0.22621.0"
                contracts = listOf("Windows.Foundation.UniversalApiContract")
            },
        )

        assertEquals(2, resolved.nugetPackages.size)
        assertTrue(resolved.sourceFiles.any { it.fileName.toString() == "Microsoft.UI.Xaml.winmd" })
        assertTrue(resolved.sourceFiles.any { it.fileName.toString() == "Windows.Foundation.winmd" })
        assertTrue(resolved.sourceFiles.any { it.fileName.toString() == "Windows.Foundation.UniversalApiContract.winmd" })
    }
}
