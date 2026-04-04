package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import kotlin.String
import windows.foundation.collections.IVectorView
import windows.system.User

public open class ApplicationLanguages(
  pointer: ComPtr,
) : Inspectable(pointer) {
  private val backing_PrimaryLanguageOverride: RuntimeProperty<String> = RuntimeProperty<String>("")

  public var primaryLanguageOverride: String
    get() = backing_PrimaryLanguageOverride.get()
    set(value) {
      backing_PrimaryLanguageOverride.set(value)
    }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Globalization.ApplicationLanguages"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Globalization",
        "ApplicationLanguages")

    override val defaultInterfaceName: String? = null

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics2: IApplicationLanguagesStatics2 by lazy {
        WinRtRuntime.projectActivationFactory(this, IApplicationLanguagesStatics2,
        ::IApplicationLanguagesStatics2) }

    private val statics: IApplicationLanguagesStatics by lazy {
        WinRtRuntime.projectActivationFactory(this, IApplicationLanguagesStatics,
        ::IApplicationLanguagesStatics) }

    public val languages: IVectorView<String>
      get() = statics.languages

    public val manifestLanguages: IVectorView<String>
      get() = statics.manifestLanguages

    public var primaryLanguageOverride: String
      get() = statics.primaryLanguageOverride
      set(value) {
        statics.primaryLanguageOverride = value
      }

    public fun getLanguagesForUser(user: User): IVectorView<String> =
        statics2.getLanguagesForUser(user)
  }
}
