package windows.foundation

import dev.winrt.core.ParameterizedInterfaceId
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

public open class AsyncOperationCompletedHandler<TResult>(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer), AutoCloseable {
  private var delegateHandle: WinRtDelegateHandle? = null
  private var localInvoke: ((ComPtr, AsyncStatus) -> Unit)? = null

  internal constructor(
    pointer: ComPtr,
    delegateHandle: WinRtDelegateHandle,
    localInvoke: (ComPtr, AsyncStatus) -> Unit,
  ) : this(pointer) {
    this.delegateHandle = delegateHandle
    this.localInvoke = localInvoke
  }

  public open fun invoke(asyncInfo: ComPtr, asyncStatus: AsyncStatus) {
    localInvoke?.invoke(asyncInfo, asyncStatus)
        ?: error("Invoking projected AsyncOperationCompletedHandler is not supported")
  }

  override fun close() {
    delegateHandle?.close()
    delegateHandle = null
  }

  public companion object {
    private const val genericDelegateGuid: String = "fcdcf02c-e5d8-4478-915a-4d90b74b83a5"

    public fun signatureOf(resultSignature: String): String =
        WinRtTypeSignature.parameterizedInterface(genericDelegateGuid, resultSignature)

    public fun iidOf(resultSignature: String): Guid =
        ParameterizedInterfaceId.createFromSignature(signatureOf(resultSignature))

    public fun <TResult> create(
      resultSignature: String,
      invoke: (ComPtr, AsyncStatus) -> Unit,
    ): AsyncOperationCompletedHandler<TResult> {
      val delegateHandle = WinRtDelegateBridge.createUnitDelegate(
          iid = iidOf(resultSignature),
          parameterKinds = listOf(
              WinRtDelegateValueKind.OBJECT,
              WinRtDelegateValueKind.INT32,
          ),
      ) { args ->
        invoke(
            args[0] as ComPtr,
            AsyncStatus.fromValue(args[1] as Int),
        )
      }
      return AsyncOperationCompletedHandler(delegateHandle.pointer, delegateHandle, invoke)
    }
  }
}
