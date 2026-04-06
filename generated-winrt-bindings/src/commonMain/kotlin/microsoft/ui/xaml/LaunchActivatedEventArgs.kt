package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public open class LaunchActivatedEventArgs(
  pointer: ComPtr,
) : Inspectable(pointer) {
  private val backing_Arguments: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val arguments: String
    get() {
      if (pointer.isNull) {
        return backing_Arguments.get()
      }
      return run {
            val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
            try {
              value.toKotlinString()
            } finally {
              value.close()
            }
          }
    }

  private val backing_UWPLaunchActivatedEventArgs:
      RuntimeProperty<windows.applicationmodel.activation.LaunchActivatedEventArgs> =
      RuntimeProperty<windows.applicationmodel.activation.LaunchActivatedEventArgs>(windows.applicationmodel.activation.LaunchActivatedEventArgs(ComPtr.NULL))

  public val uWPLaunchActivatedEventArgs:
      windows.applicationmodel.activation.LaunchActivatedEventArgs
    get() {
      if (pointer.isNull) {
        return backing_UWPLaunchActivatedEventArgs.get()
      }
      return windows.applicationmodel.activation.LaunchActivatedEventArgs(PlatformComInterop.invokeObjectMethod(pointer,
          7).getOrThrow())
    }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.LaunchActivatedEventArgs"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml",
        "LaunchActivatedEventArgs")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.ILaunchActivatedEventArgs"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory
  }
}
