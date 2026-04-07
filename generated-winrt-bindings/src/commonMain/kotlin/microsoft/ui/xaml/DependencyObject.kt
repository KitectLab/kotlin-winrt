package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.Int64
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireInt64
import kotlin.String
import microsoft.ui.dispatching.DispatcherQueue
import windows.ui.core.CoreDispatcher

public open class DependencyObject(
  pointer: ComPtr,
) : Inspectable(pointer),
    IDependencyObject {
  private val backing_Dispatcher: RuntimeProperty<CoreDispatcher> =
      RuntimeProperty<CoreDispatcher>(CoreDispatcher(ComPtr.NULL))

  override val dispatcher: CoreDispatcher
    get() {
      if (pointer.isNull) {
        return backing_Dispatcher.get()
      }
      return CoreDispatcher(PlatformComInterop.invokeObjectMethod(pointer, 13).getOrThrow())
    }

  private val backing_DispatcherQueue: RuntimeProperty<DispatcherQueue> =
      RuntimeProperty<DispatcherQueue>(DispatcherQueue(ComPtr.NULL))

  override val dispatcherQueue: DispatcherQueue
    get() {
      if (pointer.isNull) {
        return backing_DispatcherQueue.get()
      }
      return DispatcherQueue(PlatformComInterop.invokeObjectMethod(pointer, 14).getOrThrow())
    }

  public constructor() : this(Companion.factoryCreateInstance().pointer)

  override fun getValue(dp: DependencyProperty): Inspectable {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetValue")
    }
    return Inspectable(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 6,
        projectedObjectArgumentPointer(dp, "Microsoft.UI.Xaml.DependencyProperty",
        "rc(Microsoft.UI.Xaml.DependencyProperty;{960eab49-9672-58a0-995b-3a42e5ea6278})")).getOrThrow())
  }

  override fun setValue(dp: DependencyProperty, value: Inspectable) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 7,
        projectedObjectArgumentPointer(dp, "Microsoft.UI.Xaml.DependencyProperty",
        "rc(Microsoft.UI.Xaml.DependencyProperty;{960eab49-9672-58a0-995b-3a42e5ea6278})"),
        projectedObjectArgumentPointer(value, "Object", "cinterface(IInspectable)")).getOrThrow()
  }

  override fun clearValue(dp: DependencyProperty) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 8, projectedObjectArgumentPointer(dp,
        "Microsoft.UI.Xaml.DependencyProperty",
        "rc(Microsoft.UI.Xaml.DependencyProperty;{960eab49-9672-58a0-995b-3a42e5ea6278})")).getOrThrow()
  }

  override fun readLocalValue(dp: DependencyProperty): Inspectable {
    if (pointer.isNull) {
      error("Null runtime object pointer: ReadLocalValue")
    }
    return Inspectable(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 9,
        projectedObjectArgumentPointer(dp, "Microsoft.UI.Xaml.DependencyProperty",
        "rc(Microsoft.UI.Xaml.DependencyProperty;{960eab49-9672-58a0-995b-3a42e5ea6278})")).getOrThrow())
  }

  override fun getAnimationBaseValue(dp: DependencyProperty): Inspectable {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetAnimationBaseValue")
    }
    return Inspectable(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 10,
        projectedObjectArgumentPointer(dp, "Microsoft.UI.Xaml.DependencyProperty",
        "rc(Microsoft.UI.Xaml.DependencyProperty;{960eab49-9672-58a0-995b-3a42e5ea6278})")).getOrThrow())
  }

  override fun registerPropertyChangedCallback(dp: DependencyProperty,
      callback: DependencyPropertyChangedCallback): Int64 {
    if (pointer.isNull) {
      return Int64(0L)
    }
    return Int64(PlatformComInterop.invokeMethodWithTwoObjectArgs(pointer, 11,
        ComMethodResultKind.INT64, projectedObjectArgumentPointer(dp,
        "Microsoft.UI.Xaml.DependencyProperty",
        "rc(Microsoft.UI.Xaml.DependencyProperty;{960eab49-9672-58a0-995b-3a42e5ea6278})"),
        projectedObjectArgumentPointer(callback,
        "Microsoft.UI.Xaml.DependencyPropertyChangedCallback",
        "delegate({f055bb21-219b-5b0c-805d-bcaedae15458})")).getOrThrow().requireInt64())
  }

  override fun unregisterPropertyChangedCallback(dp: DependencyProperty, token: Int64) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithObjectAndInt64Args(pointer, 12,
        projectedObjectArgumentPointer(dp, "Microsoft.UI.Xaml.DependencyProperty",
        "rc(Microsoft.UI.Xaml.DependencyProperty;{960eab49-9672-58a0-995b-3a42e5ea6278})"),
        token.value).getOrThrow()
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.DependencyObject"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml", "DependencyObject")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IDependencyObject"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private fun factoryCreateInstance(): DependencyObject {
      return WinRtRuntime.compose(this, guidOf("936b614c-475f-5d7d-b3f7-bf1fbea28126"),
          guidOf("e7beaee7-160e-50f7-8789-d63463f979fa"), ::DependencyObject, 6, ComPtr.NULL)
    }
  }
}
