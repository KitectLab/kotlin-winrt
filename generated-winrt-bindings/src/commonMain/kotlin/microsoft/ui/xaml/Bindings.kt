package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtRuntime
import dev.winrt.kom.ComPtr

open class Application(pointer: ComPtr) : Inspectable(pointer) {
    fun start() {
    }

    companion object {
        private val classId = RuntimeClassId("Microsoft.UI.Xaml", "Application")

        fun activate(): Application = WinRtRuntime.activate(classId, ::Application)
    }
}

open class Window(pointer: ComPtr) : Inspectable(pointer) {
    private val backingTitle = RuntimeProperty("")

    var title: String
        get() = backingTitle.get()
        set(value) {
            backingTitle.set(value)
        }

    fun activate() {
    }

    companion object {
        private val classId = RuntimeClassId("Microsoft.UI.Xaml", "Window")

        fun activateInstance(): Window = WinRtRuntime.activate(classId, ::Window)
    }
}
