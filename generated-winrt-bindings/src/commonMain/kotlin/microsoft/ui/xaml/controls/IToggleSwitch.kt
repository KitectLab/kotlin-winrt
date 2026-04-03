package microsoft.ui.xaml.controls

import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.Guid

interface IToggleSwitch {
    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IToggleSwitch"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IToggleSwitch"
        override val iid: Guid = guidOf("1b17eeb1-74bf-5a83-8161-a86f0fdcdf24")
    }
}
