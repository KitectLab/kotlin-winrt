package microsoft.ui.xaml.controls

import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.Guid

interface IButton {
    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IButton"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IButton"
        override val iid: Guid = guidOf("216c183d-d07a-5aa5-b8a4-0300a2683e87")
    }
}
