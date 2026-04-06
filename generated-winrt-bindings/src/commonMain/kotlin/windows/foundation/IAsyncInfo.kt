package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import java.lang.Exception
import kotlin.String

public open class IAsyncInfo(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val errorCode: Exception?
    get() = Exception?.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 8,
        Exception?.ABI_LAYOUT).getOrThrow())

  public val id: UInt32
    get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 6).getOrThrow())

  public val status: AsyncStatus
    get() = AsyncStatus.fromValue(PlatformComInterop.invokeInt32Method(pointer, 7).getOrThrow())

  public fun cancel() {
    PlatformComInterop.invokeUnitMethod(pointer, 9).getOrThrow()
  }

  public fun close() {
    PlatformComInterop.invokeUnitMethod(pointer, 10).getOrThrow()
  }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.IAsyncInfo"

    override val projectionTypeKey: String = "Windows.Foundation.IAsyncInfo"

    override val iid: Guid = guidOf("00000036-0000-0000-c000-000000000046")

    public fun from(inspectable: Inspectable): IAsyncInfo = inspectable.projectInterface(this,
        ::IAsyncInfo)

    public operator fun invoke(inspectable: Inspectable): IAsyncInfo = from(inspectable)
  }
}
