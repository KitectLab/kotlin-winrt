package windows.globalization.numberformatting

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtStrings
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import windows.foundation.collections.StringVectorView

public open class INumeralSystemTranslator(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val languages: StringVectorView
    get() = get_Languages()

  public val resolvedLanguage: String
    get() = get_ResolvedLanguage()

  public val numeralSystem: String
    get() = get_NumeralSystem()

  public fun get_Languages(): StringVectorView =
      StringVectorView(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

  public fun get_ResolvedLanguage(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun get_NumeralSystem(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 8).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun translateNumerals(value: String): String {
    val translated = PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 10,
        value).getOrThrow()
    return try {
      WinRtStrings.toKotlin(translated)
    } finally {
      WinRtStrings.release(translated)
    }
  }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String =
        "Windows.Globalization.NumberFormatting.INumeralSystemTranslator"

    override val iid: Guid = guidOf("28f5bc2c-8c23-4234-ad2e-fa5a3a426e9b")

    public fun from(inspectable: Inspectable): INumeralSystemTranslator =
        inspectable.projectInterface(this, ::INumeralSystemTranslator)
  }
}
