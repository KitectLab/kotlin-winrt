package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr

public open class ClockIdentifiers(
  pointer: ComPtr,
) : Inspectable(pointer) {
  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Globalization.ClockIdentifiers"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Globalization",
        "ClockIdentifiers")

    override val defaultInterfaceName: String? = null

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics: IClockIdentifiersStatics by lazy {
        WinRtRuntime.projectActivationFactory(this, IClockIdentifiersStatics,
        ::IClockIdentifiersStatics) }

    public val twelveHour: String
      get() = statics.twelveHour

    public val twentyFourHour: String
      get() = statics.twentyFourHour
  }
}
