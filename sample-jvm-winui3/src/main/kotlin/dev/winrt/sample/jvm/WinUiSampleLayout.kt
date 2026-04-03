package dev.winrt.sample.jvm

import microsoft.ui.xaml.UIElement
import microsoft.ui.xaml.controls.Border
import microsoft.ui.xaml.controls.Button
import microsoft.ui.xaml.controls.CheckBox
import microsoft.ui.xaml.controls.StackPanel
import microsoft.ui.xaml.controls.TextBlock
import microsoft.ui.xaml.controls.ToggleSwitch

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
                "cards:projection|runtime|controls",
                "footer:winui3-sample",
            ),
        )
    }

    fun build(windowTitle: String, messageText: String): WinUiSampleLayoutResult {
        val root = StackPanel()
        root.children.append(card("WinUI3 Sample", text(windowTitle), text("Desktop acrylic with control resources")))
        root.children.append(
            card(
                "Projection",
                text("ActivationFactory default interface projection"),
                text("Versioned Window.systemBackdrop forwarding"),
                text("UIElementCollection append on WinUI3 runtime"),
            ),
        )
        root.children.append(
            card(
                "Controls",
                labeled("Button", button("Run diagnostics")),
                labeled("ToggleSwitch", toggleSwitch()),
                labeled("CheckBox", CheckBox()),
                text(messageText),
            ),
        )
        root.children.append(card("Status", text("bootstrap-ready"), text("sample-jvm-winui3"), timeline("Boot", "Resources", "Compose", "Visible")))

        return WinUiSampleLayoutResult(root = root)
    }

    private fun card(title: String, vararg children: UIElement): Border {
        val panel = StackPanel()
        panel.children.append(text("[$title]"))
        children.forEach { child -> panel.children.append(child) }
        return Border().also { it.child = panel }
    }

    private fun labeled(label: String, control: UIElement): StackPanel {
        val panel = StackPanel()
        panel.children.append(text(label))
        panel.children.append(control)
        return panel
    }

    private fun timeline(vararg steps: String): StackPanel {
        val panel = StackPanel()
        steps.forEachIndexed { index, step -> panel.children.append(text("${index + 1}. $step")) }
        return panel
    }

    private fun button(label: String): Button =
        Button().also { it.content = text(label) }

    private fun toggleSwitch(): ToggleSwitch =
        ToggleSwitch().also {
            it.isOn = true
        }

    private fun text(value: String): TextBlock =
        TextBlock().also { it.text = value }
}
