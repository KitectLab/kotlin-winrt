package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import microsoft.ui.xaml.FrameworkElement

open class Panel(
    pointer: ComPtr,
) : FrameworkElement(pointer) {
    constructor() : this(Companion.activate().pointer)

    val children: UIElementCollection
        get() = get_Children()

    fun get_Children(): UIElementCollection =
        IPanel.from(this).get_Children()

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.Panel"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "Panel")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IPanel"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        fun activate(): Panel = WinRtRuntime.activate(this, ::Panel)
    }
}
