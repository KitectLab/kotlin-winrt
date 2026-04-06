package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireBoolean
import dev.winrt.projection.WinRtListProjection
import kotlin.String
import kotlin.collections.List

public open class IBindableVectorView(
  pointer: ComPtr,
) : IBindableIterable(pointer),
    List<Inspectable> by WinRtListProjection<Inspectable>(sizeProvider = {
    UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow()).value.toInt() }, getter =
    { index -> Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7,
    index.toUInt()).getOrThrow()) }) {
  public val winRtSize: UInt32
    get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())

  public val size: UInt32
    get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())

  public fun getAt(index: UInt32): Inspectable =
      Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7,
      index.value).getOrThrow())

  public fun indexOf(value: Inspectable, index: UInt32): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeMethodWithObjectAndUInt32Args(pointer, 9,
      ComMethodResultKind.BOOLEAN, projectedObjectArgumentPointer(value, "Object",
      "cinterface(IInspectable)"), index.value).getOrThrow().requireBoolean())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVectorView"

    override val projectionTypeKey: String = "kotlin.collections.List"

    override val iid: Guid = guidOf("346dd6e7-976e-4bc3-815d-ece243bc0f33")

    public fun from(inspectable: Inspectable): IBindableVectorView =
        inspectable.projectInterface(this, ::IBindableVectorView)

    public operator fun invoke(inspectable: Inspectable): IBindableVectorView = from(inspectable)
  }
}
