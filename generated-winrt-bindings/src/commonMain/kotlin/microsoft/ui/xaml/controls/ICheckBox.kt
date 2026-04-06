package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.String

public interface ICheckBox {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.ICheckBox"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.ICheckBox"

    override val iid: Guid = guidOf("c5830000-4c9d-5fdd-9346-674c71cd80c5")

    public fun from(inspectable: Inspectable): ICheckBox = inspectable.projectInterface(this,
        ::ICheckBoxProjection)

    public operator fun invoke(inspectable: Inspectable): ICheckBox = from(inspectable)
  }
}

private class ICheckBoxProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    ICheckBox
