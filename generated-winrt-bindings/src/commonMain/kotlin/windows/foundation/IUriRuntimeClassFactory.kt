package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

internal open class IUriRuntimeClassFactory(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun createUri(uri: String): Uri =
      Uri(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6, uri).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.IUriRuntimeClassFactory"

    override val projectionTypeKey: String = "Windows.Foundation.IUriRuntimeClassFactory"

    override val iid: Guid = guidOf("44a9796f-723e-4fdf-a218-033e75b0c084")

    public fun from(inspectable: Inspectable): IUriRuntimeClassFactory =
        inspectable.projectInterface(this, ::IUriRuntimeClassFactory)

    public operator fun invoke(inspectable: Inspectable): IUriRuntimeClassFactory =
        from(inspectable)
  }
}
