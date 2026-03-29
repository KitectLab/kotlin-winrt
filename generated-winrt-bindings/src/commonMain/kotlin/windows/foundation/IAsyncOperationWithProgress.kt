package windows.foundation

import dev.winrt.core.ParameterizedInterfaceId
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

public open class IAsyncOperationWithProgress<TResult, TProgress>(
  pointer: ComPtr,
  public open val resultSignature: String,
  public open val progressSignature: String,
  public open val progressArgumentKind: WinRtDelegateValueKind,
  public open val decodeProgress: (Any?) -> TProgress,
) : IAsyncInfo(pointer) {
  public open var progress: AsyncOperationProgressHandler<TResult, TProgress>
    get() = get_Progress()
    set(value) {
      put_Progress(value)
    }

  public open var completed: AsyncOperationWithProgressCompletedHandler<TResult, TProgress>
    get() = get_Completed()
    set(value) {
      put_Completed(value)
    }

  public open fun put_Progress(handler: AsyncOperationProgressHandler<TResult, TProgress>) {
    PlatformComInterop.invokeObjectSetter(pointer, 11, handler.pointer).getOrThrow()
  }

  public open fun get_Progress(): AsyncOperationProgressHandler<TResult, TProgress> =
      AsyncOperationProgressHandler(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())

  public open fun put_Completed(handler: AsyncOperationWithProgressCompletedHandler<TResult, TProgress>) {
    PlatformComInterop.invokeObjectSetter(pointer, 13, handler.pointer).getOrThrow()
  }

  public open fun get_Completed(): AsyncOperationWithProgressCompletedHandler<TResult, TProgress> =
      AsyncOperationWithProgressCompletedHandler(PlatformComInterop.invokeObjectMethod(pointer, 14).getOrThrow())

  public open fun getResults(): TResult = error("Generic async operation with progress projection requires an override")

  public companion object {
    private const val genericInterfaceGuid: String = "b5d036d7-e297-498f-ba60-0289e76e23dd"

    public fun signatureOf(resultSignature: String, progressSignature: String): String =
        WinRtTypeSignature.parameterizedInterface(genericInterfaceGuid, resultSignature, progressSignature)

    public fun iidOf(resultSignature: String, progressSignature: String): Guid =
        ParameterizedInterfaceId.createFromSignature(signatureOf(resultSignature, progressSignature))
  }
}
