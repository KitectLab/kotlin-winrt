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

    fun split(): Pair<Map<K, V>?, Map<K, V>?> {
        val entries = entriesProvider()
            .toList()
            .sortedWith { first, second -> compareKeys(first.key, second.key) }
        if (entries.size < 2) {
            return null to null
        }
        val pivot = entries.lastIndex / 2
        val first = entries.subList(0, pivot + 1).associateTo(linkedMapOf()) { it.key to it.value }
        val second = entries.subList(pivot + 1, entries.size).associateTo(linkedMapOf()) { it.key to it.value }
        return first to second
    }

    @Suppress("UNCHECKED_CAST")
    private fun compareKeys(first: K, second: K): Int {
        val comparable = first as? Comparable<Any?>
            ?: error("WinRT map keys must be comparable to split a map view")
        return comparable.compareTo(second as Any?)
    }
}
