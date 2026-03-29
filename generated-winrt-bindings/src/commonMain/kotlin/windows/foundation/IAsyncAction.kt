package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

public open class IAsyncAction(
  pointer: ComPtr,
) : IAsyncInfo(pointer) {
  public open var completed: AsyncActionCompletedHandler
    get() = get_Completed()
    set(value) {
      put_Completed(value)
    }

  public open fun put_Completed(handler: AsyncActionCompletedHandler) {
    PlatformComInterop.invokeObjectSetter(pointer, 11, handler.pointer).getOrThrow()
  }

  public open fun get_Completed(): AsyncActionCompletedHandler =
      AsyncActionCompletedHandler(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())

  public open fun getResults() {
    PlatformComInterop.invokeUnitMethod(pointer, 13).getOrThrow()
  }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.IAsyncAction"

    override val projectionTypeKey: String = "Windows.Foundation.IAsyncAction"

    override val iid: Guid = guidOf("5a648006-843a-4da9-865b-9d26e5dfad7b")

    public fun from(inspectable: Inspectable): IAsyncAction =
        inspectable.projectInterface(this, ::IAsyncAction)
  }
}
