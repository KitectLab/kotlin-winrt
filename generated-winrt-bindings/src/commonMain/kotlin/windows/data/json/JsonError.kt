package windows.`data`.json

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import kotlin.String

public open class JsonError(
  pointer: ComPtr,
) : Inspectable(pointer) {
  public constructor() : this(Companion.activate().pointer)

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Data.Json.JsonError"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Data.Json", "JsonError")

    override val defaultInterfaceName: String? = null

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    public fun activate(): JsonError = WinRtRuntime.activate(this, ::JsonError)
  }
}
