package microsoft.ui.xaml.controls

import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.Guid

interface IXamlControlsResources {
    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IXamlControlsResources"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IXamlControlsResources"
        override val iid: Guid = guidOf("918ca043-f42c-5805-861b-62d6d1d0c162")
    }
}
