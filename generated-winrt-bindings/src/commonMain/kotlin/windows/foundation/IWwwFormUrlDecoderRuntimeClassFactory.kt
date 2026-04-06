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

internal open class IWwwFormUrlDecoderRuntimeClassFactory(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun createWwwFormUrlDecoder(query: String): WwwFormUrlDecoder =
      WwwFormUrlDecoder(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6,
      query).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.IWwwFormUrlDecoderRuntimeClassFactory"

    override val projectionTypeKey: String =
        "Windows.Foundation.IWwwFormUrlDecoderRuntimeClassFactory"

    override val iid: Guid = guidOf("5b8c6b3d-24ae-41b5-a1bf-f0c3d544845b")

    public fun from(inspectable: Inspectable): IWwwFormUrlDecoderRuntimeClassFactory =
        inspectable.projectInterface(this, ::IWwwFormUrlDecoderRuntimeClassFactory)

    public operator fun invoke(inspectable: Inspectable): IWwwFormUrlDecoderRuntimeClassFactory =
        from(inspectable)
  }
}
