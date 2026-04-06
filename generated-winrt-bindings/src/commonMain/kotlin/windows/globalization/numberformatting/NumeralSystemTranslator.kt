package windows.globalization.numberformatting

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import kotlin.collections.Iterable
import windows.foundation.collections.IVectorView

public open class NumeralSystemTranslator(
  pointer: ComPtr,
) : Inspectable(pointer) {
  private val backing_NumeralSystem: RuntimeProperty<String> = RuntimeProperty<String>("")

  public var numeralSystem: String
    get() {
      if (pointer.isNull) {
        return backing_NumeralSystem.get()
      }
      return run {
            val value = PlatformComInterop.invokeHStringMethod(pointer, 8).getOrThrow()
            try {
              value.toKotlinString()
            } finally {
              value.close()
            }
          }
    }
    set(value) {
      if (pointer.isNull) {
        backing_NumeralSystem.set(value)
        return
      }
      PlatformComInterop.invokeStringSetter(pointer, 9, value).getOrThrow()
    }

  private val backing_ResolvedLanguage: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val resolvedLanguage: String
    get() {
      if (pointer.isNull) {
        return backing_ResolvedLanguage.get()
      }
      return run {
            val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
            try {
              value.toKotlinString()
            } finally {
              value.close()
            }
          }
    }

  public constructor(languages: Iterable<String>) : this(Companion.factoryCreate(languages).pointer)

  public fun get_Languages(): IVectorView<String> {
    if (pointer.isNull) {
      error("Null runtime object pointer: get_Languages")
    }
    return IVectorView<String>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        6).getOrThrow()))
  }

  public fun translateNumerals(value: String): String {
    if (pointer.isNull) {
      return ""
    }
    return run {
          val value = PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 10,
              value).getOrThrow()
          try {
            value.toKotlinString()
          } finally {
            value.close()
          }
        }
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String =
        "Windows.Globalization.NumberFormatting.NumeralSystemTranslator"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Globalization.NumberFormatting",
        "NumeralSystemTranslator")

    override val defaultInterfaceName: String? =
        "Windows.Globalization.NumberFormatting.INumeralSystemTranslator"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val factory: INumeralSystemTranslatorFactory by lazy {
        WinRtRuntime.projectActivationFactory(this, INumeralSystemTranslatorFactory,
        ::INumeralSystemTranslatorFactory) }

    private fun factoryCreate(languages: Iterable<String>): NumeralSystemTranslator =
        factory.create(languages)
  }
}
