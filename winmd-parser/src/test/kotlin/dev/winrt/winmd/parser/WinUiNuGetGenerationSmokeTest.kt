package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModelFactory
import dev.winrt.winmd.plugin.NuGetPackageReferences
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class WinUiNuGetGenerationSmokeTest {
    private val windowsAppSdkVersion = "1.8.260317003"

    @Test
    fun generates_application_and_window_from_local_windows_app_sdk_winmd_when_available() {
        val winuiWinmd = localWinUiXamlWinmd() ?: return

        val model = WinMdModelFilters.filterNamespaces(
            model = WinMdModelFactory.merge(
                primary = WinMdModelFactory.metadataModel(listOf(winuiWinmd)),
                supplemental = WinMdModelFactory.sampleSupplementalModel(),
            ),
            namespaceFilters = listOf("Microsoft.UI.Xaml"),
        )
        val generatedFiles = KotlinBindingGenerator().generate(model).associateBy { it.relativePath }

        assertNotNull(generatedFiles["Microsoft/UI/Xaml/Application.kt"])
        assertNotNull(generatedFiles["Microsoft/UI/Xaml/Window.kt"])
        assertTrue(generatedFiles["Microsoft/UI/Xaml/Application.kt"]!!.content.contains("class Application"))
        assertTrue(generatedFiles["Microsoft/UI/Xaml/Window.kt"]!!.content.contains("class Window"))
    }

    @Test
    fun generates_additional_application_surface_from_local_windows_app_sdk_winmd_when_available() {
        val winuiWinmd = localWinUiXamlWinmd() ?: return

        val model = WinMdModelFilters.filterNamespaces(
            model = WinMdModelFactory.merge(
                primary = WinMdModelFactory.metadataModel(listOf(winuiWinmd)),
                supplemental = WinMdModelFactory.sampleSupplementalModel(),
            ),
            namespaceFilters = listOf("Microsoft.UI.Xaml"),
        )
        val generatedFiles = KotlinBindingGenerator().generate(model).associateBy { it.relativePath }

        assertNotNull(generatedFiles["Microsoft/UI/Xaml/ApplicationHighContrastAdjustment.kt"])
        assertNotNull(generatedFiles["Microsoft/UI/Xaml/ApplicationTheme.kt"])
        assertNotNull(generatedFiles["Microsoft/UI/Xaml/IApplicationInitializationCallbackParams.kt"])
        assertNotNull(generatedFiles["Microsoft/UI/Xaml/ApplicationInitializationCallbackParams.kt"])
    }

    private fun localWinUiXamlWinmd(): Path? {
        val candidatePaths = buildList {
            System.getProperty("dev.winrt.windowsAppSdkRoot")
                ?.takeIf { it.isNotBlank() }
                ?.let { add(Path.of(it).resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd")) }
            addAll(windowsAppSdkWinmdCandidates("Microsoft.UI.Xaml.winmd"))
        }.distinct()
        return candidatePaths.firstOrNull { Files.isRegularFile(it) }
    }

    private fun windowsAppSdkWinmdCandidates(fileName: String): List<Path> {
        val nugetRoots = buildList {
            add(Path.of("F:/Dependencies/nuget"))
            runCatching { NuGetPackageReferences.discoverPackagesRoot() }.getOrNull()?.let(::add)
        }.distinct()

        return runCatching {
            NuGetPackageReferences.resolvePackageFromRoots(
                packageId = "Microsoft.WindowsAppSDK",
                packageVersion = windowsAppSdkVersion,
                nugetRoots = nugetRoots,
            ).winmdFiles.filter { it.fileName.toString().equals(fileName, ignoreCase = true) }
        }.getOrDefault(emptyList())
    }
}
