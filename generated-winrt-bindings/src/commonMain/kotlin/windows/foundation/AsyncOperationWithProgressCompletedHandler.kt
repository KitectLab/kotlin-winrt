package windows.foundation

import dev.winrt.core.ParameterizedInterfaceId
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

public open class AsyncOperationWithProgressCompletedHandler<TResult, TProgress>(
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
        ?: error("Invoking projected AsyncOperationWithProgressCompletedHandler is not supported")
  }

  override fun close() {
    delegateHandle?.close()
    delegateHandle = null
    localInvoke = null
  }

  public companion object {
    private const val genericDelegateGuid: String = "e85df41d-6aa7-46e3-a8e2-f009d840c627"

    public fun signatureOf(resultSignature: String, progressSignature: String): String =
        WinRtTypeSignature.parameterizedInterface(genericDelegateGuid, resultSignature, progressSignature)

    public fun iidOf(resultSignature: String, progressSignature: String): Guid =
        ParameterizedInterfaceId.createFromSignature(signatureOf(resultSignature, progressSignature))

    public fun <TResult, TProgress> create(
      resultSignature: String,
      progressSignature: String,
      invoke: (ComPtr, AsyncStatus) -> Unit,
    ): AsyncOperationWithProgressCompletedHandler<TResult, TProgress> {
      val delegateHandle = WinRtDelegateBridge.createUnitDelegate(
          iid = iidOf(resultSignature, progressSignature),
          parameterKinds = listOf(
              WinRtDelegateValueKind.OBJECT,
              WinRtDelegateValueKind.INT32,
          ),
      ) { args ->
        invoke(args[0] as ComPtr, AsyncStatus.fromValue(args[1] as Int))
      }
      return AsyncOperationWithProgressCompletedHandler(delegateHandle.pointer, delegateHandle, invoke)
    }
  }
}
