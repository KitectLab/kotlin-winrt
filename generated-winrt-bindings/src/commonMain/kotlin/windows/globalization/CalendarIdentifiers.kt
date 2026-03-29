package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr

public open class CalendarIdentifiers(
  pointer: ComPtr,
) : Inspectable(pointer) {
  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Globalization.CalendarIdentifiers"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Globalization",
        "CalendarIdentifiers")

    override val defaultInterfaceName: String? = null

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics: ICalendarIdentifiersStatics by lazy {
        WinRtRuntime.projectActivationFactory(this, ICalendarIdentifiersStatics,
        ::ICalendarIdentifiersStatics) }

    public val gregorian: String
      get() = statics.gregorian

    public val hebrew: String
      get() = statics.hebrew

    public val hijri: String
      get() = statics.hijri

    public val japanese: String
      get() = statics.japanese

    public val julian: String
      get() = statics.julian

    public val korean: String
      get() = statics.korean

    public val taiwan: String
      get() = statics.taiwan

    public val thai: String
      get() = statics.thai

    public val umAlQura: String
      get() = statics.umAlQura
  }
}
