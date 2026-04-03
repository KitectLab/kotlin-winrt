package windows.system.userprofile

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import kotlin.String
import kotlin.collections.Iterable
import windows.globalization.DayOfWeek

public open class GlobalizationPreferences(
  pointer: ComPtr,
) : Inspectable(pointer) {
  private val backing_HomeGeographicRegion: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val homeGeographicRegion: String
    get() = backing_HomeGeographicRegion.get()

  public constructor() : this(Companion.activate().pointer)

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.System.UserProfile.GlobalizationPreferences"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.System.UserProfile",
        "GlobalizationPreferences")

    override val defaultInterfaceName: String? = null

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics: IGlobalizationPreferencesStatics by lazy {
        WinRtRuntime.projectActivationFactory(this, IGlobalizationPreferencesStatics,
        ::IGlobalizationPreferencesStatics) }

    public val calendars: List<String>
      get() = statics.calendars

    public val clocks: List<String>
      get() = statics.clocks

    public val currencies: List<String>
      get() = statics.currencies

    public val homeGeographicRegion: String
      get() = statics.homeGeographicRegion

    public val languages: List<String>
      get() = statics.languages

    public val weekStartsOn: DayOfWeek
      get() = statics.weekStartsOn

    private val statics2: IGlobalizationPreferencesStatics2 by lazy {
        WinRtRuntime.projectActivationFactory(this, IGlobalizationPreferencesStatics2,
        ::IGlobalizationPreferencesStatics2) }

    public fun activate(): GlobalizationPreferences = WinRtRuntime.activate(this,
        ::GlobalizationPreferences)

    public fun trySetHomeGeographicRegion(region: String): WinRtBoolean =
        statics2.trySetHomeGeographicRegion(region)

    public fun trySetLanguages(languageTags: Iterable<String>): WinRtBoolean =
        statics2.trySetLanguages(languageTags)
  }
}
