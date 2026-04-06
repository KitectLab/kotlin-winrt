package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import kotlin.collections.Iterator

public open class WwwFormUrlDecoder(
  pointer: ComPtr,
) : Inspectable(pointer),
    IWwwFormUrlDecoderRuntimeClass {
  private val backing_Size: RuntimeProperty<UInt32> = RuntimeProperty<UInt32>(UInt32(0u))

  override val size: UInt32
    get() {
      if (pointer.isNull) {
        return backing_Size.get()
      }
      return UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
    }

  public constructor(query: String) : this(Companion.factoryCreateWwwFormUrlDecoder(query).pointer)

  override fun getFirstValueByName(name: String): String {
    if (pointer.isNull) {
      return ""
    }
    return run {
          val value = PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 12,
              name).getOrThrow()
          try {
            value.toKotlinString()
          } finally {
            value.close()
          }
        }
  }

  override fun getAt(index: UInt32): IWwwFormUrlDecoderEntry {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetAt")
    }
    return IWwwFormUrlDecoderEntry.from(Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer,
        7, index.value).getOrThrow()))
  }

  override fun winRtIndexOf(value: IWwwFormUrlDecoderEntry): UInt32? {
    if (pointer.isNull) {
      return null
    }
    val (found, index) = PlatformComInterop.invokeIndexOfMethod(pointer, 9,
        projectedObjectArgumentPointer(value, "Windows.Foundation.IWwwFormUrlDecoderEntry",
        "{125e7431-f678-4e8e-b670-20a9b06c512d}")).getOrThrow()
    return if (found) UInt32(index) else null
  }

  override fun first(): Iterator<IWwwFormUrlDecoderEntry> {
    if (pointer.isNull) {
      error("Null runtime object pointer: First")
    }
    return Iterator<IWwwFormUrlDecoderEntry>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        6).getOrThrow()))
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Foundation.WwwFormUrlDecoder"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Foundation", "WwwFormUrlDecoder")

    override val defaultInterfaceName: String? = "Windows.Foundation.IWwwFormUrlDecoderRuntimeClass"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val factory: IWwwFormUrlDecoderRuntimeClassFactory by lazy {
        WinRtRuntime.projectActivationFactory(this, IWwwFormUrlDecoderRuntimeClassFactory,
        ::IWwwFormUrlDecoderRuntimeClassFactory) }

    private fun factoryCreateWwwFormUrlDecoder(query: String): WwwFormUrlDecoder =
        factory.createWwwFormUrlDecoder(query)
  }
}
