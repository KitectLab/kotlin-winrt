package windows.globalization.numberformatting

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import windows.foundation.collections.IVectorView

public interface INumeralSystemTranslator {
  public val languages: IVectorView<String>

  public var numeralSystem: String

  public val resolvedLanguage: String

  public fun translateNumerals(value: String): String

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String =
        "Windows.Globalization.NumberFormatting.INumeralSystemTranslator"

    override val projectionTypeKey: String =
        "Windows.Globalization.NumberFormatting.INumeralSystemTranslator"

    override val iid: Guid = guidOf("28f5bc2c-8c23-4234-ad2e-fa5a3a426e9b")

    public fun from(inspectable: Inspectable): INumeralSystemTranslator =
        inspectable.projectInterface(this, ::INumeralSystemTranslatorProjection)

    public operator fun invoke(inspectable: Inspectable): INumeralSystemTranslator =
        from(inspectable)
  }
}

private class INumeralSystemTranslatorProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    INumeralSystemTranslator {
  override val languages: IVectorView<String>
    get() = IVectorView.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        6).getOrThrow()), "string", "String")

  override var numeralSystem: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 8).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }
    set(value) {
      PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 9, value).getOrThrow()
    }

  override val resolvedLanguage: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override fun translateNumerals(value: String): String {
    val value = PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 10,
        value).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }
}
