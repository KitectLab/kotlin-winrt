package dev.winrt.core

object WinRtProjectionRegistry {
    private val defaultProjectionTypeMappings: Map<String, String> = linkedMapOf(
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
    private val defaultAbiHelperTypeMappings: Map<String, String> = linkedMapOf(
        "System.Collections.IEnumerable" to "ABI.System.Collections.IEnumerable",
        "System.Collections.IList" to "ABI.System.Collections.IList",
        "System.Collections.Generic.IEnumerable" to "ABI.System.Collections.Generic.IEnumerable",
        "System.Collections.Generic.IEnumerator" to "ABI.System.Collections.Generic.IEnumerator",
        "System.Collections.Generic.IList" to "ABI.System.Collections.Generic.IList",
        "System.Collections.Generic.IReadOnlyList" to "ABI.System.Collections.Generic.IReadOnlyList",
        "System.Collections.Generic.IDictionary" to "ABI.System.Collections.Generic.IDictionary",
        "System.Collections.Generic.IReadOnlyDictionary" to "ABI.System.Collections.Generic.IReadOnlyDictionary",
        "System.Collections.Generic.KeyValuePair" to "ABI.System.Collections.Generic.KeyValuePair",
    )
    private val projectionTypeMappings: MutableMap<String, String> = linkedMapOf<String, String>().apply {
        putAll(defaultProjectionTypeMappings)
    }
    private val abiHelperTypeMappings: MutableMap<String, String> = linkedMapOf<String, String>().apply {
        putAll(defaultAbiHelperTypeMappings)
    }

    fun registerProjectionTypeMapping(
        winrtTypeKey: String,
        projectionTypeKey: String,
    ) {
        projectionTypeMappings[winrtTypeKey] = projectionTypeKey
    }

    fun registerAbiHelperTypeMapping(
        projectionTypeKey: String,
        abiHelperTypeKey: String,
    ) {
        abiHelperTypeMappings[projectionTypeKey] = abiHelperTypeKey
    }

    fun findProjectionTypeKey(winrtTypeKey: String): String? {
        return projectionTypeMappings[winrtTypeKey]
    }

    fun projectionTypeKeyFor(winrtTypeKey: String): String {
        return findProjectionTypeKey(winrtTypeKey) ?: winrtTypeKey
    }

    fun findAbiHelperTypeKey(projectionTypeKey: String): String? {
        val rawTypeName = projectionTypeKey.substringBefore('<')
        val genericArguments = projectionTypeKey
            .substringAfter('<', "")
            .substringBeforeLast('>', "")
        val rawHelperTypeName = abiHelperTypeMappings[rawTypeName] ?: return null
        return if (genericArguments.isEmpty()) {
            rawHelperTypeName
        } else {
            "$rawHelperTypeName<$genericArguments>"
        }
    }

    fun abiHelperTypeKeyFor(projectionTypeKey: String): String {
        return findAbiHelperTypeKey(projectionTypeKey) ?: projectionTypeKey
    }

    internal fun resetForTests() {
        projectionTypeMappings.clear()
        projectionTypeMappings.putAll(defaultProjectionTypeMappings)
        abiHelperTypeMappings.clear()
        abiHelperTypeMappings.putAll(defaultAbiHelperTypeMappings)
    }
}
