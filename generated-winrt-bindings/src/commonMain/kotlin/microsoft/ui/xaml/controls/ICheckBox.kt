package microsoft.ui.xaml.controls

import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.Guid

interface ICheckBox {
    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.ICheckBox"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.ICheckBox"
        override val iid: Guid = guidOf("c5830000-4c9d-5fdd-9346-674c71cd80c5")
    }
}
