package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import kotlin.Unit
import windows.foundation.Uri

internal open class IApplicationStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val current: Application
    get() = Application(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

  public fun start(callback: ApplicationInitializationCallback) {
    PlatformComInterop.invokeObjectSetter(pointer, 7, projectedObjectArgumentPointer(callback,
        "Microsoft.UI.Xaml.ApplicationInitializationCallback",
        "delegate({d8eef1c9-1234-56f1-9963-45dd9c80a661})")).getOrThrow()
  }

  public fun loadComponent(component: Inspectable, resourceLocator: Uri) {
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 8,
        projectedObjectArgumentPointer(component, "Object", "cinterface(IInspectable)"),
        projectedObjectArgumentPointer(resourceLocator, "Windows.Foundation.Uri",
        "rc(Windows.Foundation.Uri;{9e365e57-48b2-4160-956f-c7385120bbfc})")).getOrThrow()
  }

  public fun start(callback: (ApplicationInitializationCallbackParams) -> Unit):
      WinRtDelegateHandle {
    val delegateHandle =
        WinRtDelegateBridge.createUnitDelegate(ApplicationInitializationCallback.iid,
        listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args ->
        callback(microsoft.ui.xaml.ApplicationInitializationCallbackParams(args[0] as ComPtr)) }
    try {
      start(ApplicationInitializationCallback(delegateHandle.pointer))
    } catch (t: Throwable) {
      delegateHandle.close()
      throw t
    }
    return delegateHandle
  }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IApplicationStatics"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.IApplicationStatics"

    override val iid: Guid = guidOf("4e0d09f5-4358-512c-a987-503b52848e95")

    public fun from(inspectable: Inspectable): IApplicationStatics =
        inspectable.projectInterface(this, ::IApplicationStatics)

    public operator fun invoke(inspectable: Inspectable): IApplicationStatics = from(inspectable)
  }
}
