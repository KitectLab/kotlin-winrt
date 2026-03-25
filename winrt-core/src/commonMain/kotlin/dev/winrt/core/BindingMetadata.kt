package dev.winrt.core

import dev.winrt.kom.Guid

interface WinRtTypeMetadata {
    val qualifiedName: String
}

interface WinRtInterfaceMetadata : WinRtTypeMetadata {
    val iid: Guid
    fun <T : WinRtObject> project(pointer: dev.winrt.kom.ComPtr, constructor: (dev.winrt.kom.ComPtr) -> T): T = constructor(pointer)
}

interface WinRtRuntimeClassMetadata : WinRtTypeMetadata {
    val classId: RuntimeClassId
    val defaultInterfaceName: String?
    val activationKind: WinRtActivationKind
        get() = WinRtActivationKind.Factory
}

enum class WinRtActivationKind {
    Factory,
}

fun guidOf(value: String): Guid {
    val parts = value.split("-")
    require(parts.size == 5) { "Invalid GUID string: $value" }

    val tail = (parts[3] + parts[4]).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    return Guid(
        data1 = parts[0].toLong(16).toInt(),
        data2 = parts[1].toInt(16).toShort(),
        data3 = parts[2].toInt(16).toShort(),
        data4 = tail,
    )
}
