package dev.winrt.core

import dev.winrt.kom.ComPtr

open class WinRtInterfaceProjection(pointer: ComPtr) : Inspectable(pointer)

fun <T : WinRtObject> Inspectable.projectInterface(
    metadata: WinRtInterfaceMetadata,
    constructor: (ComPtr) -> T,
): T {
    additionalTypeData["projection-type:${metadata.projectionTypeKey}"]?.let { cached ->
        @Suppress("UNCHECKED_CAST")
        return cached as T
    }
    return getOrPutAdditionalTypeData(metadata.projectionCacheKey) {
        val projected = try {
            getObjectReferenceForProjectedType(metadata.projectionTypeKey, metadata.iid)
        } catch (error: Throwable) {
            throw ProjectionException("Failed to project ${metadata.qualifiedName}: ${error.message}")
        }
        metadata.project(projected, constructor).also { wrapper ->
            additionalTypeData.putIfAbsent("projection-type:${metadata.projectionTypeKey}", wrapper)
        }
    }
}

class ProjectionException(message: String) : RuntimeException(message)
