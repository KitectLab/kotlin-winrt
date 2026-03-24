package dev.winrt.sample.jvm

import dev.winrt.kom.KomSmoke
import dev.winrt.kom.PlatformRuntime
import microsoft.ui.xaml.Application
import microsoft.ui.xaml.Window

fun main() {
    SampleBootstrap.configure()
    try {
        val app = Application.activate()
        val window = Window.activateInstance()
        window.title = "kotlin-winrt sample"
        app.start()
        window.activate()

        println(KomSmoke.description())
        if (PlatformRuntime.isWindows) {
            println("Sample bootstrap completed with JDK 22+ FFM COM initialization. Replace SampleBootstrap with a real WinUI 3 launcher.")
        } else {
            println("Non-Windows host detected. Static sample path executed without native WinUI 3 launch.")
        }
    } finally {
        SampleBootstrap.shutdown()
    }
}
