package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import kotlin.String
import microsoft.ui.xaml.controls.primitives.ToggleButton

public open class CheckBox(
  pointer: ComPtr,
) : ToggleButton(pointer) {
  public constructor() : this(Companion.factoryCreateInstance().pointer)

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.CheckBox"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "CheckBox")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.ICheckBox"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private fun factoryCreateInstance(): CheckBox {
      return WinRtRuntime.compose(this, guidOf("f43ff58d-31d5-5835-af7b-375bc6a9bcf3"),
          guidOf("c5830000-4c9d-5fdd-9346-674c71cd80c5"), ::CheckBox, 6, ComPtr.NULL)
    }
  }
}
