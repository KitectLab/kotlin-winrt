package dev.winrt.core

object WinRtProjectionRegistry {
    private val helperTypeMappings: MutableMap<String, String> = linkedMapOf()

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

    internal fun clearForTests() {
        helperTypeMappings.clear()
    }
}
