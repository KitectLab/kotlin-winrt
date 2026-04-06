package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import kotlin.collections.MutableMap
import windows.foundation.Uri
import windows.foundation.collections.IMap
import windows.foundation.collections.IVector

public interface IResourceDictionary {
  public val mergedDictionaries: IVector<ResourceDictionary>

  public var source: Uri

  public val themeDictionaries: MutableMap<Inspectable, Inspectable>

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IResourceDictionary"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.IResourceDictionary"

    override val iid: Guid = guidOf("1b690975-a710-5783-a6e1-15836f6186c2")

    public fun from(inspectable: Inspectable): IResourceDictionary =
        inspectable.projectInterface(this, ::IResourceDictionaryProjection)

    public operator fun invoke(inspectable: Inspectable): IResourceDictionary = from(inspectable)
  }
}

private class IResourceDictionaryProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IResourceDictionary {
  override val mergedDictionaries: IVector<ResourceDictionary>
    get() = IVector.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        8).getOrThrow()),
        "rc(Microsoft.UI.Xaml.ResourceDictionary;{1b690975-a710-5783-a6e1-15836f6186c2})",
        "Microsoft.UI.Xaml.ResourceDictionary")

  override var source: Uri
    get() = Uri(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 7, projectedObjectArgumentPointer(value,
          "Windows.Foundation.Uri",
          "rc(Windows.Foundation.Uri;{9e365e57-48b2-4160-956f-c7385120bbfc})")).getOrThrow()
    }

  override val themeDictionaries: MutableMap<Inspectable, Inspectable>
    get() = IMap.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 9).getOrThrow()),
        "cinterface(IInspectable)", "cinterface(IInspectable)", "Object", "Object")
}
