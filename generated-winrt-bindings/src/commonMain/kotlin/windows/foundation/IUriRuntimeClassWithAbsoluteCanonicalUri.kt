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

public interface IUriRuntimeClassWithAbsoluteCanonicalUri {
  public val absoluteCanonicalUri: String

  public val displayIri: String

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String =
        "Windows.Foundation.IUriRuntimeClassWithAbsoluteCanonicalUri"

    override val projectionTypeKey: String =
        "Windows.Foundation.IUriRuntimeClassWithAbsoluteCanonicalUri"

    override val iid: Guid = guidOf("758d9661-221c-480f-a339-50656673f46f")

    public fun from(inspectable: Inspectable): IUriRuntimeClassWithAbsoluteCanonicalUri =
        inspectable.projectInterface(this, ::IUriRuntimeClassWithAbsoluteCanonicalUriProjection)

    public operator fun invoke(inspectable: Inspectable): IUriRuntimeClassWithAbsoluteCanonicalUri =
        from(inspectable)
  }
}

private class IUriRuntimeClassWithAbsoluteCanonicalUriProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IUriRuntimeClassWithAbsoluteCanonicalUri {
  override val absoluteCanonicalUri: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val displayIri: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }
}
