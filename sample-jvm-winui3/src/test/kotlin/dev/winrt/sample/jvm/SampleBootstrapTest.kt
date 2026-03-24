package dev.winrt.sample.jvm

import microsoft.ui.xaml.Window
import org.junit.Assert.assertEquals
import org.junit.Test

class SampleBootstrapTest {
    @Test
    fun sample_can_activate_placeholder_window() {
        SampleBootstrap.configure()

        val window = Window.activateInstance()
        window.title = "sample"

        assertEquals("sample", window.title)
    }
}
