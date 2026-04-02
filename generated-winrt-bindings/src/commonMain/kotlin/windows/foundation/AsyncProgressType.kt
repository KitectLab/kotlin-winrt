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
  ): AsyncProgressType<TProgress> = create(signature, argumentKind)

  public val string: AsyncProgressType<String> =
      create(WinRtTypeSignature.string(), WinRtDelegateValueKind.STRING)

  public val boolean: AsyncProgressType<Boolean> =
      create("b1", WinRtDelegateValueKind.BOOLEAN)

  public val int32: AsyncProgressType<Int> =
      create("i4", WinRtDelegateValueKind.INT32)

  public val uint32: AsyncProgressType<UInt32> =
      create("u4", WinRtDelegateValueKind.UINT32)

  public val int64: AsyncProgressType<Long> =
      create("i8", WinRtDelegateValueKind.INT64)

  public val uint64: AsyncProgressType<ULong> =
      create("u8", WinRtDelegateValueKind.UINT64)

  public val float32: AsyncProgressType<Float> =
      create("f4", WinRtDelegateValueKind.FLOAT32)

  public val float64: AsyncProgressType<Double> =
      create("f8", WinRtDelegateValueKind.FLOAT64)

  public fun <TProgress> interfaceType(metadata: WinRtInterfaceMetadata): AsyncProgressType<TProgress> =
      create(
          signature = WinRtTypeSignature.guid(metadata.iid.toString()),
          argumentKind = WinRtDelegateValueKind.OBJECT,
      )

  private fun <TProgress> create(
    signature: String,
    argumentKind: WinRtDelegateValueKind,
  ): AsyncProgressType<TProgress> = AsyncProgressType(signature, argumentKind)
}
