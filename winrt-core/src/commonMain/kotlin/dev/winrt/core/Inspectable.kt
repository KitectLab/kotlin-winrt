package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComReference
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult
import dev.winrt.kom.KomException
import dev.winrt.kom.PlatformComInterop

open class WinRtObject(
    override val pointer: ComPtr,
) : ComReference {
    val queryInterfaceCache: MutableMap<String, ComPtr> = linkedMapOf()
    val additionalTypeData: MutableMap<String, Any> = linkedMapOf()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrPutAdditionalTypeData(key: String, factory: () -> T): T {
        return additionalTypeData.getOrPut(key, factory) as T
    }
}

open class Inspectable(pointer: ComPtr) : WinRtObject(pointer) {
    companion object {
        val iinspectableIid: Guid = guidOf("af86e2e0-b12d-4c6a-9c5a-d7aa65101e90")
    }

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
        return queryInterfaceCache.getOrPut(referenceCacheKey(typeKey, iid)) {
            queryInterface(iid)
        }
    }

    fun getObjectReferenceForProjectedType(typeKey: String, iid: Guid): ComPtr {
        val projectionTypeKey = WinRtProjectionRegistry.projectionTypeKeyFor(typeKey)
        val abiHelperTypeKey = WinRtProjectionRegistry.abiHelperTypeKeyFor(projectionTypeKey)
        return getOrPutProjectedTypeReference(typeKey, projectionTypeKey, abiHelperTypeKey, iid)
    }

    fun getInspectableArgumentPointer(): ComPtr {
        return getObjectReferenceForType("marshal:IInspectable", iinspectableIid)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrPutHelperWrapper(typeKey: String, factory: () -> T): T {
        return additionalTypeData.getOrPut("helper-wrapper:$typeKey", factory) as T
    }

    private fun getOrPutProjectedTypeReference(
        winrtTypeKey: String,
        projectionTypeKey: String,
        abiHelperTypeKey: String,
        iid: Guid,
    ): ComPtr {
        val projectionReferenceKey = referenceCacheKey(projectionTypeKey, iid)
        val abiReferenceKey = referenceCacheKey(abiHelperTypeKey, iid)
        val winrtReferenceKey = referenceCacheKey(winrtTypeKey, iid)
        queryInterfaceCache[projectionReferenceKey]?.let { cached ->
            if (queryInterfaceCache[abiReferenceKey] == null) {
                queryInterfaceCache[abiReferenceKey] = cached
            }
            if (winrtTypeKey != projectionTypeKey && queryInterfaceCache[winrtReferenceKey] == null) {
                queryInterfaceCache[winrtReferenceKey] = cached
            }
            return cached
        }
        queryInterfaceCache[winrtReferenceKey]?.let { cached ->
            if (queryInterfaceCache[projectionReferenceKey] == null) {
                queryInterfaceCache[projectionReferenceKey] = cached
            }
            if (queryInterfaceCache[abiReferenceKey] == null) {
                queryInterfaceCache[abiReferenceKey] = cached
            }
            return cached
        }
        val reference = getObjectReferenceForType(abiHelperTypeKey, iid)
        queryInterfaceCache[projectionReferenceKey] = reference
        if (winrtTypeKey != projectionTypeKey) {
            queryInterfaceCache[winrtReferenceKey] = reference
        }
        return reference
    }

    private fun referenceCacheKey(typeKey: String, iid: Guid): String {
        return "$typeKey:$iid"
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
    fun getActivationFactory(metadata: WinRtRuntimeClassMetadata, iid: Guid): Result<ComPtr>
}

object NullActivationFactoryProvider : ActivationFactoryProvider {
    override fun <T : WinRtObject> activate(metadata: WinRtRuntimeClassMetadata, constructor: (ComPtr) -> T): Result<T> {
        return Result.failure(ActivationException("Activation is not configured for ${metadata.classId.qualifiedName}"))
    }

    override fun getActivationFactory(metadata: WinRtRuntimeClassMetadata, iid: Guid): Result<ComPtr> {
        return Result.failure(ActivationException("Activation factory is not configured for ${metadata.classId.qualifiedName}"))
    }
}

object WinRtRuntime {
    var activationFactoryProvider: ActivationFactoryProvider = NullActivationFactoryProvider
    private val activationFactoryCache: MutableMap<String, ComPtr> = linkedMapOf()
    private val activationFactoryProjectionCache: MutableMap<String, Any> = linkedMapOf()

    fun <T : WinRtObject> activate(metadata: WinRtRuntimeClassMetadata, constructor: (ComPtr) -> T): T {
        return activationFactoryProvider.activate(metadata, constructor).getOrElse { throw it }
    }

    fun <T : WinRtObject> projectActivationFactory(
        runtimeClass: WinRtRuntimeClassMetadata,
        interfaceMetadata: WinRtInterfaceMetadata,
        constructor: (ComPtr) -> T,
    ): T {
        val projectionTypeCacheKey = "${runtimeClass.classId.qualifiedName}:projection-type:${interfaceMetadata.projectionTypeKey}:${interfaceMetadata.iid}"
        activationFactoryProjectionCache[projectionTypeCacheKey]?.let { cached ->
            @Suppress("UNCHECKED_CAST")
            return cached as T
        }
        val cacheKey = "${runtimeClass.classId.qualifiedName}:${interfaceMetadata.projectionCacheKey}:${interfaceMetadata.iid}"
        @Suppress("UNCHECKED_CAST")
        return activationFactoryProjectionCache.getOrPut(cacheKey) {
            val factory = activationFactoryCache.getOrPut(
                "${runtimeClass.classId.qualifiedName}:${interfaceMetadata.iid}",
            ) {
                activationFactoryProvider.getActivationFactory(runtimeClass, interfaceMetadata.iid)
                    .getOrElse { throw it }
            }
            interfaceMetadata.project(factory, constructor).also { wrapper ->
                activationFactoryProjectionCache.putIfAbsent(projectionTypeCacheKey, wrapper)
            }
        } as T
    }

    fun check(result: HResult, operation: String) {
        result.requireSuccess(operation)
    }

    internal fun resetForTests() {
        activationFactoryCache.clear()
        activationFactoryProjectionCache.clear()
    }
}
