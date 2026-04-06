package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.String
import kotlin.Unit

public typealias AsyncActionCompletedHandlerHandler = (IAsyncAction, AsyncStatus) -> Unit

public open class AsyncActionCompletedHandler(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.AsyncActionCompletedHandler"

    override val iid: Guid = guidOf("a4ed5c81-76c9-40bd-8be6-b1d90fb20ae7")

    public fun from(inspectable: Inspectable): AsyncActionCompletedHandler =
        inspectable.projectInterface(this, ::AsyncActionCompletedHandler)
  }
}
