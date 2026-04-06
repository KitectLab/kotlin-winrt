package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

internal open class IClockIdentifiersStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val twelveHour: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  public val twentyFourHour: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Globalization.IClockIdentifiersStatics"

    override val projectionTypeKey: String = "Windows.Globalization.IClockIdentifiersStatics"

    override val iid: Guid = guidOf("523805bb-12ec-4f83-bc31-b1b4376b0808")

    public fun from(inspectable: Inspectable): IClockIdentifiersStatics =
        inspectable.projectInterface(this, ::IClockIdentifiersStatics)

    public operator fun invoke(inspectable: Inspectable): IClockIdentifiersStatics =
        from(inspectable)
  }
}
