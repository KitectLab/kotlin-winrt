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
        root.children.add(makeTextBlock("Bootstrap: ready"))
        root.children.add(makeTextBlock(messageText))
        root.children.add(makeTextBlock("Runtime bridge: active"))
        root.children.add(makeTextBlock("winrt-core + generated-winrt-bindings + kom"))
        return root
    }

    private fun makeTextBlock(text: String): TextBlock {
        return TextBlock().also { block ->
            block.text = text
        }
    }
}
