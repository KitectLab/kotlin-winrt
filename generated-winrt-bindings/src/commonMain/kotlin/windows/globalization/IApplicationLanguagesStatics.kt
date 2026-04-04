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

internal open class IApplicationLanguagesStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public var primaryLanguageOverride: String
    get() = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow().use {
        it.toKotlinString() }
    set(value) {
      PlatformComInterop.invokeStringSetter(pointer, 7, value).getOrThrow()
    }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Globalization.IApplicationLanguagesStatics"

    override val projectionTypeKey: String = "Windows.Globalization.IApplicationLanguagesStatics"

    override val iid: Guid = guidOf("75b40847-0a4c-4a92-9565-fd63c95f7aed")

    public fun from(inspectable: Inspectable): IApplicationLanguagesStatics =
        inspectable.projectInterface(this, ::IApplicationLanguagesStatics)

    public operator fun invoke(inspectable: Inspectable): IApplicationLanguagesStatics =
        from(inspectable)
  }
}
