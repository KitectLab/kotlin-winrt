package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModelFactory
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class WinUiNuGetGenerationSmokeTest {
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

    private fun localWinUiXamlWinmd(): Path? {
        val candidatePaths = buildList {
            System.getProperty("dev.winrt.windowsAppSdkRoot")
                ?.takeIf { it.isNotBlank() }
                ?.let { add(Path.of(it).resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd")) }
            add(
                Path.of(
                    "F:/Dependencies/nuget/microsoft.windowsappsdk/1.6.240923002/lib/uap10.0/Microsoft.UI.Xaml.winmd",
                ),
            )
        }
        return candidatePaths.firstOrNull { Files.isRegularFile(it) }
    }
}
