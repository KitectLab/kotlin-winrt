package microsoft.ui.xaml.controls

import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.Guid

interface IBorder {
    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IBorder"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IBorder"
        override val iid: Guid = guidOf("1ca13b47-ff5c-5abc-a411-a177df9482a9")
    }
}
