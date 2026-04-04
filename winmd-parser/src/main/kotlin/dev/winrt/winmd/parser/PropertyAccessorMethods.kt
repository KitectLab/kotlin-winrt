package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdMethod

internal fun WinMdMethod.projectedPropertyNameOrNull(): String? {
    return when {
        name.startsWith("get_") && parameters.isEmpty() -> name.removePrefix("get_")
        name.startsWith("put_") && parameters.size == 1 -> name.removePrefix("put_")
        else -> null
    }
}

internal fun WinMdMethod.isProjectedPropertyAccessor(projectedPropertyNames: Set<String>): Boolean {
    return projectedPropertyNameOrNull() in projectedPropertyNames
}
