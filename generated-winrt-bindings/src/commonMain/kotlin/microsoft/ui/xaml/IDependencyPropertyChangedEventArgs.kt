package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public interface IDependencyPropertyChangedEventArgs {
  public val newValue: Inspectable

  public val oldValue: Inspectable

  public val property: DependencyProperty

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IDependencyPropertyChangedEventArgs"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.IDependencyPropertyChangedEventArgs"

    override val iid: Guid = guidOf("84ead020-7849-5e98-8030-488a80d164ec")

    public fun from(inspectable: Inspectable): IDependencyPropertyChangedEventArgs =
        inspectable.projectInterface(this, ::IDependencyPropertyChangedEventArgsProjection)

    public operator fun invoke(inspectable: Inspectable): IDependencyPropertyChangedEventArgs =
        from(inspectable)
  }
}

private class IDependencyPropertyChangedEventArgsProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IDependencyPropertyChangedEventArgs {
  override val newValue: Inspectable
    get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())

  override val oldValue: Inspectable
    get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())

  override val property: DependencyProperty
    get() = DependencyProperty(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
}
