package dev.winrt.core

import dev.winrt.kom.Guid

expect object ParameterizedInterfaceId {
    fun createFromSignature(signature: String): Guid
}
