package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.Int32
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import windows.foundation.Point

public interface IInsertionPanel {
  public fun getInsertionIndexes(
    position: Point,
    first: Int32,
    second: Int32,
  )

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IInsertionPanel"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IInsertionPanel"

    override val iid: Guid = guidOf("84e13e27-2d24-59c4-a00e-16c7255901e2")

    public fun from(inspectable: Inspectable): IInsertionPanel = inspectable.projectInterface(this,
        ::IInsertionPanelProjection)

    public operator fun invoke(inspectable: Inspectable): IInsertionPanel = from(inspectable)
  }
}

private class IInsertionPanelProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IInsertionPanel {
  override fun getInsertionIndexes(
    position: Point,
    first: Int32,
    second: Int32,
  ) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 6, position.toAbi(), first.value,
        second.value).getOrThrow()
  }
}
