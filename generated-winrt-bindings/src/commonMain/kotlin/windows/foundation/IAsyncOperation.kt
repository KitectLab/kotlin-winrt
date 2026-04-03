package windows.foundation

import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop

public open class IAsyncOperation<TResult>(
  pointer: ComPtr,
  public open val resultType: AsyncResultType<TResult>,
) : IAsyncInfo(pointer) {
  public open val resultSignature: String
    get() = resultType.signature

  public open var completed: AsyncOperationCompletedHandler<TResult>
    get() = get_Completed()
    set(value) {
      put_Completed(value)
    }

  public open fun put_Completed(handler: AsyncOperationCompletedHandler<TResult>) {
    PlatformComInterop.invokeObjectSetter(pointer, 11, handler.pointer).getOrThrow()
  }

  public open fun get_Completed(): AsyncOperationCompletedHandler<TResult> =
      AsyncOperationCompletedHandler(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())

  public constructor(
    pointer: ComPtr,
    resultSignature: String,
  ) : this(pointer, AsyncResultTypes.signature(resultSignature))

  public open fun getResults(): TResult = resultType.getResults(pointer, 13)
}
