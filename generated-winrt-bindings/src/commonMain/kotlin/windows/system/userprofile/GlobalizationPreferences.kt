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
import windows.foundation.collections.IVectorView
import windows.globalization.DayOfWeek
import windows.system.User

public open class GlobalizationPreferences(
  pointer: ComPtr,
) : Inspectable(pointer) {
  private val backing_HomeGeographicRegion: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val homeGeographicRegion: String
    get() = backing_HomeGeographicRegion.get()

  private val backing_WeekStartsOn: RuntimeProperty<DayOfWeek> =
      RuntimeProperty<DayOfWeek>(DayOfWeek(ComPtr.NULL))

  public val weekStartsOn: DayOfWeek
    get() = backing_WeekStartsOn.get()

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.System.UserProfile.GlobalizationPreferences"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.System.UserProfile",
        "GlobalizationPreferences")

    override val defaultInterfaceName: String? = null

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics: IGlobalizationPreferencesStatics by lazy {
        WinRtRuntime.projectActivationFactory(this, IGlobalizationPreferencesStatics,
        ::IGlobalizationPreferencesStatics) }

    public val calendars: IVectorView<String>
      get() = statics.calendars

    public val clocks: IVectorView<String>
      get() = statics.clocks

    public val currencies: IVectorView<String>
      get() = statics.currencies

    public val homeGeographicRegion: String
      get() = statics.homeGeographicRegion

    public val languages: IVectorView<String>
      get() = statics.languages

    public val weekStartsOn: DayOfWeek
      get() = statics.weekStartsOn

    private val statics2: IGlobalizationPreferencesStatics2 by lazy {
        WinRtRuntime.projectActivationFactory(this, IGlobalizationPreferencesStatics2,
        ::IGlobalizationPreferencesStatics2) }

    private val statics3: IGlobalizationPreferencesStatics3 by lazy {
        WinRtRuntime.projectActivationFactory(this, IGlobalizationPreferencesStatics3,
        ::IGlobalizationPreferencesStatics3) }

    public fun trySetHomeGeographicRegion(region: String): WinRtBoolean =
        statics2.trySetHomeGeographicRegion(region)

    public fun trySetLanguages(languageTags: Iterable<String>): WinRtBoolean =
        statics2.trySetLanguages(languageTags)

    public fun getForUser(user: User): GlobalizationPreferencesForUser = statics3.getForUser(user)
  }
}
