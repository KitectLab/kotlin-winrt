package windows.foundation

import dev.winrt.core.UInt32
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import kotlinx.coroutines.CancellationException

public object AsyncInfo {
  public fun <TResult> fromResult(resultType: AsyncResultType<TResult>, value: TResult): IAsyncOperation<TResult> {
    return CompletedAsyncOperation(resultType.signature, value)
  }

  public fun <TResult> fromResult(resultSignature: String, value: TResult): IAsyncOperation<TResult> {
    return CompletedAsyncOperation(resultSignature, value)
  }

  public fun <TResult, TProgress> fromResultWithProgress(
    resultType: AsyncResultType<TResult>,
    progressType: AsyncProgressType<TProgress>,
    value: TResult,
  ): IAsyncOperationWithProgress<TResult, TProgress> {
    return CompletedAsyncOperationWithProgress(
        resultType.signature,
        progressType.signature,
        progressType.argumentKind,
        value,
    )
  }

  public fun <TResult, TProgress> fromResultWithProgress(
    resultSignature: String,
    progressSignature: String,
    progressArgumentKind: WinRtDelegateValueKind,
    value: TResult,
  ): IAsyncOperationWithProgress<TResult, TProgress> {
    return CompletedAsyncOperationWithProgress(resultSignature, progressSignature, progressArgumentKind, value)
  }

  public fun <TResult> canceledOperation(resultType: AsyncResultType<TResult>): IAsyncOperation<TResult> {
    return CanceledAsyncOperation(resultType.signature)
  }

  public fun <TResult> canceledOperation(resultSignature: String): IAsyncOperation<TResult> {
    return CanceledAsyncOperation(resultSignature)
  }

  public fun <TResult, TProgress> canceledOperationWithProgress(
    resultType: AsyncResultType<TResult>,
    progressType: AsyncProgressType<TProgress>,
  ): IAsyncOperationWithProgress<TResult, TProgress> {
    return CanceledAsyncOperationWithProgress(resultType.signature, progressType.signature, progressType.argumentKind)
  }

  public fun <TResult, TProgress> canceledOperationWithProgress(
    resultSignature: String,
    progressSignature: String,
    progressArgumentKind: WinRtDelegateValueKind,
  ): IAsyncOperationWithProgress<TResult, TProgress> {
    return CanceledAsyncOperationWithProgress(resultSignature, progressSignature, progressArgumentKind)
  }

  public fun <TResult> fromException(resultType: AsyncResultType<TResult>, error: Throwable): IAsyncOperation<TResult> {
    return FailedAsyncOperation(resultType.signature, error)
  }

  public fun <TResult> fromException(resultSignature: String, error: Throwable): IAsyncOperation<TResult> {
    return FailedAsyncOperation(resultSignature, error)
  }

  public fun <TResult, TProgress> fromExceptionWithProgress(
    resultType: AsyncResultType<TResult>,
    progressType: AsyncProgressType<TProgress>,
    error: Throwable,
  ): IAsyncOperationWithProgress<TResult, TProgress> {
    return FailedAsyncOperationWithProgress(resultType.signature, progressType.signature, progressType.argumentKind, error)
  }

  public fun <TResult, TProgress> fromExceptionWithProgress(
    resultSignature: String,
    progressSignature: String,
    progressArgumentKind: WinRtDelegateValueKind,
    error: Throwable,
  ): IAsyncOperationWithProgress<TResult, TProgress> {
    return FailedAsyncOperationWithProgress(resultSignature, progressSignature, progressArgumentKind, error)
  }
}

private class CompletedAsyncOperation<TResult>(
  resultSignature: String,
  private val value: TResult,
) : IAsyncOperation<TResult>(
  ComPtr.NULL,
  resultSignature,
) {
  override val id: UInt32
    get() = UInt32(0u)

  override val status: AsyncStatus
    get() = AsyncStatus.Completed

  override val errorCode: HResult
    get() = HResult(0)

  override fun put_Completed(handler: AsyncOperationCompletedHandler<TResult>) {
    handler.invoke(pointer, AsyncStatus.Completed)
  }

  override fun get_Completed(): AsyncOperationCompletedHandler<TResult> =
      AsyncOperationCompletedHandler(ComPtr.NULL)

  override fun getResults(): TResult = value

  override fun cancel() = Unit
}

private class CompletedAsyncOperationWithProgress<TResult, TProgress>(
  resultSignature: String,
  progressSignature: String,
  progressArgumentKind: WinRtDelegateValueKind,
  private val value: TResult,
) : IAsyncOperationWithProgress<TResult, TProgress>(
  ComPtr.NULL,
  resultSignature,
  progressSignature,
  progressArgumentKind,
  { @Suppress("UNCHECKED_CAST") it as TProgress },
) {
  override val id: UInt32
    get() = UInt32(0u)

  override val status: AsyncStatus
    get() = AsyncStatus.Completed

  override val errorCode: HResult
    get() = HResult(0)

  override fun put_Completed(handler: AsyncOperationWithProgressCompletedHandler<TResult, TProgress>) {
    handler.invoke(pointer, AsyncStatus.Completed)
  }

  override fun get_Completed(): AsyncOperationWithProgressCompletedHandler<TResult, TProgress> =
      AsyncOperationWithProgressCompletedHandler(ComPtr.NULL)

  override fun getResults(): TResult = value

  override fun put_Progress(handler: AsyncOperationProgressHandler<TResult, TProgress>) = Unit

  override fun get_Progress(): AsyncOperationProgressHandler<TResult, TProgress> =
      AsyncOperationProgressHandler(ComPtr.NULL)

  override fun cancel() = Unit
}

private class CanceledAsyncOperation<TResult>(
  resultSignature: String,
) : IAsyncOperation<TResult>(ComPtr.NULL, resultSignature) {
  override val id: UInt32
    get() = UInt32(0u)

  override val status: AsyncStatus
    get() = AsyncStatus.Canceled

  override val errorCode: HResult
    get() = HResult(0x80004004.toInt())

  override fun put_Completed(handler: AsyncOperationCompletedHandler<TResult>) {
    handler.invoke(pointer, AsyncStatus.Canceled)
  }

  override fun get_Completed(): AsyncOperationCompletedHandler<TResult> =
      AsyncOperationCompletedHandler(ComPtr.NULL)

  override fun getResults(): TResult {
    throw CancellationException("WinRT async operation was canceled")
  }

  override fun cancel() = Unit
}

private class CanceledAsyncOperationWithProgress<TResult, TProgress>(
  resultSignature: String,
  progressSignature: String,
  progressArgumentKind: WinRtDelegateValueKind,
) : IAsyncOperationWithProgress<TResult, TProgress>(
  ComPtr.NULL,
  resultSignature,
  progressSignature,
  progressArgumentKind,
  { @Suppress("UNCHECKED_CAST") it as TProgress },
) {
  override val id: UInt32
    get() = UInt32(0u)

  override val status: AsyncStatus
    get() = AsyncStatus.Canceled

  override val errorCode: HResult
    get() = HResult(0x80004004.toInt())

  override fun put_Completed(handler: AsyncOperationWithProgressCompletedHandler<TResult, TProgress>) {
    handler.invoke(pointer, AsyncStatus.Canceled)
  }

  override fun get_Completed(): AsyncOperationWithProgressCompletedHandler<TResult, TProgress> =
      AsyncOperationWithProgressCompletedHandler(ComPtr.NULL)

  override fun getResults(): TResult {
    throw CancellationException("WinRT async operation with progress was canceled")
  }

  override fun put_Progress(handler: AsyncOperationProgressHandler<TResult, TProgress>) = Unit

  override fun get_Progress(): AsyncOperationProgressHandler<TResult, TProgress> =
      AsyncOperationProgressHandler(ComPtr.NULL)

  override fun cancel() = Unit
}

private class FailedAsyncOperation<TResult>(
  resultSignature: String,
  private val error: Throwable,
) : IAsyncOperation<TResult>(ComPtr.NULL, resultSignature) {
  override val id: UInt32
    get() = UInt32(0u)

  override val status: AsyncStatus
    get() = AsyncStatus.Error

  override val errorCode: HResult
    get() = HResult(0x80004005.toInt())

  override fun put_Completed(handler: AsyncOperationCompletedHandler<TResult>) {
    handler.invoke(pointer, AsyncStatus.Error)
  }

  override fun get_Completed(): AsyncOperationCompletedHandler<TResult> =
      AsyncOperationCompletedHandler(ComPtr.NULL)

  override fun getResults(): TResult {
    throw error
  }

  override fun cancel() = Unit
}

private class FailedAsyncOperationWithProgress<TResult, TProgress>(
  resultSignature: String,
  progressSignature: String,
  progressArgumentKind: WinRtDelegateValueKind,
  private val error: Throwable,
) : IAsyncOperationWithProgress<TResult, TProgress>(
  ComPtr.NULL,
  resultSignature,
  progressSignature,
  progressArgumentKind,
  { @Suppress("UNCHECKED_CAST") it as TProgress },
) {
  override val id: UInt32
    get() = UInt32(0u)

  override val status: AsyncStatus
    get() = AsyncStatus.Error

  override val errorCode: HResult
    get() = HResult(0x80004005.toInt())

  override fun put_Completed(handler: AsyncOperationWithProgressCompletedHandler<TResult, TProgress>) {
    handler.invoke(pointer, AsyncStatus.Error)
  }

  override fun get_Completed(): AsyncOperationWithProgressCompletedHandler<TResult, TProgress> =
      AsyncOperationWithProgressCompletedHandler(ComPtr.NULL)

  override fun getResults(): TResult {
    throw error
  }

  override fun put_Progress(handler: AsyncOperationProgressHandler<TResult, TProgress>) = Unit

  override fun get_Progress(): AsyncOperationProgressHandler<TResult, TProgress> =
      AsyncOperationProgressHandler(ComPtr.NULL)

  override fun cancel() = Unit
}
