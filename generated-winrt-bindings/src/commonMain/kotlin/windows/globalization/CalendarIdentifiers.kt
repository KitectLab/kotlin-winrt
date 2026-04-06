package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import kotlin.String

public open class CalendarIdentifiers(
  pointer: ComPtr,
) : Inspectable(pointer) {
  private val backing_Julian: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val julian: String
    get() = backing_Julian.get()

  private val backing_Gregorian: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val gregorian: String
    get() = backing_Gregorian.get()

  private val backing_Hebrew: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val hebrew: String
    get() = backing_Hebrew.get()

  private val backing_Hijri: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val hijri: String
    get() = backing_Hijri.get()

  private val backing_Japanese: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val japanese: String
    get() = backing_Japanese.get()

  private val backing_Korean: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val korean: String
    get() = backing_Korean.get()

  private val backing_Taiwan: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val taiwan: String
    get() = backing_Taiwan.get()

  private val backing_Thai: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val thai: String
    get() = backing_Thai.get()

  private val backing_UmAlQura: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val umAlQura: String
    get() = backing_UmAlQura.get()

  private val backing_Persian: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val persian: String
    get() = backing_Persian.get()

  private val backing_ChineseLunar: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val chineseLunar: String
    get() = backing_ChineseLunar.get()

  private val backing_VietnameseLunar: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val vietnameseLunar: String
    get() = backing_VietnameseLunar.get()

  private val backing_TaiwanLunar: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val taiwanLunar: String
    get() = backing_TaiwanLunar.get()

  private val backing_KoreanLunar: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val koreanLunar: String
    get() = backing_KoreanLunar.get()

  private val backing_JapaneseLunar: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val japaneseLunar: String
    get() = backing_JapaneseLunar.get()

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

    private val statics2: ICalendarIdentifiersStatics2 by lazy {
        WinRtRuntime.projectActivationFactory(this, ICalendarIdentifiersStatics2,
        ::ICalendarIdentifiersStatics2) }

    public val persian: String
      get() = statics2.persian

    private val statics3: ICalendarIdentifiersStatics3 by lazy {
        WinRtRuntime.projectActivationFactory(this, ICalendarIdentifiersStatics3,
        ::ICalendarIdentifiersStatics3) }

    public val chineseLunar: String
      get() = statics3.chineseLunar

    public val japaneseLunar: String
      get() = statics3.japaneseLunar

    public val koreanLunar: String
      get() = statics3.koreanLunar

    public val taiwanLunar: String
      get() = statics3.taiwanLunar

    public val vietnameseLunar: String
      get() = statics3.vietnameseLunar
  }
}
