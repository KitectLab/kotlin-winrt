package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComReference
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult
import dev.winrt.kom.KomException
import dev.winrt.kom.PlatformComInterop

interface WinRtProjectedObject {
    val queryInterfaceCache: MutableMap<String, ComPtr>
    val additionalTypeData: MutableMap<String, Any>
}

open class WinRtObject(
    final override val pointer: ComPtr,
) : ComReference, WinRtProjectedObject {
    override val queryInterfaceCache: MutableMap<String, ComPtr> = linkedMapOf()
    override val additionalTypeData: MutableMap<String, Any> = linkedMapOf()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrPutAdditionalTypeData(key: String, factory: () -> T): T {
        return additionalTypeData.getOrPut(key, factory) as T
    }
}

open class Inspectable(pointer: ComPtr) : WinRtObject(pointer) {
    open fun queryInterface(iid: Guid): ComPtr {
        return PlatformComInterop.queryInterface(pointer, iid)
            .getOrElse { throw KomException("QueryInterface failed: ${it.message}") }
    }

    fun cachedQueryInterface(iid: Guid): ComPtr {
        return queryInterfaceCache.getOrPut(iid.toString()) {
            queryInterface(iid)
        }
    }

    open fun getObjectReferenceForType(typeKey: String, iid: Guid): ComPtr {
        return queryInterfaceCache.getOrPut(typeKey) {
            queryInterface(iid)
        }
    }

    fun getObjectReferenceForProjectedType(typeKey: String, iid: Guid): ComPtr {
        return getObjectReferenceForType(
            typeKey = WinRtProjectionRegistry.helperTypeKeyFor(typeKey),
            iid = iid,
        )
    }
}

data class RuntimeClassId(
    val namespace: String,
    val name: String,
) {
    val qualifiedName: String
        get() = "$namespace.$name"
}

class ActivationException(message: String) : RuntimeException(message)

interface ActivationFactoryProvider {
    fun <T : WinRtObject> activate(metadata: WinRtRuntimeClassMetadata, constructor: (ComPtr) -> T): Result<T>
}

object NullActivationFactoryProvider : ActivationFactoryProvider {
    override fun <T : WinRtObject> activate(metadata: WinRtRuntimeClassMetadata, constructor: (ComPtr) -> T): Result<T> {
        return Result.failure(ActivationException("Activation is not configured for ${metadata.classId.qualifiedName}"))
    }
}

object WinRtRuntime {
    var activationFactoryProvider: ActivationFactoryProvider = NullActivationFactoryProvider

    fun <T : WinRtObject> activate(metadata: WinRtRuntimeClassMetadata, constructor: (ComPtr) -> T): T {
        return activationFactoryProvider.activate(metadata, constructor).getOrElse { throw it }
    }

    fun check(result: HResult, operation: String) {
        result.requireSuccess(operation)
    }
}
