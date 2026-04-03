package windows.foundation

import dev.winrt.core.ParameterizedInterfaceId
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

public open class IAsyncActionWithProgress<TProgress>(
  pointer: ComPtr,
  public open val progressSignature: String,
  public open val progressArgumentKind: WinRtDelegateValueKind,
  public open val decodeProgress: (Any?) -> TProgress,
) : IAsyncInfo(pointer) {
  public open var progress: AsyncActionProgressHandler<TProgress>
    get() = get_Progress()
    set(value) {
      put_Progress(value)
    }

  public open var completed: AsyncActionWithProgressCompletedHandler<TProgress>
    get() = get_Completed()
    set(value) {
      put_Completed(value)
    }

  public open fun put_Progress(handler: AsyncActionProgressHandler<TProgress>) {
    PlatformComInterop.invokeObjectSetter(pointer, 11, handler.pointer).getOrThrow()
  }

  public open fun get_Progress(): AsyncActionProgressHandler<TProgress> =
      AsyncActionProgressHandler(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())

  public open fun put_Completed(handler: AsyncActionWithProgressCompletedHandler<TProgress>) {
    PlatformComInterop.invokeObjectSetter(pointer, 13, handler.pointer).getOrThrow()
  }

  public open fun get_Completed(): AsyncActionWithProgressCompletedHandler<TProgress> =
      AsyncActionWithProgressCompletedHandler(PlatformComInterop.invokeObjectMethod(pointer, 14).getOrThrow())

  public open fun getResults() {
    PlatformComInterop.invokeUnitMethod(pointer, 15).getOrThrow()
  }

  public companion object {
    private const val genericInterfaceGuid: String = "1f6db258-e803-48a1-9546-eb7353398884"

    public fun signatureOf(progressSignature: String): String =
        WinRtTypeSignature.parameterizedInterface(genericInterfaceGuid, progressSignature)

    public fun iidOf(progressSignature: String): Guid =
        ParameterizedInterfaceId.createFromSignature(signatureOf(progressSignature))
  }
}
