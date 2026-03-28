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
        if (index != size) {
            throw UnsupportedOperationException("insert at arbitrary index is not implemented yet")
        }
        append(element)
    }

    override fun set(index: Int, element: T): T {
        throw UnsupportedOperationException("set is not implemented yet")
    }

    override fun removeAt(index: Int): T {
        throw UnsupportedOperationException("removeAt is not implemented yet")
    }

    override fun clear() {
        clearer()
    }
}
