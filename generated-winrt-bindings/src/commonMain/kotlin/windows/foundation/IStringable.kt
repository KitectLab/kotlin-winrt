package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtStrings
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public interface IStringable {
  public override fun toString(): String

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.IStringable"

    override val projectionTypeKey: String = "Windows.Foundation.IStringable"

    override val iid: Guid = guidOf("96369f54-8eb6-48f0-abce-c1b211e627c3")

    public fun from(inspectable: Inspectable): IStringable = inspectable.projectInterface(this,
        ::IStringableProjection)
  }
}

private class IStringableProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer), IStringable {
  override fun toString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }
}
