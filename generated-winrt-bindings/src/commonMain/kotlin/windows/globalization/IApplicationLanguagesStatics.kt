package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import windows.foundation.collections.StringVectorView

internal open class IApplicationLanguagesStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val primaryLanguageOverride: String
    get() = get_PrimaryLanguageOverride()

  public val languages: StringVectorView
    get() = get_Languages()

  public val manifestLanguages: StringVectorView
    get() = get_ManifestLanguages()

  public fun get_PrimaryLanguageOverride(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  public fun get_Languages(): StringVectorView =
      StringVectorView(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())

  public fun get_ManifestLanguages(): StringVectorView =
      StringVectorView(PlatformComInterop.invokeObjectMethod(pointer, 9).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Globalization.IApplicationLanguagesStatics"

    override val iid: Guid = guidOf("75b40847-0a4c-4a92-9565-fd63c95f7aed")

    public fun from(inspectable: Inspectable): IApplicationLanguagesStatics =
        inspectable.projectInterface(this, ::IApplicationLanguagesStatics)
  }
}
