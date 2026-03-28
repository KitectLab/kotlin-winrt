package dev.winrt.core

object WinRtProjectionRegistry {
    private val defaultHelperTypeMappings: Map<String, String> = linkedMapOf(
        "Microsoft.UI.Xaml.Interop.IBindableIterable" to "System.Collections.IEnumerable",
        "Microsoft.UI.Xaml.Interop.IBindableVector" to "System.Collections.IList",
        "Windows.Foundation.Collections.IIterable`1" to "System.Collections.Generic.IEnumerable",
        "Windows.Foundation.Collections.IIterator`1" to "System.Collections.Generic.IEnumerator",
        "Windows.Foundation.Collections.IVector`1" to "System.Collections.Generic.IList",
        "Windows.Foundation.Collections.IVectorView`1" to "System.Collections.Generic.IReadOnlyList",
        "Windows.Foundation.Collections.IMap`2" to "System.Collections.Generic.IDictionary",
        "Windows.Foundation.Collections.IMapView`2" to "System.Collections.Generic.IReadOnlyDictionary",
        "Windows.Foundation.Collections.IKeyValuePair`2" to "System.Collections.Generic.KeyValuePair",
    )
    private val helperTypeMappings: MutableMap<String, String> = linkedMapOf<String, String>().apply {
        putAll(defaultHelperTypeMappings)
    }

    fun registerHelperTypeMapping(
        publicTypeKey: String,
        helperTypeKey: String,
    ) {
        helperTypeMappings[publicTypeKey] = helperTypeKey
    }

    fun findHelperTypeKey(publicTypeKey: String): String? {
        return helperTypeMappings[publicTypeKey]
    }

    fun helperTypeKeyFor(publicTypeKey: String): String {
        return findHelperTypeKey(publicTypeKey) ?: publicTypeKey
    }

    internal fun resetForTests() {
        helperTypeMappings.clear()
        helperTypeMappings.putAll(defaultHelperTypeMappings)
    }
}
