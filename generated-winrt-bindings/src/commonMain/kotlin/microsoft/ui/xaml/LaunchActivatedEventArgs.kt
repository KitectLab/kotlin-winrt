package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr

public open class LaunchActivatedEventArgs(
  pointer: ComPtr,
) : Inspectable(pointer) {
  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.LaunchActivatedEventArgs"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml",
        "LaunchActivatedEventArgs")

    override val defaultInterfaceName: String? = null

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    public fun activate(): LaunchActivatedEventArgs = WinRtRuntime.activate(this,
        ::LaunchActivatedEventArgs)
  }
}
