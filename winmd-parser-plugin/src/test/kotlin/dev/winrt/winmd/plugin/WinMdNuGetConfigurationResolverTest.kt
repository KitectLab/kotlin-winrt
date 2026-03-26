package dev.winrt.winmd.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertNull(resolved.referencesRoot)
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

        assertEquals(Path.of(root.toString(), "microsoft.windowsappsdk", "1.6.0"), resolved.nugetPackage!!.packageRoot)
        assertEquals(1, resolved.sourceFiles.size)
        assertTrue(resolved.sourceFiles.single().toString().endsWith("Microsoft.UI.Xaml.winmd"))
    }
}
