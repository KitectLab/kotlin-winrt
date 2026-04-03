package dev.winrt.sample.jvm

import microsoft.ui.xaml.controls.StackPanel
import microsoft.ui.xaml.controls.TextBlock

data class WinUiSampleLayoutDescriptor(
    val sections: List<String>,
)

data class WinUiSampleLayoutResult(
    val root: StackPanel,
)

object WinUiSampleLayout {
    fun describe(windowTitle: String, messageText: String): WinUiSampleLayoutDescriptor {
        return WinUiSampleLayoutDescriptor(
            sections = listOf(
                "header:$windowTitle",
                "status:bootstrap-ready",
                "summary:$messageText",
                "cards:projection|runtime",
                "footer:winui3-sample",
            ),
        )
    }

    fun build(windowTitle: String, messageText: String): WinUiSampleLayoutResult {
        val root = StackPanel()
        root.children.append(text(windowTitle))
        root.children.append(text("WinUI3 sample dashboard"))
        root.children.append(section("Runtime", "ActivationFactory", "Default interface projection", "Nested panel append"))
        root.children.append(section("Bindings", "Window content", "TextBlock text", "UIElementCollection append"))
        root.children.append(section("Status", "bootstrap-ready", messageText, "sample-jvm-winui3"))
        root.children.append(timeline("Boot", "Activate", "Compose", "Visible"))

        return WinUiSampleLayoutResult(root = root)
    }

    private fun section(title: String, vararg lines: String): StackPanel {
        val panel = StackPanel()
        panel.children.append(text("[$title]"))
        lines.forEach { line ->
            panel.children.append(text(" - $line"))
        }
        return panel
    }

    private fun timeline(vararg steps: String): StackPanel {
        val panel = StackPanel()
        panel.children.append(text("[Timeline]"))
        steps.forEachIndexed { index, step ->
            panel.children.append(text(" ${index + 1}. $step"))
        }
        return panel
    }

    private fun text(value: String): TextBlock =
        TextBlock().also { it.text = value }
}
