package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import dev.winrt.projection.WinRtMutableListProjection
import kotlin.String
import kotlin.collections.MutableList

public open class IBindableVector(
  pointer: ComPtr,
) : IBindableIterable(pointer),
    MutableList<Inspectable> by WinRtMutableListProjection<Inspectable>(sizeProvider = {
    UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow()).value.toInt() }, getter =
    { index -> Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7,
    index.toUInt()).getOrThrow()) }, append = { value ->
    PlatformComInterop.invokeObjectSetter(pointer, 14, (value as
    Inspectable).pointer).getOrThrow() }, clearer = { PlatformComInterop.invokeUnitMethod(pointer,
    16).getOrThrow() }) {
  public val winRtSize: UInt32
    get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())

  public val size: UInt32
    get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())

  public fun getAt(index: UInt32): Inspectable =
      Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7,
      index.value).getOrThrow())

  public fun getView(): IBindableVectorView =
      IBindableVectorView.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
      9).getOrThrow()))

  public fun winRtIndexOf(value: Inspectable): UInt32? {
    val (found, index) = PlatformComInterop.invokeIndexOfMethod(pointer, 10,
        projectedObjectArgumentPointer(value, "Object", "cinterface(IInspectable)")).getOrThrow()
    return if (found) UInt32(index) else null
  }

  public fun setAt(index: UInt32, value: Inspectable) {
    PlatformComInterop.invokeUnitMethodWithUInt32AndObjectArgs(pointer, 11, index.value,
        projectedObjectArgumentPointer(value, "Object", "cinterface(IInspectable)")).getOrThrow()
  }

  public fun insertAt(index: UInt32, value: Inspectable) {
    PlatformComInterop.invokeUnitMethodWithUInt32AndObjectArgs(pointer, 12, index.value,
        projectedObjectArgumentPointer(value, "Object", "cinterface(IInspectable)")).getOrThrow()
  }

  public fun removeAt(index: UInt32) {
    PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 13, index.value).getOrThrow()
  }

  public fun append(value: Inspectable) {
    PlatformComInterop.invokeObjectSetter(pointer, 14, projectedObjectArgumentPointer(value,
        "Object", "cinterface(IInspectable)")).getOrThrow()
  }

  public fun removeAtEnd() {
    PlatformComInterop.invokeUnitMethod(pointer, 15).getOrThrow()
  }

  public fun clear() {
    PlatformComInterop.invokeUnitMethod(pointer, 16).getOrThrow()
  }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVector"

    override val projectionTypeKey: String = "kotlin.collections.MutableList"

    override val iid: Guid = guidOf("393de7de-6fd0-4c0d-bb71-47244a113e93")

    public fun from(inspectable: Inspectable): IBindableVector = inspectable.projectInterface(this,
        ::IBindableVector)

    public operator fun invoke(inspectable: Inspectable): IBindableVector = from(inspectable)
  }
}
