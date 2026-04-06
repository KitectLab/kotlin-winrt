package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import kotlin.collections.Iterable
import kotlin.collections.Iterator

public open class IBindableIterable(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    Iterable<Inspectable> {
  override fun iterator(): Iterator<Inspectable> = first()

  public fun first(): IBindableIterator =
      IBindableIterator.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
      6).getOrThrow()))

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableIterable"

    override val projectionTypeKey: String = "kotlin.collections.Iterable"

    override val iid: Guid = guidOf("036d2c08-df29-41af-8aa2-d774be62ba6f")

    public fun from(inspectable: Inspectable): IBindableIterable =
        inspectable.projectInterface(this, ::IBindableIterable)

    public operator fun invoke(inspectable: Inspectable): IBindableIterable = from(inspectable)
  }
}
