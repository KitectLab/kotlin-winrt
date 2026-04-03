package dev.winrt.projection

class WinRtMutableMapProjection<K, V> internal constructor(
    private val sizeProvider: () -> Int,
    private val lookupFn: (K) -> V,
    private val containsKeyFn: (K) -> Boolean,
    private val putValueFn: (K, V) -> Boolean,
    private val removeKeyFn: (K) -> Boolean,
    private val clearerFn: () -> Unit,
    private val entriesProvider: () -> Iterable<Map.Entry<K, V>>,
) : AbstractMutableMap<K, V>() {
    override val size: Int
        get() = sizeProvider()

    override fun containsKey(key: K): Boolean = containsKeyFn(key)

    override fun get(key: K): V? {
        if (!containsKeyFn(key)) {
            return null
        }
        return lookupFn(key)
    }

    override fun put(key: K, value: V): V? {
        val previous = if (containsKeyFn(key)) lookupFn(key) else null
        putValueFn(key, value)
        return previous
    }

    override fun remove(key: K): V? {
        val previous = if (containsKeyFn(key)) lookupFn(key) else null
        removeKeyFn(key)
        return previous
    }

    override fun clear() {
        clearerFn()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = entriesProvider().mapTo(linkedSetOf()) { MapEntry(it.key, it.value) }

    fun snapshot(): MutableMap<K, V> = entriesProvider().associateTo(linkedMapOf()) { it.key to it.value }

    private data class MapEntry<K, V>(
        override val key: K,
        override var value: V,
    ) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V {
            val previous = value
            value = newValue
            return previous
        }
    }
}
