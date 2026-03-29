package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.Int32
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult
import dev.winrt.kom.PlatformComInterop

public open class IAsyncInfo(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public open val id: UInt32
    get() = get_Id()

  public open val status: AsyncStatus
    get() = get_Status()

  public open val errorCode: HResult
    get() = get_ErrorCode()

  public open fun get_Id(): UInt32 =
      UInt32(PlatformComInterop.invokeUInt32Method(pointer, 6).getOrThrow())

  public open fun get_Status(): AsyncStatus =
      AsyncStatus.fromValue(PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow().toInt())

  public open fun get_ErrorCode(): HResult =
      HResult(PlatformComInterop.invokeInt32Method(pointer, 8).getOrThrow())

  public open fun cancel() {
    PlatformComInterop.invokeUnitMethod(pointer, 9).getOrThrow()
  }

  public open fun close() {
    PlatformComInterop.invokeUnitMethod(pointer, 10).getOrThrow()
  }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.IAsyncInfo"

    override val projectionTypeKey: String = "Windows.Foundation.IAsyncInfo"

    override val iid: Guid = guidOf("00000036-0000-0000-c000-000000000046")

    public fun from(inspectable: Inspectable): IAsyncInfo =
        inspectable.projectInterface(this, ::IAsyncInfo)
  }
}
