package windows.foundation.collections

import dev.winrt.core.UInt32
import dev.winrt.projection.WinRtListProjection
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.io.use

public open class StringVectorView(
  pointer: ComPtr,
) : List<String> by createListDelegate(pointer) {
  public val pointer: ComPtr = pointer

  public val winRtSize: UInt32
    get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())

  public fun getAt(index: UInt32): String {
    return PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow().use {
      it.toKotlinString()
    }
  }

  public companion object {
    private fun createListDelegate(pointer: ComPtr): List<String> =
        WinRtListProjection(
            sizeProvider = {
              UInt32(PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow()).value.toInt()
            },
            getter = { index ->
              PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 6, index.toUInt()).getOrThrow().use {
                it.toKotlinString()
              }
            },
        )
  }
}
