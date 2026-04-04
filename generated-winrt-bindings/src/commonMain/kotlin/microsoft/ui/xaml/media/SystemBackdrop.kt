package microsoft.ui.xaml.media

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
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
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

        fun activate(): SystemBackdrop = WinRtRuntime.activate(this, ::SystemBackdrop)

        private fun factoryCreateInstance(): SystemBackdrop =
            WinRtRuntime.compose(
                this,
                guidOf("1e07656b-fad2-5b29-913f-b6748bc45942"),
                guidOf("5aeed5c4-37ac-5852-b73f-1b76ebc3205f"),
                ::SystemBackdrop,
                6,
                ComPtr.NULL,
            )
    }
}
