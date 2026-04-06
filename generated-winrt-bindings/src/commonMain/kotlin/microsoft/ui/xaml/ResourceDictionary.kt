package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireBoolean
import kotlin.String
import kotlin.collections.Iterator
import kotlin.collections.Map
import kotlin.collections.MutableMap
import windows.foundation.Uri
import windows.foundation.collections.IVector

public open class ResourceDictionary(
  pointer: ComPtr,
) : DependencyObject(pointer) {
  private val backing_Size: RuntimeProperty<UInt32> = RuntimeProperty<UInt32>(UInt32(0u))

  public val size: UInt32
    get() {
      if (pointer.isNull) {
        return backing_Size.get()
      }
      return UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
    }

  private val backing_Source: RuntimeProperty<Uri> = RuntimeProperty<Uri>(Uri(ComPtr.NULL))

  public var source: Uri
    get() {
      if (pointer.isNull) {
        return backing_Source.get()
      }
      return Uri(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Source.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 7, (value as
          Inspectable).pointer).getOrThrow()
    }

  public constructor() : this(Companion.factoryCreateInstance().pointer)

  public fun lookup(key: Inspectable): Inspectable {
    if (pointer.isNull) {
      error("Null runtime object pointer: Lookup")
    }
    return Inspectable(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 7,
        projectedObjectArgumentPointer(key, "Object", "cinterface(IInspectable)")).getOrThrow())
  }

  public fun hasKey(key: Inspectable): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithObjectArg(pointer, 9,
        projectedObjectArgumentPointer(key, "Object", "cinterface(IInspectable)")).getOrThrow())
  }

  public fun getView(): Map<Inspectable, Inspectable> {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetView")
    }
    return Map<Inspectable, Inspectable>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        10).getOrThrow()))
  }

  public fun insert(key: Inspectable, value: Inspectable): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeMethodWithTwoObjectArgs(pointer, 11,
        ComMethodResultKind.BOOLEAN, projectedObjectArgumentPointer(key, "Object",
        "cinterface(IInspectable)"), projectedObjectArgumentPointer(value, "Object",
        "cinterface(IInspectable)")).getOrThrow().requireBoolean())
  }

  public fun remove(key: Inspectable) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 12, projectedObjectArgumentPointer(key, "Object",
        "cinterface(IInspectable)")).getOrThrow()
  }

  public fun clear() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 13).getOrThrow()
  }

  public fun first(): Iterator<Map.Entry<Inspectable, Inspectable>> {
    if (pointer.isNull) {
      error("Null runtime object pointer: First")
    }
    return Iterator<Map.Entry<Inspectable, Inspectable>>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        6).getOrThrow()))
  }

  public fun get_MergedDictionaries(): IVector<ResourceDictionary> {
    if (pointer.isNull) {
      error("Null runtime object pointer: get_MergedDictionaries")
    }
    return IVector<ResourceDictionary>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        8).getOrThrow()))
  }

  public fun get_ThemeDictionaries(): MutableMap<Inspectable, Inspectable> {
    if (pointer.isNull) {
      error("Null runtime object pointer: get_ThemeDictionaries")
    }
    return MutableMap<Inspectable, Inspectable>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        9).getOrThrow()))
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.ResourceDictionary"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml", "ResourceDictionary")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IResourceDictionary"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private fun factoryCreateInstance(): ResourceDictionary {
      return WinRtRuntime.compose(this, guidOf("ea22a48f-ab71-56f6-a392-d82310c8aa7b"),
          guidOf("1b690975-a710-5783-a6e1-15836f6186c2"), ::ResourceDictionary, 6, ComPtr.NULL)
    }
  }
}
