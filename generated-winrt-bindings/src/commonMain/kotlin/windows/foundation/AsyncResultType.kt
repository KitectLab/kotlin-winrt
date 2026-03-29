package windows.foundation

import dev.winrt.core.UInt32
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtTypeSignature

public class AsyncResultType<TResult> internal constructor(
  public val signature: String,
)

public object AsyncResultTypes {
  public val string: AsyncResultType<String> =
      AsyncResultType(WinRtTypeSignature.string())

  public val boolean: AsyncResultType<Boolean> =
      AsyncResultType("b1")

  public val int32: AsyncResultType<Int> =
      AsyncResultType("i4")

  public val uint32: AsyncResultType<UInt32> =
      AsyncResultType("u4")

  public val int64: AsyncResultType<Long> =
      AsyncResultType("i8")

  public val uint64: AsyncResultType<ULong> =
      AsyncResultType("u8")

  public val float32: AsyncResultType<Float> =
      AsyncResultType("f4")

  public val float64: AsyncResultType<Double> =
      AsyncResultType("f8")

  public fun <TResult> interfaceType(metadata: WinRtInterfaceMetadata): AsyncResultType<TResult> =
      AsyncResultType(WinRtTypeSignature.guid(metadata.iid.toString()))
}
