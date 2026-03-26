package dev.winrt.winmd.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class NuGetPackageReferencesTest {
    @Test
    fun resolves_winmd_files_from_nuget_package_layout() {
        val root = Files.createTempDirectory("nuget-root")
        val packageRoot = root.resolve("microsoft.windowsappsdk").resolve("1.6.0")
        Files.createDirectories(packageRoot.resolve("lib").resolve("uap10.0"))
        Files.write(
            packageRoot.resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd"),
            byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()),
        )

        val resolved = NuGetPackageReferences.resolvePackage(
            packageId = "Microsoft.WindowsAppSDK",
            packageVersion = "1.6.0",
            nugetRoot = root,
        )

        assertEquals("Microsoft.WindowsAppSDK", resolved.packageId)
        assertEquals("1.6.0", resolved.packageVersion)
        assertEquals(1, resolved.winmdFiles.size)
        assertTrue(resolved.winmdFiles.single().toString().endsWith("Microsoft.UI.Xaml.winmd"))
    }
}
