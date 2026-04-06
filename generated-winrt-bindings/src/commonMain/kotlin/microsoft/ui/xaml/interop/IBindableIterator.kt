package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import java.util.NoSuchElementException
import kotlin.Boolean
import kotlin.String
import kotlin.collections.Iterator

public open class IBindableIterator(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    Iterator<Inspectable> {
  public val winRtCurrent: Inspectable
    get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

  public val winRtHasCurrent: WinRtBoolean
    get() = dev.winrt.core.WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer,
        7).getOrThrow())

  public val current: Inspectable
    get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

  public val hasCurrent: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow())

  override fun hasNext(): Boolean = winRtHasCurrent.value

  override fun next(): Inspectable {
    if (!hasNext()) {
      throw NoSuchElementException()
    }
    val current = winRtCurrent
    moveNext()
    return current
  }

  public fun moveNext(): WinRtBoolean = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer,
      8).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableIterator"

    override val projectionTypeKey: String = "kotlin.collections.Iterator"

    override val iid: Guid = guidOf("6a1d6c07-076d-49f2-8314-f52c9c9a8331")

    public fun from(inspectable: Inspectable): IBindableIterator =
        inspectable.projectInterface(this, ::IBindableIterator)

    public operator fun invoke(inspectable: Inspectable): IBindableIterator = from(inspectable)
  }
}
