package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public open class WwwFormUrlDecoderEntry(
  pointer: ComPtr,
) : Inspectable(pointer),
    IWwwFormUrlDecoderEntry {
  private val backing_Name: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val name: String
    get() {
      if (pointer.isNull) {
        return backing_Name.get()
      }
      return run {
            val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
            try {
              value.toKotlinString()
            } finally {
              value.close()
            }
          }
    }

  private val backing_Value: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val value: String
    get() {
      if (pointer.isNull) {
        return backing_Value.get()
      }
      return run {
            val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
            try {
              value.toKotlinString()
            } finally {
              value.close()
            }
          }
    }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Foundation.WwwFormUrlDecoderEntry"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Foundation",
        "WwwFormUrlDecoderEntry")

    override val defaultInterfaceName: String? = "Windows.Foundation.IWwwFormUrlDecoderEntry"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory
  }
}
