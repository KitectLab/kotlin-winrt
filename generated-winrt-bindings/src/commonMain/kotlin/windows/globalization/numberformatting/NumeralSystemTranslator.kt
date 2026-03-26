package windows.globalization.numberformatting

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr

public open class NumeralSystemTranslator(
  pointer: ComPtr,
) : Inspectable(pointer) {
  public fun asINumeralSystemTranslator(): INumeralSystemTranslator =
      INumeralSystemTranslator.from(this)

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String =
        "Windows.Globalization.NumberFormatting.NumeralSystemTranslator"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Globalization.NumberFormatting",
        "NumeralSystemTranslator")

    override val defaultInterfaceName: String? =
        "Windows.Globalization.NumberFormatting.INumeralSystemTranslator"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    public fun activate(): NumeralSystemTranslator = WinRtRuntime.activate(this,
        ::NumeralSystemTranslator)
  }
}
