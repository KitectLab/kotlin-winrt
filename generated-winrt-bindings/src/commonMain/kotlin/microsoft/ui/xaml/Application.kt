package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import windows.foundation.IStringable

open class Application(pointer: ComPtr) : Inspectable(pointer) {
    fun start() {
    }

    fun getLaunchCount(): UInt32 = UInt32(0u)

    fun asIStringable(): IStringable = IStringable.from(this)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Application"
        override val classId = RuntimeClassId("Microsoft.UI.Xaml", "Application")
        override val defaultInterfaceName: String? = "Windows.Foundation.IStringable"

        fun activate(): Application = WinRtRuntime.activate(classId, ::Application)
    }
}
