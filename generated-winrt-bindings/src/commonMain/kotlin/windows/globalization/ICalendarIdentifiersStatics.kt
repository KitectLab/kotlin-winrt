package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

internal open class ICalendarIdentifiersStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val gregorian: String
    get() = get_Gregorian()

  public val hebrew: String
    get() = get_Hebrew()

  public val hijri: String
    get() = get_Hijri()

  public val japanese: String
    get() = get_Japanese()

  public val julian: String
    get() = get_Julian()

  public val korean: String
    get() = get_Korean()

  public val taiwan: String
    get() = get_Taiwan()

  public val thai: String
    get() = get_Thai()

  public val umAlQura: String
    get() = get_UmAlQura()

  public fun get_Gregorian(): String = readString(6)

  public fun get_Hebrew(): String = readString(7)

  public fun get_Hijri(): String = readString(8)

  public fun get_Japanese(): String = readString(9)

  public fun get_Julian(): String = readString(10)

  public fun get_Korean(): String = readString(11)

  public fun get_Taiwan(): String = readString(12)

  public fun get_Thai(): String = readString(13)

  public fun get_UmAlQura(): String = readString(14)

  private fun readString(vtableIndex: Int): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, vtableIndex).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Globalization.ICalendarIdentifiersStatics"

    override val iid: Guid = guidOf("80653f68-2cb2-4c1f-b590-f0f52bf4fd1a")

    public fun from(inspectable: Inspectable): ICalendarIdentifiersStatics =
        inspectable.projectInterface(this, ::ICalendarIdentifiersStatics)
  }
}
