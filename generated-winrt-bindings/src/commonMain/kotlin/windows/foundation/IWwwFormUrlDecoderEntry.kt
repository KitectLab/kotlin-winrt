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

public interface IWwwFormUrlDecoderEntry {
  public val name: String

  public val value: String

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.IWwwFormUrlDecoderEntry"

    override val projectionTypeKey: String = "Windows.Foundation.IWwwFormUrlDecoderEntry"

    override val iid: Guid = guidOf("125e7431-f678-4e8e-b670-20a9b06c512d")

    public fun from(inspectable: Inspectable): IWwwFormUrlDecoderEntry =
        inspectable.projectInterface(this, ::IWwwFormUrlDecoderEntryProjection)

    public operator fun invoke(inspectable: Inspectable): IWwwFormUrlDecoderEntry =
        from(inspectable)
  }
}

private class IWwwFormUrlDecoderEntryProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IWwwFormUrlDecoderEntry {
  override val name: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val value: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }
}
