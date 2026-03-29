package dev.winrt.projection

class WinRtMutableListProjection<T> internal constructor(
    private val sizeProvider: () -> Int,
    private val getter: (Int) -> T,
    private val append: (T) -> Unit,
    private val clearer: () -> Unit,
) : AbstractMutableList<T>() {
    override val size: Int
        get() = sizeProvider()

    override fun get(index: Int): T {
        require(index >= 0) { "index must be non-negative" }
        return getter(index)
    }

    override fun add(index: Int, element: T) {
        require(index in 0..size) { "index must be between 0 and size" }
        if (index == size) {
            append(element)
            return
        }
        val items = snapshot()
        items.add(index, element)
        rewrite(items)
    }

    override fun set(index: Int, element: T): T {
        require(index in 0 until size) { "index must be between 0 and size" }
        val items = snapshot()
        val previous = items.set(index, element)
        rewrite(items)
        return previous
    }

    override fun removeAt(index: Int): T {
        require(index in 0 until size) { "index must be between 0 and size" }
        val items = snapshot()
        val removed = items.removeAt(index)
        rewrite(items)
        return removed
    }

    override fun clear() {
        clearer()
    }

    private fun snapshot(): MutableList<T> =
        MutableList(sizeProvider()) { getter(it) }

    private fun rewrite(items: List<T>) {
        clearer()
        items.forEach { append(it) }
    }
}
