package dev.winrt.core

import dev.winrt.kom.ComPtr

fun projectedObjectArgumentPointer(
    value: Any,
    projectionTypeKey: String,
    signature: String,
): ComPtr {
    val iid = projectedObjectArgumentInterfaceId(signature)
    return when (value) {
        is Inspectable -> value.getObjectReferenceForProjectedType(projectionTypeKey, iid)
        else -> WinRtProjectedObjectAuthoringBridge.createPointerOrNull(value, projectionTypeKey, signature)
            ?: error(
                "Projected WinRT object arguments for $projectionTypeKey require a projected WinRT value " +
                    "or a supported plain Kotlin collection/map value",
            )
    }
}

internal fun projectedObjectArgumentInterfaceId(signature: String): dev.winrt.kom.Guid {
    return when {
        signature == WinRtTypeSignature.object_() -> Inspectable.iinspectableIid
        signature.startsWith("{") && signature.endsWith("}") ->
            guidOf(signature.removePrefix("{").removeSuffix("}"))
        signature.startsWith("delegate(") && signature.endsWith(")") ->
            projectedObjectArgumentInterfaceId(signature.removePrefix("delegate(").removeSuffix(")"))
        signature.startsWith("pinterface(") ->
            ParameterizedInterfaceId.createFromSignature(signature)
        signature.startsWith("rc(") && signature.endsWith(")") -> {
            val content = signature.removePrefix("rc(").removeSuffix(")")
            val separatorIndex = findTopLevelSeparator(content, ';')
            require(separatorIndex >= 0) { "Invalid runtime class signature: $signature" }
            val defaultInterfaceSignature = content.substring(separatorIndex + 1)
            projectedObjectArgumentInterfaceId(defaultInterfaceSignature)
        }
        else -> error("Unsupported projected object argument signature: $signature")
    }
}

private fun findTopLevelSeparator(source: String, separator: Char): Int {
    var parenthesisDepth = 0
    for (index in source.indices) {
        when (source[index]) {
            '(' -> parenthesisDepth += 1
            ')' -> parenthesisDepth -= 1
            separator -> if (parenthesisDepth == 0) {
                return index
            }
        }
    }
    return -1
}
