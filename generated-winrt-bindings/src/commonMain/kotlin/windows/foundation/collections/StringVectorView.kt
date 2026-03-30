package windows.foundation.collections

import dev.winrt.core.UInt32
import dev.winrt.core.WinRtObject
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.collections.Collection

public open class StringVectorView(
  pointer: ComPtr,
) : WinRtObject(pointer),
  List<String> {

  public val winRtSize: UInt32
    get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())

  public fun getAt(index: UInt32): String {
    val value = PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow()
    try {
      return value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override val size: Int
    get() = winRtSize.value.toInt()

  override fun contains(element: String): Boolean = indexOf(element) >= 0

  override fun containsAll(elements: Collection<String>): Boolean = elements.all { contains(it) }

  override fun get(index: Int): String = getAt(UInt32(index.toUInt()))

  override fun indexOf(element: String): Int {
    for (index in 0 until size) {
      if (get(index) == element) {
        return index
      }
    }
    return -1
  }

  override fun isEmpty(): Boolean = size == 0

  override fun iterator(): Iterator<String> = object : Iterator<String> {
    private var index = 0
    override fun hasNext(): Boolean = index < size
    override fun next(): String = get(index++)
  }

  override fun lastIndexOf(element: String): Int {
    for (index in size - 1 downTo 0) {
      if (get(index) == element) {
        return index
      }
    }
    return -1
  }

  override fun listIterator(): ListIterator<String> = listIterator(0)

  override fun listIterator(index: Int): ListIterator<String> = object : ListIterator<String> {
    private var current = index
    override fun hasNext(): Boolean = current < size
    override fun next(): String = get(current++)
    override fun hasPrevious(): Boolean = current > 0
    override fun previous(): String = get(--current)
    override fun nextIndex(): Int = current
    override fun previousIndex(): Int = current - 1
  }

  override fun subList(fromIndex: Int, toIndex: Int): List<String> =
      (fromIndex until toIndex).map { get(it) }
}
