package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.String
import kotlin.Unit

public typealias ApplicationInitializationCallbackHandler =
    (ApplicationInitializationCallbackParams) -> Unit

public open class ApplicationInitializationCallback(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.ApplicationInitializationCallback"

    override val iid: Guid = guidOf("d8eef1c9-1234-56f1-9963-45dd9c80a661")

    public fun from(inspectable: Inspectable): ApplicationInitializationCallback =
        inspectable.projectInterface(this, ::ApplicationInitializationCallback)
  }
}
