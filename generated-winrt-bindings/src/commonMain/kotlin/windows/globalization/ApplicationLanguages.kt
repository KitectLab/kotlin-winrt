package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import kotlin.String
import kotlin.collections.List

public open class ApplicationLanguages(
  pointer: ComPtr,
) : Inspectable(pointer) {
  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Globalization.ApplicationLanguages"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Globalization",
        "ApplicationLanguages")

    override val defaultInterfaceName: String? = null

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    val languages: List<String>
      get() = statics.languages

    val manifestLanguages: List<String>
      get() = statics.manifestLanguages

    private val statics: IApplicationLanguagesStatics by lazy {
        WinRtRuntime.projectActivationFactory(this, IApplicationLanguagesStatics,
        ::IApplicationLanguagesStatics)
    }
  }
}
