package windows.foundation.collections

import dev.winrt.core.UInt32
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.collections.AbstractList
import kotlin.io.use

public open class StringVectorView(
  pointer: ComPtr,
) : AbstractList<String>() {
  public val pointer: ComPtr = pointer

  public val winRtSize: UInt32
    get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())

  public fun getAt(index: UInt32): String {
    return PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow().use {
      it.toKotlinString()
    }
  }

  override val size: Int
    get() = winRtSize.value.toInt()

  override fun get(index: Int): String = getAt(UInt32(index.toUInt()))
}
