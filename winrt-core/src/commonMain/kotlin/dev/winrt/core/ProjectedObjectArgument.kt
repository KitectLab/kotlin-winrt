package dev.winrt.core

import dev.winrt.kom.ComPtr

fun projectedObjectArgumentPointer(
    value: Any,
    projectionTypeKey: String,
    signature: String,
): ComPtr {
    val iid = ParameterizedInterfaceId.createFromSignature(signature)
    return when (value) {
        is Inspectable -> value.getObjectReferenceForProjectedType(projectionTypeKey, iid)
        else -> WinRtProjectedObjectAuthoringBridge.createPointerOrNull(value, projectionTypeKey, signature)
            ?: error(
                "Projected WinRT object arguments for $projectionTypeKey require a projected WinRT value " +
                    "or a supported plain Kotlin collection/map value",
            )
    }
}
