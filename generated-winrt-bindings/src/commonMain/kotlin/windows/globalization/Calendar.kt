package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import kotlin.String

public open class Calendar(
  pointer: ComPtr,
) : Inspectable(pointer) {
  public fun asICalendar(): ICalendar = ICalendar.from(this)

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Globalization.Calendar"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Globalization", "Calendar")

    override val defaultInterfaceName: String? = "Windows.Globalization.ICalendar"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    public fun activate(): Calendar = WinRtRuntime.activate(this, ::Calendar)
  }
}
