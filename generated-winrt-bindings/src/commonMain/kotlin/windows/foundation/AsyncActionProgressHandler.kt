package windows.foundation

import dev.winrt.core.ParameterizedInterfaceId
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

public open class AsyncActionProgressHandler<TProgress>(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer), AutoCloseable {
  private var delegateHandle: WinRtDelegateHandle? = null
  private var localInvoke: ((ComPtr, TProgress) -> Unit)? = null

  internal constructor(
    pointer: ComPtr,
    delegateHandle: WinRtDelegateHandle,
    localInvoke: (ComPtr, TProgress) -> Unit,
  ) : this(pointer) {
    this.delegateHandle = delegateHandle
    this.localInvoke = localInvoke
  }

  public open fun invoke(asyncInfo: ComPtr, progressInfo: TProgress) {
    localInvoke?.invoke(asyncInfo, progressInfo)
        ?: error("Invoking projected AsyncActionProgressHandler is not supported")
  }

  override fun close() {
    delegateHandle?.close()
    delegateHandle = null
    localInvoke = null
  }

  public companion object {
    private const val genericDelegateGuid: String = "6d844858-0cff-4590-ae89-95a5a5c8b4b8"

    public fun signatureOf(progressSignature: String): String =
        WinRtTypeSignature.parameterizedInterface(genericDelegateGuid, progressSignature)

    public fun iidOf(progressSignature: String): Guid =
        ParameterizedInterfaceId.createFromSignature(signatureOf(progressSignature))

    public fun <TProgress> create(
      progressSignature: String,
      progressArgumentKind: WinRtDelegateValueKind,
      decodeProgress: (Any?) -> TProgress,
      invoke: (ComPtr, TProgress) -> Unit,
    ): AsyncActionProgressHandler<TProgress> {
      val delegateHandle = WinRtDelegateBridge.createUnitDelegate(
          iid = iidOf(progressSignature),
          parameterKinds = listOf(WinRtDelegateValueKind.OBJECT, progressArgumentKind),
      ) { args ->
        invoke(args[0] as ComPtr, decodeProgress(args[1]))
      }
      return AsyncActionProgressHandler(delegateHandle.pointer, delegateHandle, invoke)
    }
  }
}
