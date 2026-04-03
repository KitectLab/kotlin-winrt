package dev.winrt.sample.jvm

import org.junit.Assert.assertEquals
import org.junit.Test

class WinUiSampleLayoutTest {
    @Test
    fun describes_nested_layout_sections() {
        val layout = WinUiSampleLayout.describe(
            windowTitle = "kotlin-winrt sample",
            messageText = "Hello from Kotlin/WinUI 3",
        )

        assertEquals(
            listOf(
                "header:kotlin-winrt sample",
                "status:bootstrap-ready",
                "summary:Hello from Kotlin/WinUI 3",
                "cards:projection|runtime",
                "footer:winui3-sample",
            ),
            layout.sections,
        )
    }
}
