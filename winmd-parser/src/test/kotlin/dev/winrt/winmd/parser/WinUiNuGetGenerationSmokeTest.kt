package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModelFactory
import dev.winrt.winmd.plugin.NuGetPackageReferences
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
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

    @Test
    fun generates_toggle_switch_surface_from_local_windows_app_sdk_winmd_when_available() {
        val winuiWinmd = localWinUiXamlWinmd() ?: return

        val model = WinMdModelFilters.filterNamespaces(
            model = WinMdModelFactory.merge(
                primary = WinMdModelFactory.metadataModel(listOf(winuiWinmd)),
                supplemental = WinMdModelFactory.sampleSupplementalModel(),
            ),
            namespaceFilters = listOf(
                "Microsoft.UI.Xaml",
                "Microsoft.UI.Xaml.Controls",
                "Microsoft.UI.Xaml.Controls.Primitives",
            ),
        )
        val generatedFiles = KotlinBindingGenerator().generate(model).associateBy { it.relativePath }

        val runtimeBinding = generatedFiles["Microsoft/UI/Xaml/Controls/ToggleSwitch.kt"]
        val interfaceBinding = generatedFiles["Microsoft/UI/Xaml/Controls/IToggleSwitch.kt"]
        val staticsBinding = generatedFiles["Microsoft/UI/Xaml/Controls/IToggleSwitchStatics.kt"]
        val templateSettingsBinding = generatedFiles["Microsoft/UI/Xaml/Controls/Primitives/ToggleSwitchTemplateSettings.kt"]

        assertNotNull(runtimeBinding)
        assertNotNull(interfaceBinding)
        assertNotNull(staticsBinding)
        assertNotNull(templateSettingsBinding)

        val runtimeContent = runtimeBinding!!.content
        val interfaceContent = interfaceBinding!!.content
        val staticsContent = staticsBinding!!.content
        val templateSettingsContent = templateSettingsBinding!!.content

        assertTrue(runtimeContent.contains("override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory"))
        assertTrue(runtimeContent.contains("constructor() : this(Companion.activate().pointer)"))
        assertFalse(runtimeContent.contains("WinRtRuntime.compose("))
        if (!runtimeContent.contains("var header: Inspectable")) {
            println(runtimeContent)
        }
        assertTrue(runtimeContent.contains("var header: Inspectable"))
        assertTrue(runtimeContent.contains("var headerTemplate: DataTemplate"))
        assertTrue(runtimeContent.contains("var onContentTemplate: DataTemplate"))
        assertTrue(runtimeContent.contains("var offContentTemplate: DataTemplate"))
        assertTrue(runtimeContent.contains("val templateSettings: ToggleSwitchTemplateSettings"))
        assertTrue(runtimeContent.contains("fun add_Toggled(handler: RoutedEventHandler): EventRegistrationToken"))
        assertTrue(runtimeContent.contains("fun remove_Toggled(token: EventRegistrationToken)"))

        assertTrue(interfaceContent.contains("var header: Inspectable"))
        assertTrue(interfaceContent.contains("var headerTemplate: DataTemplate"))
        assertTrue(interfaceContent.contains("var onContentTemplate: DataTemplate"))
        assertTrue(interfaceContent.contains("var offContentTemplate: DataTemplate"))
        assertTrue(interfaceContent.contains("val templateSettings: ToggleSwitchTemplateSettings"))
        assertTrue(interfaceContent.contains("fun add_Toggled(handler: RoutedEventHandler): EventRegistrationToken"))
        assertTrue(interfaceContent.contains("fun remove_Toggled(token: EventRegistrationToken)"))

        assertTrue(staticsContent.contains("val isOnProperty: DependencyProperty"))
        assertTrue(staticsContent.contains("val headerProperty: DependencyProperty"))
        assertTrue(staticsContent.contains("val headerTemplateProperty: DependencyProperty"))
        assertTrue(staticsContent.contains("val onContentTemplateProperty: DependencyProperty"))
        assertTrue(staticsContent.contains("val offContentTemplateProperty: DependencyProperty"))

        assertTrue(templateSettingsContent.contains("class ToggleSwitchTemplateSettings"))
    }

    @Test
    fun generates_external_struct_members_from_local_windows_app_sdk_winmd_when_available() {
        val winuiWinmd = localWinUiXamlWinmd() ?: return

        val model = WinMdModelFilters.filterNamespaces(
            model = WinMdModelFactory.merge(
                primary = WinMdModelFactory.metadataModel(listOf(winuiWinmd)),
                supplemental = WinMdModelFactory.sampleSupplementalModel(),
            ),
            namespaceFilters = listOf(
                "Microsoft.UI.Xaml",
                "Microsoft.UI.Xaml.Controls",
                "Microsoft.UI.Xaml.Hosting",
                "Microsoft.UI.Xaml.Input",
            ),
        )
        val generatedFiles = KotlinBindingGenerator().generate(model).associateBy { it.relativePath }

        val keyArgsBinding = generatedFiles["Microsoft/UI/Xaml/Input/IKeyRoutedEventArgs.kt"]
        val hostingBinding = generatedFiles["Microsoft/UI/Xaml/Hosting/IDesktopWindowXamlSource.kt"]
        val tearOutBinding = generatedFiles["Microsoft/UI/Xaml/Controls/TabViewTabTearOutRequestedEventArgs.kt"]

        assertNotNull(keyArgsBinding)
        assertNotNull(hostingBinding)
        assertNotNull(tearOutBinding)

        val keyArgsContent = keyArgsBinding!!.content.replace(Regex("\\s+"), "")
        val hostingContent = hostingBinding!!.content.replace(Regex("\\s+"), "")
        val tearOutContent = tearOutBinding!!.content.replace(Regex("\\s+"), "")

        assertTrue(keyArgsContent.contains("valkeyStatus:CorePhysicalKeyStatus"))
        assertTrue(keyArgsContent.contains("CorePhysicalKeyStatus.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,7,CorePhysicalKeyStatus.ABI_LAYOUT).getOrThrow())"))

        assertTrue(hostingContent.contains("funinitialize(parentWindowId:WindowId)"))
        assertTrue(hostingContent.contains("PlatformComInterop.invokeUnitMethodWithArgs(pointer,17,parentWindowId.toAbi()).getOrThrow()"))

        assertTrue(tearOutContent.contains("valnewWindowId:WindowId"))
        assertTrue(tearOutContent.contains("WindowId.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,8,WindowId.ABI_LAYOUT).getOrThrow())"))
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
