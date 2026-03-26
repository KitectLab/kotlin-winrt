package windows.foundation.collections

import dev.winrt.core.UInt32
import dev.winrt.core.WinRtStrings
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop

public open class StringVectorView(
  pointer: ComPtr,
) {
  public val pointer: ComPtr = pointer

  public val size: UInt32
    get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())

  public fun getAt(index: UInt32): String {
    val value = PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }
}
