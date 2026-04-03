package dev.winrt.sample.jvm

import microsoft.ui.xaml.controls.StackPanel
import microsoft.ui.xaml.controls.TextBlock

data class WinUiSampleLayoutDescriptor(
    val sections: List<String>,
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

    fun build(windowTitle: String, messageText: String): StackPanel {
        val root = StackPanel()

        root.children.add(makeTextBlock("kotlin-winrt sample"))
        root.children.add(makeTextBlock(windowTitle))

        val statusPanel = StackPanel()
        statusPanel.children.add(makeTextBlock("Bootstrap: ready"))
        statusPanel.children.add(makeTextBlock("Projection: WinRT bindings loaded"))
        root.children.add(statusPanel)

        val bodyPanel = StackPanel()
        bodyPanel.children.add(makeTextBlock(messageText))
        bodyPanel.children.add(makeTextBlock("Runtime bridge: active"))
        bodyPanel.children.add(makeTextBlock("Layout: nested StackPanel sections"))
        root.children.add(bodyPanel)

        val metricsPanel = StackPanel()
        metricsPanel.children.add(makeTextBlock("Cards: 2"))
        metricsPanel.children.add(makeTextBlock("Sections: 5"))
        metricsPanel.children.add(makeTextBlock("Mode: sample launch"))
        root.children.add(metricsPanel)

        root.children.add(makeTextBlock("winrt-core + generated-winrt-bindings + kom"))
        return root
    }

    private fun makeTextBlock(text: String): TextBlock {
        return TextBlock().also { block ->
            block.text = text
        }
    }
}
