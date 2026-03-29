package windows.foundation

import dev.winrt.core.UInt32
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtTypeSignature

public class AsyncProgressType<TProgress> internal constructor(
  public val signature: String,
  public val argumentKind: WinRtDelegateValueKind,
)

public object AsyncProgressTypes {
  public fun <TProgress> signature(
    signature: String,
    argumentKind: WinRtDelegateValueKind,
  ): AsyncProgressType<TProgress> = AsyncProgressType(signature, argumentKind)

  public val string: AsyncProgressType<String> =
      AsyncProgressType(WinRtTypeSignature.string(), WinRtDelegateValueKind.STRING)

  public val boolean: AsyncProgressType<Boolean> =
      AsyncProgressType("b1", WinRtDelegateValueKind.BOOLEAN)

  public val int32: AsyncProgressType<Int> =
      AsyncProgressType("i4", WinRtDelegateValueKind.INT32)

  public val uint32: AsyncProgressType<UInt32> =
      AsyncProgressType("u4", WinRtDelegateValueKind.UINT32)

  public val int64: AsyncProgressType<Long> =
      AsyncProgressType("i8", WinRtDelegateValueKind.INT64)

  public val uint64: AsyncProgressType<ULong> =
      AsyncProgressType("u8", WinRtDelegateValueKind.UINT64)

  public val float32: AsyncProgressType<Float> =
      AsyncProgressType("f4", WinRtDelegateValueKind.FLOAT32)

  public val float64: AsyncProgressType<Double> =
      AsyncProgressType("f8", WinRtDelegateValueKind.FLOAT64)

  public fun <TProgress> interfaceType(metadata: WinRtInterfaceMetadata): AsyncProgressType<TProgress> =
      AsyncProgressType(
          signature = WinRtTypeSignature.guid(metadata.iid.toString()),
          argumentKind = WinRtDelegateValueKind.OBJECT,
      )
}
