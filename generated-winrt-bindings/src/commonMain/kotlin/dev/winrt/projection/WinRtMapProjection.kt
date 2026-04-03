package dev.winrt.projection

class WinRtMapProjection<K, V> internal constructor(
    private val sizeProvider: () -> Int,
    private val lookupFn: (K) -> V,
    private val containsKeyFn: (K) -> Boolean,
    private val entriesProvider: () -> Iterable<Map.Entry<K, V>>,
) : AbstractMap<K, V>() {
    override val size: Int
        get() = sizeProvider()

    override fun containsKey(key: K): Boolean = containsKeyFn(key)

    override fun get(key: K): V? {
        if (!containsKeyFn(key)) {
            return null
        }
        return lookupFn(key)
    }

    override val entries: Set<Map.Entry<K, V>>
        get() = entriesProvider().toSet()

    fun split(): Pair<Map<K, V>, Map<K, V>> {
        val entries = entriesProvider().toList()
        val midpoint = entries.size / 2
        val first = entries.take(midpoint).associate { it.key to it.value }
        val second = entries.drop(midpoint).associate { it.key to it.value }
        return first to second
    }
}
