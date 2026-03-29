package windows.foundation

import dev.winrt.core.ParameterizedInterfaceId
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

public open class AsyncActionWithProgressCompletedHandler<TProgress>(
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
        ?: error("Invoking projected AsyncActionWithProgressCompletedHandler is not supported")
  }

  override fun close() {
    delegateHandle?.close()
    delegateHandle = null
  }

  public companion object {
    private const val genericDelegateGuid: String = "9c029f91-cc84-44fd-ac26-0a6c4e555281"

    public fun signatureOf(progressSignature: String): String =
        WinRtTypeSignature.parameterizedInterface(genericDelegateGuid, progressSignature)

    public fun iidOf(progressSignature: String): Guid =
        ParameterizedInterfaceId.createFromSignature(signatureOf(progressSignature))

    public fun <TProgress> create(
      progressSignature: String,
      invoke: (ComPtr, AsyncStatus) -> Unit,
    ): AsyncActionWithProgressCompletedHandler<TProgress> {
      val delegateHandle = WinRtDelegateBridge.createUnitDelegate(
          iid = iidOf(progressSignature),
          parameterKinds = listOf(
              WinRtDelegateValueKind.OBJECT,
              WinRtDelegateValueKind.INT32,
          ),
      ) { args ->
        invoke(args[0] as ComPtr, AsyncStatus.fromValue(args[1] as Int))
      }
      return AsyncActionWithProgressCompletedHandler(delegateHandle.pointer, delegateHandle, invoke)
    }
  }
}
