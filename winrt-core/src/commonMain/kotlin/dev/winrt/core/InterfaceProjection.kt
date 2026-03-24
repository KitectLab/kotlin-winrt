package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop

open class WinRtInterfaceProjection(pointer: ComPtr) : Inspectable(pointer)

fun <T : WinRtObject> Inspectable.projectInterface(
    metadata: WinRtInterfaceMetadata,
    constructor: (ComPtr) -> T,
): T {
    val projected = PlatformComInterop.queryInterface(pointer, metadata.iid)
        .getOrElse { throw ProjectionException("Failed to project ${metadata.qualifiedName}: ${it.message}") }
    return metadata.project(projected, constructor)
}

class ProjectionException(message: String) : RuntimeException(message)
