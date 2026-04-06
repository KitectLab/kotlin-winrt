package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireBoolean
import kotlin.String
import kotlin.collections.Iterable
import kotlin.collections.Iterator
import windows.foundation.collections.IIterator
import windows.foundation.collections.IVectorView

public interface IWwwFormUrlDecoderRuntimeClass : IVectorView<IWwwFormUrlDecoderEntry>,
    Iterable<IWwwFormUrlDecoderEntry> {
  public fun getFirstValueByName(name: String): String

  public fun indexOf(value: IWwwFormUrlDecoderEntry, index: UInt32): WinRtBoolean

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.IWwwFormUrlDecoderRuntimeClass"

    override val projectionTypeKey: String = "Windows.Foundation.IWwwFormUrlDecoderRuntimeClass"

    override val iid: Guid = guidOf("d45a0451-f225-4542-9296-0e1df5d254df")

    public fun from(inspectable: Inspectable): IWwwFormUrlDecoderRuntimeClass =
        inspectable.projectInterface(this, ::IWwwFormUrlDecoderRuntimeClassProjection)

    public operator fun invoke(inspectable: Inspectable): IWwwFormUrlDecoderRuntimeClass =
        from(inspectable)
  }
}

private class IWwwFormUrlDecoderRuntimeClassProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IWwwFormUrlDecoderRuntimeClass {
  override val size: UInt32
    get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())

  override fun getFirstValueByName(name: String): String {
    val value = PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 12, name).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun getAt(index: UInt32): IWwwFormUrlDecoderEntry =
      IWwwFormUrlDecoderEntry.from(Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer,
      7, index.value).getOrThrow()))

  override fun indexOf(value: IWwwFormUrlDecoderEntry, index: UInt32): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeMethodWithObjectAndUInt32Args(pointer, 9,
      ComMethodResultKind.BOOLEAN, projectedObjectArgumentPointer(value,
      "Windows.Foundation.IWwwFormUrlDecoderEntry", "{125e7431-f678-4e8e-b670-20a9b06c512d}"),
      index.value).getOrThrow().requireBoolean())

  override fun first(): Iterator<IWwwFormUrlDecoderEntry> =
      IIterator.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()),
      "{125e7431-f678-4e8e-b670-20a9b06c512d}", "Windows.Foundation.IWwwFormUrlDecoderEntry")
}
