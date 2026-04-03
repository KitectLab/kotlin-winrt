package dev.winrt.sample.jvm

import microsoft.ui.xaml.controls.StackPanel

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
        println("winui: root stackpanel pointer null=${root.pointer.isNull}")
        val children = root.children
        println("winui: children collection pointer null=${children.pointer.isNull}")
        return WinUiSampleLayoutResult(root = root)
    }
}
