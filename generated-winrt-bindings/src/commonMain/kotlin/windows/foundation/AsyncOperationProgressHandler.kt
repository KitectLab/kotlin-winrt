package windows.foundation

import dev.winrt.core.ParameterizedInterfaceId
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

public open class AsyncOperationProgressHandler<TResult, TProgress>(
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
        ?: error("Invoking projected AsyncOperationProgressHandler is not supported")
  }

  override fun close() {
    delegateHandle?.close()
    delegateHandle = null
  }

  public companion object {
    private const val genericDelegateGuid: String = "55690902-0aab-421a-8778-f8ce5026d758"

    public fun signatureOf(resultSignature: String, progressSignature: String): String =
        WinRtTypeSignature.parameterizedInterface(genericDelegateGuid, resultSignature, progressSignature)

    public fun iidOf(resultSignature: String, progressSignature: String): Guid =
        ParameterizedInterfaceId.createFromSignature(signatureOf(resultSignature, progressSignature))

    public fun <TResult, TProgress> create(
      resultSignature: String,
      progressSignature: String,
      progressArgumentKind: WinRtDelegateValueKind,
      decodeProgress: (Any?) -> TProgress,
      invoke: (ComPtr, TProgress) -> Unit,
    ): AsyncOperationProgressHandler<TResult, TProgress> {
      val delegateHandle = WinRtDelegateBridge.createUnitDelegate(
          iid = iidOf(resultSignature, progressSignature),
          parameterKinds = listOf(WinRtDelegateValueKind.OBJECT, progressArgumentKind),
      ) { args ->
        invoke(args[0] as ComPtr, decodeProgress(args[1]))
      }
      return AsyncOperationProgressHandler(delegateHandle.pointer, delegateHandle, invoke)
    }
  }
}
