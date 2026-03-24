package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComReference
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult
import dev.winrt.kom.KomException
import dev.winrt.kom.PlatformComInterop

open class WinRtObject(
    final override val pointer: ComPtr,
) : ComReference

open class Inspectable(pointer: ComPtr) : WinRtObject(pointer) {
    fun queryInterface(iid: Guid): ComPtr {
        return PlatformComInterop.queryInterface(pointer, iid)
            .getOrElse { throw KomException("QueryInterface failed: ${it.message}") }
    }
}

data class EventRegistrationToken(val value: Long)

data class RuntimeClassId(
    val namespace: String,
    val name: String,
) {
    val qualifiedName: String
        get() = "$namespace.$name"
}

class ActivationException(message: String) : RuntimeException(message)

interface ActivationFactoryProvider {
    fun <T : WinRtObject> activate(classId: RuntimeClassId, constructor: (ComPtr) -> T): Result<T>
}

object NullActivationFactoryProvider : ActivationFactoryProvider {
    override fun <T : WinRtObject> activate(classId: RuntimeClassId, constructor: (ComPtr) -> T): Result<T> {
        return Result.failure(ActivationException("Activation is not configured for ${classId.qualifiedName}"))
    }
}

object WinRtRuntime {
    var activationFactoryProvider: ActivationFactoryProvider = NullActivationFactoryProvider

    fun <T : WinRtObject> activate(classId: RuntimeClassId, constructor: (ComPtr) -> T): T {
        return activationFactoryProvider.activate(classId, constructor).getOrElse { throw it }
    }

    fun check(result: HResult, operation: String) {
        result.requireSuccess(operation)
    }
}
