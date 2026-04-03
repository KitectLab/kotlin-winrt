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
        val header = TextBlock()
        header.text = windowTitle
        root.children.append(header)

        val summary = TextBlock()
        summary.text = messageText
        root.children.append(summary)

        return WinUiSampleLayoutResult(root = root)
    }
}
