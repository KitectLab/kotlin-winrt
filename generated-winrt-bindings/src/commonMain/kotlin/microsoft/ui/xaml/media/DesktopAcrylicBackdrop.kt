package microsoft.ui.xaml.media

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import kotlin.String

public open class DesktopAcrylicBackdrop(
  pointer: ComPtr,
) : SystemBackdrop(pointer) {
  public constructor() : this(Companion.factoryCreateInstance().pointer)

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Media.DesktopAcrylicBackdrop"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Media",
        "DesktopAcrylicBackdrop")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Media.IDesktopAcrylicBackdrop"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private fun factoryCreateInstance(): DesktopAcrylicBackdrop {
      return WinRtRuntime.compose(this, guidOf("00922e6d-ae51-564a-bce2-1973d5e463dd"),
          guidOf("bfd9915b-82a6-5df6-aff0-a4824ddc1143"), ::DesktopAcrylicBackdrop, 6, ComPtr.NULL)
    }
  }
}
