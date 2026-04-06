package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import kotlin.String

public open class ApplicationInitializationCallbackParams(
  pointer: ComPtr,
) : Inspectable(pointer),
    IApplicationInitializationCallbackParams {
  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.ApplicationInitializationCallbackParams"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml",
        "ApplicationInitializationCallbackParams")

    override val defaultInterfaceName: String? =
        "Microsoft.UI.Xaml.IApplicationInitializationCallbackParams"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory
  }
}
