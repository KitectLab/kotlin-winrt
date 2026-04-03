package dev.winrt.core

import dev.winrt.kom.Guid

actual object ParameterizedInterfaceId {
    actual fun createFromSignature(signature: String): Guid {
        return ParameterizedInterfaceIdSupport.createFromSignature(signature)
    }
}
