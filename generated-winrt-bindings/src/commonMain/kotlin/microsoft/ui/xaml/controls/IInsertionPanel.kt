package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

interface IInsertionPanel {
    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IInsertionPanel"

        override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IInsertionPanel"

        override val iid: Guid = guidOf("84e13e27-2d24-59c4-a00e-16c7255901e2")

        fun from(inspectable: Inspectable): IInsertionPanel =
            inspectable.projectInterface(this, ::IInsertionPanelProjection)

        operator fun invoke(inspectable: Inspectable): IInsertionPanel = from(inspectable)
    }
}

private class IInsertionPanelProjection(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer), IInsertionPanel
