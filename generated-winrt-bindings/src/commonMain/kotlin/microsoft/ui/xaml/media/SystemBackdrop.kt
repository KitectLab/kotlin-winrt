package microsoft.ui.xaml.media

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import microsoft.ui.xaml.DependencyObject

open class SystemBackdrop(
    pointer: ComPtr,
) : DependencyObject(pointer) {
    constructor() : this(Companion.activate().pointer)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Media.SystemBackdrop"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Media", "SystemBackdrop")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Media.ISystemBackdrop"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        fun activate(): SystemBackdrop = WinRtRuntime.activate(this, ::SystemBackdrop)
    }
}
