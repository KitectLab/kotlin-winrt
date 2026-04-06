package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireBoolean
import kotlin.String
import kotlin.collections.Iterator
import microsoft.ui.xaml.UIElement
import windows.foundation.collections.IVectorView

public open class UIElementCollection(
  pointer: ComPtr,
) : Inspectable(pointer) {
  private val backing_Size: RuntimeProperty<UInt32> = RuntimeProperty<UInt32>(UInt32(0u))

  public val size: UInt32
    get() {
      if (pointer.isNull) {
        return backing_Size.get()
      }
      return UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
    }

  public fun getAt(index: UInt32): UIElement {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetAt")
    }
    return UIElement(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7,
        index.value).getOrThrow())
  }

  public fun getView(): IVectorView<UIElement> {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetView")
    }
    return IVectorView<UIElement>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        9).getOrThrow()))
  }

  public fun indexOf(value: UIElement, index: UInt32): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeMethodWithObjectAndUInt32Args(pointer, 10,
        ComMethodResultKind.BOOLEAN, projectedObjectArgumentPointer(value,
        "Microsoft.UI.Xaml.UIElement",
        "rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b})"),
        index.value).getOrThrow().requireBoolean())
  }

  public fun setAt(index: UInt32, value: UIElement) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithUInt32AndObjectArgs(pointer, 11, index.value,
        projectedObjectArgumentPointer(value, "Microsoft.UI.Xaml.UIElement",
        "rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b})")).getOrThrow()
  }

  public fun insertAt(index: UInt32, value: UIElement) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithUInt32AndObjectArgs(pointer, 12, index.value,
        projectedObjectArgumentPointer(value, "Microsoft.UI.Xaml.UIElement",
        "rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b})")).getOrThrow()
  }

  public fun removeAt(index: UInt32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 13, index.value).getOrThrow()
  }

  public fun append(value: UIElement) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 14, projectedObjectArgumentPointer(value,
        "Microsoft.UI.Xaml.UIElement",
        "rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b})")).getOrThrow()
  }

  public fun removeAtEnd() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 15).getOrThrow()
  }

  public fun clear() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 16).getOrThrow()
  }

  public fun first(): Iterator<UIElement> {
    if (pointer.isNull) {
      error("Null runtime object pointer: First")
    }
    return Iterator<UIElement>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        6).getOrThrow()))
  }

  public fun move(oldIndex: UInt32, newIndex: UInt32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoUInt32Args(pointer, 6, oldIndex.value,
        newIndex.value).getOrThrow()
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.UIElementCollection"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls",
        "UIElementCollection")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IUIElementCollection"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory
  }
}
