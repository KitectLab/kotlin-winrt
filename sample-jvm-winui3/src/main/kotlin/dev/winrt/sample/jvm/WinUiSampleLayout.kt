package dev.winrt.sample.jvm

import microsoft.ui.xaml.UIElement
import microsoft.ui.xaml.controls.Border
import microsoft.ui.xaml.controls.Button
import microsoft.ui.xaml.controls.ContentControl
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
                "cards:hero|projection|controls|verification",
                "footer:winui3-sample",
            ),
        )
    }

    fun build(windowTitle: String, messageText: String): WinUiSampleLayoutResult {
        println("winui: layout build start")
        val root = StackPanel()
        println("winui: layout root stackpanel created")
        root.children.append(
            card(
                "Hero",
                text(windowTitle),
                contentHost(text("WinUI 3 resources, backdrop, and nested controls")),
                text(messageText),
            ),
        )
        println("winui: layout hero card appended")
        root.children.append(
            card(
                "Projection",
                text("Activatable XamlControlsResources initialization"),
                text("Composable Window construction for backdrop hosting"),
                text("UIElementCollection append on WinUI3 runtime"),
            ),
        )
        println("winui: layout projection card appended")
        root.children.append(
            card(
                "Controls",
                labeled("Button", button("Run diagnostics")),
                surface("Nested Text", contentHost(text("Composable resource lookup works in nested content"))),
                labeled("Secondary Action", button("Open details")),
                labeled("ContentControl", contentHost(button("Nested action"))),
            ),
        )
        println("winui: layout controls card appended")
        root.children.append(
            card(
                "Verification",
                surface("Resources", text("XamlControlsResources attached during Application init")),
                surface("Backdrop", text("Mica requested first, DesktopAcrylic as fallback")),
                surface("Status", timeline("Boot", "Resources", "Controls", "Backdrop", "Visible")),
            ),
        )
        println("winui: layout verification card appended")

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

    private fun surface(title: String, child: UIElement): Border =
        card(title, child)

    private fun timeline(vararg steps: String): StackPanel {
        val panel = StackPanel()
        steps.forEachIndexed { index, step -> panel.children.append(text("${index + 1}. $step")) }
        return panel
    }

    private fun button(label: String): Button =
        Button().also {
            println("winui: create Button label=$label")
            it.content = text(label)
            println("winui: button content set label=$label")
        }

    private fun contentHost(content: UIElement): ContentControl =
        ContentControl().also {
            println("winui: create ContentControl")
            it.content = content
            println("winui: contentcontrol content set")
        }

    private fun text(value: String): TextBlock =
        TextBlock().also {
            println("winui: create TextBlock text=$value")
            it.text = value
        }
}
