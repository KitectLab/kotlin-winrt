package microsoft.ui.xaml

import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.Guid

interface IResourceDictionary {
    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.IResourceDictionary"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.IResourceDictionary"
        override val iid: Guid = guidOf("1b690975-a710-5783-a6e1-15836f6186c2")
    }
}
