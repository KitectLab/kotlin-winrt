package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
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
  }
}
