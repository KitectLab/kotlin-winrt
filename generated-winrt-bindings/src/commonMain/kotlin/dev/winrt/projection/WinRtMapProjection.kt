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
}
