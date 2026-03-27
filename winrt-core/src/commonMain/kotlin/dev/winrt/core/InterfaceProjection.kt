package dev.winrt.core

import dev.winrt.kom.ComPtr

open class WinRtInterfaceProjection(pointer: ComPtr) : Inspectable(pointer)

fun <T : WinRtObject> Inspectable.projectInterface(
    metadata: WinRtInterfaceMetadata,
    constructor: (ComPtr) -> T,
): T {
    val projected = try {
        getObjectReferenceForProjectedType(metadata.qualifiedName, metadata.iid)
    } catch (error: Throwable) {
        throw ProjectionException("Failed to project ${metadata.qualifiedName}: ${error.message}")
    }
    return metadata.project(projected, constructor)
}

class ProjectionException(message: String) : RuntimeException(message)
