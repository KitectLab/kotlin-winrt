package dev.winrt.core

object WinRtProjectionRegistry {
    private val defaultProjectionTypeMappings: Map<String, String> = linkedMapOf(
        "Microsoft.UI.Xaml.Interop.IBindableIterable" to "kotlin.collections.Iterable",
        "Microsoft.UI.Xaml.Interop.IBindableIterator" to "kotlin.collections.Iterator",
        "Microsoft.UI.Xaml.Interop.IBindableVector" to "kotlin.collections.MutableList",
        "Microsoft.UI.Xaml.Interop.IBindableVectorView" to "kotlin.collections.List",
        "Windows.Foundation.Collections.IIterable`1" to "kotlin.collections.Iterable",
        "Windows.Foundation.Collections.IIterator`1" to "kotlin.collections.Iterator",
        "Windows.Foundation.Collections.IVector`1" to "kotlin.collections.MutableList",
        "Windows.Foundation.Collections.IVectorView`1" to "kotlin.collections.List",
        "Windows.Foundation.Collections.IMap`2" to "kotlin.collections.MutableMap",
        "Windows.Foundation.Collections.IMapView`2" to "kotlin.collections.Map",
        "Windows.Foundation.Collections.IKeyValuePair`2" to "kotlin.collections.Map.Entry",
        "Windows.Foundation.Collections.IObservableVector`1" to "kotlin.collections.MutableList",
        "Windows.Foundation.Collections.IObservableMap`2" to "kotlin.collections.MutableMap",
    )
    private val defaultAbiHelperTypeMappings: Map<String, String> = linkedMapOf(
        "System.Collections.IEnumerable" to "ABI.System.Collections.IEnumerable",
        "kotlin.collections.Iterable" to "ABI.System.Collections.IEnumerable",
        "System.Collections.IList" to "ABI.System.Collections.IList",
        "kotlin.collections.Iterator" to "ABI.System.Collections.Generic.IEnumerator",
        "kotlin.collections.List" to "ABI.System.Collections.Generic.IReadOnlyList",
        "kotlin.collections.MutableList" to "ABI.System.Collections.Generic.IList",
        "kotlin.collections.Map" to "ABI.System.Collections.Generic.IReadOnlyDictionary",
        "kotlin.collections.MutableMap" to "ABI.System.Collections.Generic.IDictionary",
        "kotlin.collections.Map.Entry" to "ABI.System.Collections.Generic.KeyValuePair",
        "System.Collections.Generic.IEnumerable" to "ABI.System.Collections.Generic.IEnumerable",
        "System.Collections.Generic.IEnumerator" to "ABI.System.Collections.Generic.IEnumerator",
        "System.Collections.Generic.IList" to "ABI.System.Collections.Generic.IList",
        "System.Collections.Generic.IReadOnlyList" to "ABI.System.Collections.Generic.IReadOnlyList",
        "System.Collections.Generic.IDictionary" to "ABI.System.Collections.Generic.IDictionary",
        "System.Collections.Generic.IReadOnlyDictionary" to "ABI.System.Collections.Generic.IReadOnlyDictionary",
        "System.Collections.Generic.KeyValuePair" to "ABI.System.Collections.Generic.KeyValuePair",
        "kotlin.collections.Iterable" to "ABI.System.Collections.Generic.IEnumerable",
        "kotlin.collections.Iterator" to "ABI.System.Collections.Generic.IEnumerator",
        "kotlin.collections.List" to "ABI.System.Collections.Generic.IReadOnlyList",
        "kotlin.collections.MutableList" to "ABI.System.Collections.Generic.IList",
        "kotlin.collections.Map" to "ABI.System.Collections.Generic.IReadOnlyDictionary",
        "kotlin.collections.MutableMap" to "ABI.System.Collections.Generic.IDictionary",
        "kotlin.collections.Map.Entry" to "ABI.System.Collections.Generic.KeyValuePair",
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
        kotlinCollectionAbiHelperTypeKey(rawTypeName, genericArguments)?.let { return it }
        val rawHelperTypeName = abiHelperTypeMappings[rawTypeName] ?: return null
        return if (genericArguments.isEmpty()) {
            rawHelperTypeName
        } else {
            "$rawHelperTypeName<$genericArguments>"
        }
    }

    private fun kotlinCollectionAbiHelperTypeKey(rawTypeName: String, genericArguments: String): String? {
        return when (rawTypeName) {
            "kotlin.collections.Iterable" -> if (genericArguments.isEmpty()) {
                "ABI.System.Collections.IEnumerable"
            } else {
                "ABI.System.Collections.Generic.IEnumerable<$genericArguments>"
            }
            "kotlin.collections.Iterator" -> "ABI.System.Collections.Generic.IEnumerator"
            "kotlin.collections.List" -> if (genericArguments.isEmpty()) {
                "ABI.System.Collections.Generic.IReadOnlyList"
            } else {
                "ABI.System.Collections.Generic.IReadOnlyList<$genericArguments>"
            }
            "kotlin.collections.MutableList" -> if (genericArguments.isEmpty()) {
                "ABI.System.Collections.IList"
            } else {
                "ABI.System.Collections.Generic.IList<$genericArguments>"
            }
            "kotlin.collections.Map" -> if (genericArguments.isEmpty()) {
                "ABI.System.Collections.Generic.IReadOnlyDictionary"
            } else {
                "ABI.System.Collections.Generic.IReadOnlyDictionary<$genericArguments>"
            }
            "kotlin.collections.MutableMap" -> if (genericArguments.isEmpty()) {
                "ABI.System.Collections.Generic.IDictionary"
            } else {
                "ABI.System.Collections.Generic.IDictionary<$genericArguments>"
            }
            "kotlin.collections.Map.Entry" -> "ABI.System.Collections.Generic.KeyValuePair<$genericArguments>"
            else -> null
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
