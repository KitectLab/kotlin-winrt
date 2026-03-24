package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.WinRtRuntime
import windows.foundation.IStringable
import dev.winrt.kom.ComPtr

open class Application(pointer: ComPtr) : Inspectable(pointer) {
    fun start() {
    }

    fun asIStringable(): IStringable = IStringable.from(this)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Application"
        override val classId = RuntimeClassId("Microsoft.UI.Xaml", "Application")
        override val defaultInterfaceName: String? = "Windows.Foundation.IStringable"

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

    fun asIStringable(): IStringable = IStringable.from(this)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Window"
        override val classId = RuntimeClassId("Microsoft.UI.Xaml", "Window")
        override val defaultInterfaceName: String? = "Windows.Foundation.IStringable"

        fun activateInstance(): Window = WinRtRuntime.activate(classId, ::Window)
    }
}
