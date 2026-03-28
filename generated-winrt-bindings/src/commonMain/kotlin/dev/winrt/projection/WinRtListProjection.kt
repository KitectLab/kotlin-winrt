package dev.winrt.projection

class WinRtListProjection<T> internal constructor(
    private val sizeProvider: () -> Int,
    private val getter: (Int) -> T,
) : AbstractList<T>() {
    override val size: Int
        get() = sizeProvider()

    override fun get(index: Int): T {
        require(index >= 0) { "index must be non-negative" }
        return getter(index)
    }
}
