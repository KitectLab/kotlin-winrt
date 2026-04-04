package dev.winrt.winmd.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

private const val WINDOWS_APP_SDK_VERSION = "1.8.260317003"
private const val WINDOWS_APP_SDK_FOUNDATION_VERSION = "1.8.260222000"
private const val WINDOWS_APP_SDK_WINUI_VERSION = "1.8.260224000"

class NuGetPackageReferencesTest {
    @Test
    fun resolves_winmd_files_from_nuget_package_layout() {
        val root = Files.createTempDirectory("nuget-root")
        createPackage(
            root = root,
            packageId = "Microsoft.WindowsAppSDK.WinUI",
            version = WINDOWS_APP_SDK_WINUI_VERSION,
            winmdFiles = listOf("Microsoft.UI.Xaml.winmd"),
            dllFiles = listOf("Microsoft.UI.Xaml.dll"),
        )

        val resolved = NuGetPackageReferences.resolvePackage(
            packageId = "Microsoft.WindowsAppSDK.WinUI",
            packageVersion = WINDOWS_APP_SDK_WINUI_VERSION,
            nugetRoot = root,
        )

        assertEquals("Microsoft.WindowsAppSDK.WinUI", resolved.packageId)
        assertEquals(WINDOWS_APP_SDK_WINUI_VERSION, resolved.packageVersion)
        assertEquals(1, resolved.winmdFiles.size)
        assertEquals(1, resolved.runtimeDllFiles.size)
        assertTrue(resolved.winmdFiles.single().toString().endsWith("Microsoft.UI.Xaml.winmd"))
        assertTrue(resolved.runtimeDllFiles.single().toString().endsWith("Microsoft.UI.Xaml.dll"))
    }

    @Test
    fun resolves_winmd_files_from_nuget_meta_package_dependency_closure() {
        val root = Files.createTempDirectory("nuget-root")
        createPackage(
            root = root,
            packageId = "Microsoft.WindowsAppSDK",
            version = WINDOWS_APP_SDK_VERSION,
            dependencies = listOf(
                "Microsoft.WindowsAppSDK.Foundation" to "[$WINDOWS_APP_SDK_FOUNDATION_VERSION]",
                "Microsoft.WindowsAppSDK.WinUI" to "[$WINDOWS_APP_SDK_WINUI_VERSION]",
            ),
        )
        createPackage(
            root = root,
            packageId = "Microsoft.WindowsAppSDK.Foundation",
            version = WINDOWS_APP_SDK_FOUNDATION_VERSION,
            winmdFiles = listOf("Microsoft.Windows.Foundation.winmd"),
        )
        createPackage(
            root = root,
            packageId = "Microsoft.WindowsAppSDK.WinUI",
            version = WINDOWS_APP_SDK_WINUI_VERSION,
            winmdFiles = listOf("Microsoft.UI.Xaml.winmd"),
            dllFiles = listOf("Microsoft.UI.Xaml.dll"),
        )

        val resolved = NuGetPackageReferences.resolvePackage(
            packageId = "Microsoft.WindowsAppSDK",
            packageVersion = WINDOWS_APP_SDK_VERSION,
            nugetRoot = root,
        )

        assertEquals(root.resolve("microsoft.windowsappsdk").resolve(WINDOWS_APP_SDK_VERSION), resolved.packageRoot)
        assertEquals(
            listOf("Microsoft.UI.Xaml.winmd", "Microsoft.Windows.Foundation.winmd"),
            resolved.winmdFiles.map { it.fileName.toString() }.sorted(),
        )
        assertEquals(listOf("Microsoft.UI.Xaml.dll"), resolved.runtimeDllFiles.map { it.fileName.toString() }.sorted())
    }

    private fun createPackage(
        root: Path,
        packageId: String,
        version: String,
        winmdFiles: List<String> = emptyList(),
        dllFiles: List<String> = emptyList(),
        dependencies: List<Pair<String, String>> = emptyList(),
    ) {
        val packageRoot = root.resolve(packageId.lowercase()).resolve(version)
        Files.createDirectories(packageRoot)
        Files.writeString(
            packageRoot.resolve("${packageId.lowercase()}.nuspec"),
            nuspecContents(packageId, version, dependencies),
        )
        if (winmdFiles.isNotEmpty()) {
            val metadataDir = packageRoot.resolve("metadata")
            Files.createDirectories(metadataDir)
            winmdFiles.forEach { fileName ->
                Files.write(
                    metadataDir.resolve(fileName),
                    byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()),
                )
            }
        }
        if (dllFiles.isNotEmpty()) {
            val runtimeDir = packageRoot.resolve("runtimes").resolve("win-x64").resolve("native")
            Files.createDirectories(runtimeDir)
            dllFiles.forEach { fileName ->
                Files.write(
                    runtimeDir.resolve(fileName),
                    byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()),
                )
            }
        }
    }

    private fun nuspecContents(
        packageId: String,
        version: String,
        dependencies: List<Pair<String, String>>,
    ): String = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<package xmlns="http://schemas.microsoft.com/packaging/2013/05/nuspec.xsd">""")
        appendLine("""  <metadata>""")
        appendLine("""    <id>$packageId</id>""")
        appendLine("""    <version>$version</version>""")
        if (dependencies.isNotEmpty()) {
            appendLine("""    <dependencies>""")
            dependencies.forEach { (dependencyId, dependencyVersion) ->
                appendLine("""      <dependency id="$dependencyId" version="$dependencyVersion" />""")
            }
            appendLine("""    </dependencies>""")
        }
        appendLine("""  </metadata>""")
        appendLine("""</package>""")
    }
}
