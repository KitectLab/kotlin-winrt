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
        val reference = queryInterfaceCache.getOrPut(referenceCacheKey(typeKey, iid)) {
            queryInterface(iid)
        }
        val projectionTypeKey = WinRtProjectionRegistry.findProjectionTypeKey(typeKey)
        if (projectionTypeKey != null) {
            val projectionReferenceKey = referenceCacheKey(projectionTypeKey, iid)
            if (queryInterfaceCache[projectionReferenceKey] == null) {
                queryInterfaceCache[projectionReferenceKey] = reference
            }
            val abiHelperTypeKey = WinRtProjectionRegistry.findAbiHelperTypeKey(projectionTypeKey)
            if (abiHelperTypeKey != null) {
                val abiReferenceKey = referenceCacheKey(abiHelperTypeKey, iid)
                if (queryInterfaceCache[abiReferenceKey] == null) {
                    queryInterfaceCache[abiReferenceKey] = reference
                }
            }
        }
        return reference
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
        val sharedIidCacheKey = "${runtimeClass.classId.qualifiedName}:projection-iid:${interfaceMetadata.iid}"
        activationFactoryProjectionCache[projectionTypeCacheKey]?.let { cached ->
            @Suppress("UNCHECKED_CAST")
            return cached as T
        }
        activationFactoryProjectionCache[sharedIidCacheKey]?.let { cached ->
            if (projectionTypeCacheKey !in activationFactoryProjectionCache) {
                activationFactoryProjectionCache[projectionTypeCacheKey] = cached
            }
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
                if (projectionTypeCacheKey !in activationFactoryProjectionCache) {
                    activationFactoryProjectionCache[projectionTypeCacheKey] = wrapper
                }
                if (sharedIidCacheKey !in activationFactoryProjectionCache) {
                    activationFactoryProjectionCache[sharedIidCacheKey] = wrapper
                }
            }
        } as T
    }

    fun <T : WinRtObject> compose(
        runtimeClass: WinRtRuntimeClassMetadata,
        factoryIid: Guid,
        defaultInterfaceIid: Guid?,
        constructor: (ComPtr) -> T,
        vtableIndex: Int,
        vararg arguments: Any,
    ): T {
        val factory = activationFactoryPointer(runtimeClass, factoryIid)
        val composed = PlatformComInterop.invokeComposableMethod(factory, vtableIndex, *arguments)
            .getOrElse { throw it }
        val projectedPointer = defaultInterfaceIid
            ?.let { iid -> queryComposableDefaultInterface(composed.instance, composed.inner, iid) }
            ?: composed.inner
        return try {
            constructor(projectedPointer)
        } finally {
            val releasedPointers = linkedSetOf<ComPtr>()
            releaseComposablePointer(composed.instance, projectedPointer, releasedPointers)
            releaseComposablePointer(composed.inner, projectedPointer, releasedPointers)
        }
    }

    fun check(result: HResult, operation: String) {
        result.requireSuccess(operation)
    }

    internal fun resetForTests() {
        activationFactoryCache.clear()
        activationFactoryProjectionCache.clear()
    }

    private fun activationFactoryPointer(runtimeClass: WinRtRuntimeClassMetadata, iid: Guid): ComPtr {
        return activationFactoryCache.getOrPut("${runtimeClass.classId.qualifiedName}:$iid") {
            activationFactoryProvider.getActivationFactory(runtimeClass, iid)
                .getOrElse { throw it }
        }
    }

    private fun releaseComposablePointer(pointer: ComPtr, retainedPointer: ComPtr, releasedPointers: MutableSet<ComPtr>) {
        if (!pointer.isNull && pointer != retainedPointer && releasedPointers.add(pointer)) {
            PlatformComInterop.release(pointer)
        }
    }

    private fun queryComposableDefaultInterface(instance: ComPtr, inner: ComPtr, iid: Guid): ComPtr {
        return PlatformComInterop.queryInterface(inner, iid).getOrElse {
            PlatformComInterop.queryInterface(instance, iid).getOrElse { throw it }
        }
    }
}
