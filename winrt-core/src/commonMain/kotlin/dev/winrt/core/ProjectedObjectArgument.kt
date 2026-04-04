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
        else -> error(
            "Projected WinRT object arguments for $projectionTypeKey require a projected WinRT value; " +
                "plain Kotlin values are not supported yet",
        )
    }
}
