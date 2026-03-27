package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr

open class StackPanel(
    pointer: ComPtr,
) : Panel(pointer) {
    constructor() : this(Companion.activate().pointer)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.StackPanel"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "StackPanel")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IStackPanel"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        fun activate(): StackPanel = WinRtRuntime.activate(this, ::StackPanel)
    }
}
