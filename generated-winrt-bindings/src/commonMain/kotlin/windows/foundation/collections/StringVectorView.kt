package windows.foundation.collections

import dev.winrt.core.UInt32
import dev.winrt.core.WinRtObject
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import dev.winrt.projection.WinRtListProjection
import kotlin.io.use

public open class StringVectorView(
  pointer: ComPtr,
) : WinRtObject(pointer),
  List<String> {
  private val listDelegate: List<String> = createListDelegate(pointer)

  public val winRtSize: UInt32
    get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())

  public fun getAt(index: UInt32): String {
    return PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow().use {
      it.toKotlinString()
    }
  }

  override val size: Int
    get() = listDelegate.size

  override fun contains(element: String): Boolean = listDelegate.contains(element)

  override fun containsAll(elements: Collection<String>): Boolean = listDelegate.containsAll(elements)

  override fun get(index: Int): String = listDelegate[index]

  override fun indexOf(element: String): Int = listDelegate.indexOf(element)

  override fun isEmpty(): Boolean = listDelegate.isEmpty()

  override fun iterator(): Iterator<String> = listDelegate.iterator()

  override fun lastIndexOf(element: String): Int = listDelegate.lastIndexOf(element)

  override fun listIterator(): ListIterator<String> = listDelegate.listIterator()

  override fun listIterator(index: Int): ListIterator<String> = listDelegate.listIterator(index)

  override fun subList(fromIndex: Int, toIndex: Int): List<String> = listDelegate.subList(fromIndex, toIndex)

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
