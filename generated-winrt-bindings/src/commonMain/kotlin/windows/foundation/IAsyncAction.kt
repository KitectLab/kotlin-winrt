package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public open class IAsyncAction(
  pointer: ComPtr,
) : IAsyncInfo(pointer) {
  public var completed: AsyncActionCompletedHandler
    get() = AsyncActionCompletedHandler(PlatformComInterop.invokeObjectMethod(pointer,
        12).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 11, projectedObjectArgumentPointer(value,
          "Windows.Foundation.AsyncActionCompletedHandler",
          "delegate({a4ed5c81-76c9-40bd-8be6-b1d90fb20ae7})")).getOrThrow()
    }

  public fun getResults() {
    PlatformComInterop.invokeUnitMethod(pointer, 13).getOrThrow()
  }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.IAsyncAction"

    override val projectionTypeKey: String = "Windows.Foundation.IAsyncAction"

    override val iid: Guid = guidOf("5a648006-843a-4da9-865b-9d26e5dfad7b")

    public fun from(inspectable: Inspectable): IAsyncAction = inspectable.projectInterface(this,
        ::IAsyncAction)

    public operator fun invoke(inspectable: Inspectable): IAsyncAction = from(inspectable)
  }
}
