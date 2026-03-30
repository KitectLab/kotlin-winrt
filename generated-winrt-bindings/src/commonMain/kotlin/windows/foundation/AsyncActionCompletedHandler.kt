package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

public open class AsyncActionCompletedHandler(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer), AutoCloseable {
  private var delegateHandle: WinRtDelegateHandle? = null
  private var localInvoke: ((IAsyncAction, AsyncStatus) -> Unit)? = null

  internal constructor(
    pointer: ComPtr,
    delegateHandle: WinRtDelegateHandle,
    localInvoke: (IAsyncAction, AsyncStatus) -> Unit,
  ) : this(pointer) {
    this.delegateHandle = delegateHandle
    this.localInvoke = localInvoke
  }

  public open fun invoke(asyncInfo: IAsyncAction, asyncStatus: AsyncStatus) {
    localInvoke?.invoke(asyncInfo, asyncStatus)
        ?: error("Invoking projected AsyncActionCompletedHandler is not supported")
  }

  override fun close() {
    delegateHandle?.close()
    delegateHandle = null
    localInvoke = null
  }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.AsyncActionCompletedHandler"

    override val projectionTypeKey: String = "Windows.Foundation.AsyncActionCompletedHandler"

    override val iid: Guid = guidOf("a4ed5c81-76c9-40bd-8be6-b1d90fb20ae7")

    public fun create(invoke: (IAsyncAction, AsyncStatus) -> Unit): AsyncActionCompletedHandler {
      val delegateHandle = WinRtDelegateBridge.createUnitDelegate(
          iid = iid,
          parameterKinds = listOf(
              WinRtDelegateValueKind.OBJECT,
              WinRtDelegateValueKind.INT32,
          ),
      ) { args ->
        invoke(
            IAsyncAction(args[0] as ComPtr),
            AsyncStatus.fromValue(args[1] as Int),
        )
      }
      return AsyncActionCompletedHandler(delegateHandle.pointer, delegateHandle, invoke)
    }

    public fun from(inspectable: Inspectable): AsyncActionCompletedHandler =
        inspectable.projectInterface(this, ::AsyncActionCompletedHandler)
  }
}
