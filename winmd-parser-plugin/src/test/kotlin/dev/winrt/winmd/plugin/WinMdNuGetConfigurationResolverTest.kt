package dev.winrt.winmd.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class WinMdNuGetConfigurationResolverTest {
    @Test
    fun prefers_explicit_winmd_files_over_sdk_discovery() {
        val winmdFile = Files.createTempFile("custom", ".winmd")
        val resolved = WinMdConfigurationResolver.resolve(
            WinMdExtension().apply {
                winmdFiles = listOf(winmdFile.toString())
            },
        )

        assertEquals(listOf(winmdFile), resolved.sourceFiles)
        assertTrue(resolved.contracts.isEmpty())
        assertTrue(resolved.referencesRoot.toString().endsWith("References"))
    }

    @Test
    fun resolves_winmd_files_from_nuget_package() {
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
            },
        )

        assertEquals(1, resolved.nugetPackages.size)
        assertEquals(Path.of(root.toString(), "microsoft.windowsappsdk", "1.6.0"), resolved.nugetPackages.single().packageRoot)
        assertEquals(1, resolved.sourceFiles.size)
        assertTrue(resolved.sourceFiles.single().toString().endsWith("Microsoft.UI.Xaml.winmd"))
    }

    @Test
    fun resolves_multiple_winmd_packages_from_nuget_components() {
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
            },
        )

        assertEquals(2, resolved.nugetPackages.size)
        assertTrue(resolved.sourceFiles.any { it.fileName.toString() == "Microsoft.UI.Xaml.winmd" })
        assertTrue(resolved.sourceFiles.any { it.fileName.toString() == "Windows.Foundation.winmd" })
    }

    @Test
    fun resolves_winmd_files_from_configured_nuget_source_roots() {
        val sourceRoot = Files.createTempDirectory("nuget-source")
        val packageRoot = sourceRoot.resolve("microsoft.windowsappsdk").resolve("1.6.0")
        Files.createDirectories(packageRoot.resolve("lib").resolve("uap10.0"))
        Files.write(
            packageRoot.resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd"),
            byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()),
        )

        val resolved = WinMdConfigurationResolver.resolve(
            WinMdExtension().apply {
                nugetSources = listOf(sourceRoot.toString())
                nugetComponent("Microsoft.WindowsAppSDK", "1.6.0")
            },
        )

        assertEquals(1, resolved.nugetPackages.size)
        assertEquals(sourceRoot.resolve("microsoft.windowsappsdk").resolve("1.6.0"), resolved.nugetPackages.single().packageRoot)
        assertTrue(resolved.sourceFiles.single().toString().endsWith("Microsoft.UI.Xaml.winmd"))
    }
}
