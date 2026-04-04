package microsoft.ui.xaml.media

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr

open class MicaBackdrop(
    pointer: ComPtr,
) : SystemBackdrop(pointer) {
    constructor() : this(Companion.activate().pointer)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Media.MicaBackdrop"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Media", "MicaBackdrop")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Media.IMicaBackdrop"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

        fun activate(): MicaBackdrop = WinRtRuntime.activate(this, ::MicaBackdrop)

        private fun factoryCreateInstance(): MicaBackdrop =
            WinRtRuntime.compose(
                this,
                guidOf("774379ce-74bd-59d4-849d-d99c4184d838"),
                guidOf("c156a404-3dac-593a-b1f3-7a33c289dc83"),
                ::MicaBackdrop,
                6,
                ComPtr.NULL,
            )
    }
}
