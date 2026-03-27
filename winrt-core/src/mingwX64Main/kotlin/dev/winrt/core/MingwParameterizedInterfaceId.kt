package dev.winrt.core

import dev.winrt.kom.Guid

actual object ParameterizedInterfaceId {
    actual fun createFromSignature(signature: String): Guid {
        error("ParameterizedInterfaceId is not implemented for mingwX64 yet")
    }
}
