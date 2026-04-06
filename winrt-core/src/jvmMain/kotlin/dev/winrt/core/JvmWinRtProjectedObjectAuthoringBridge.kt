package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComStructValue
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult
import dev.winrt.kom.PlatformComInterop
import java.util.IdentityHashMap

internal actual object WinRtProjectedObjectAuthoringBridge {
    private val iterableIidText = "faa585ea-6214-4217-afda-7f46de5869b3"
    private val iteratorIidText = "6a79e863-4300-459a-9966-cbb660963ee1"
    private val vectorViewIidText = "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56"
    private val mapViewIidText = "e480ce40-a338-4ada-adcf-272272e48cb9"
    private val keyValuePairIidText = "02b51929-c1c4-4a7e-8940-0312b5c18500"
    private val iReferenceIidText = "61c17706-2d65-11e0-9ae8-d48564015472"
    private val bindableIterableIidText = "036d2c08-df29-41af-8aa2-d774be62ba6f"
    private val bindableIteratorIidText = "6a1d6c07-076d-49f2-8314-f52c9c9a8331"
    private val bindableVectorIidText = "393de7de-6fd0-4c0d-bb71-47244a113e93"
    private val bindableVectorViewIidText = "346dd6e7-976e-4bc3-815d-ece243bc0f33"
    private val hResultStructSignature = WinRtTypeSignature.struct("Windows.Foundation.HResult", "i4")

    private val iterableIid = guidOf(iterableIidText)
    private val iteratorIid = guidOf(iteratorIidText)
    private val vectorViewIid = guidOf(vectorViewIidText)
    private val mapViewIid = guidOf(mapViewIidText)
    private val keyValuePairIid = guidOf(keyValuePairIidText)
    private val iReferenceIid = guidOf(iReferenceIidText)
    private val bindableIterableIid = guidOf(bindableIterableIidText)
    private val bindableIteratorIid = guidOf(bindableIteratorIidText)
    private val bindableVectorIid = guidOf(bindableVectorIidText)
    private val bindableVectorViewIid = guidOf(bindableVectorViewIidText)

    private val cache = IdentityHashMap<Any, MutableMap<String, ProjectedObjectHandle>>()

    actual fun createPointerOrNull(
        value: Any,
        projectionTypeKey: String,
        signature: String,
    ): ComPtr? {
        val parsedProjectionTypeKey = parseProjectionTypeKey(projectionTypeKey)
        val cacheKey = "$projectionTypeKey::$signature"
        return synchronized(cache) {
            val handles = cache.getOrPut(value) { linkedMapOf() }
            val cached = handles[cacheKey]
            if (cached != null) {
                return@synchronized cached.pointer
            }
            val created = createProjectedHandle(value, parsedProjectionTypeKey, signature) ?: return@synchronized null
            handles[cacheKey] = created
            created.pointer
        }
    }

    private fun createProjectedHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
        signature: String,
    ): ProjectedObjectHandle? {
        parseParameterizedInterfaceSignature(signature)?.let { parsedSignature ->
            return createProjectedHandle(value, projectionTypeKey, parsedSignature)
        }
        parseRawInterfaceSignature(signature)?.let { iid ->
            return createRawInterfaceHandle(value, projectionTypeKey, iid)
        }
        return null
    }

    private fun createProjectedHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
        signature: AbiValueSignature.ParameterizedInterface,
    ): ProjectedObjectHandle? {
        return when (signature.iid.canonical) {
            iterableIid.canonical -> createIterableHandle(value, projectionTypeKey, signature)
            iteratorIid.canonical -> createIteratorHandle(value, projectionTypeKey, signature)
            vectorViewIid.canonical -> createVectorViewHandle(value, projectionTypeKey, signature)
            mapViewIid.canonical -> createMapViewHandle(value, projectionTypeKey, signature)
            keyValuePairIid.canonical -> createKeyValuePairHandle(value, projectionTypeKey, signature)
            iReferenceIid.canonical -> createReferenceHandle(value, projectionTypeKey, signature)
            bindableIterableIid.canonical -> createBindableIterableHandle(value, projectionTypeKey)
            bindableIteratorIid.canonical -> createBindableIteratorHandle(value, projectionTypeKey)
            else -> null
        }
    }

    private fun createRawInterfaceHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
        iid: Guid,
    ): ProjectedObjectHandle? {
        return when (iid.canonical) {
            bindableIterableIid.canonical -> createBindableIterableHandle(value, projectionTypeKey)
            bindableIteratorIid.canonical -> createBindableIteratorHandle(value, projectionTypeKey)
            bindableVectorIid.canonical -> createBindableVectorHandle(value, projectionTypeKey)
            bindableVectorViewIid.canonical -> createBindableVectorViewHandle(value, projectionTypeKey)
            else -> null
        }
    }

    private fun createReferenceHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
        signature: AbiValueSignature.ParameterizedInterface,
    ): ProjectedObjectHandle? {
        val reference = value as? IReference<*> ?: return null
        val elementSignature = signature.arguments.singleOrNull() ?: return null
        val elementProjectionTypeKey = projectionTypeKey.arguments.singleOrNull()
            ?: inferProjectionTypeKey(elementSignature)
        val retainedChildren = mutableListOf<AutoCloseable>()
        val interfaceSpec = when {
            elementSignature is AbiValueSignature.StringType -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgHStringMethods = mapOf(
                    6 to {
                        reference.value as? String
                            ?: error("Expected IReference value to be String, got ${reference.value?.let { it::class.qualifiedName }}")
                    },
                ),
            )
            elementSignature is AbiValueSignature.ObjectType &&
                elementSignature.rawSignature == hResultStructSignature -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgInt32Methods = mapOf(
                    6 to {
                        when (val error = reference.value) {
                            null -> hResultOfException(null)
                            is Exception -> hResultOfException(error)
                            else -> error(
                                "Expected IReference<HResult> value to be Exception?, got ${error::class.qualifiedName}",
                            )
                        }
                    },
                ),
            )
            elementSignature is AbiValueSignature.ObjectType &&
                elementSignature.rawSignature.startsWith("struct(") -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgStructMethods = mapOf(
                    6 to { marshalStructAbiValue(requireNotNull(reference.value) { "IReference struct values cannot be null" }) },
                ),
            )
            elementSignature is AbiValueSignature.ObjectType &&
                elementSignature.rawSignature.startsWith("enum(") -> createEnumReferenceInterfaceSpec(
                signature = signature,
                reference = reference,
                elementSignature = elementSignature.rawSignature,
            )
            else -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgObjectMethods = mapOf(
                    6 to {
                        marshalObjectResultPointer(
                            value = reference.value,
                            projectionTypeKey = elementProjectionTypeKey,
                            signature = elementSignature,
                            retainedChildren = retainedChildren,
                        )
                    },
                ),
            )
        }
        val stub = JvmWinRtObjectStub.create(interfaceSpec)
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun createEnumReferenceInterfaceSpec(
        signature: AbiValueSignature.ParameterizedInterface,
        reference: IReference<*>,
        elementSignature: String,
    ): JvmWinRtObjectStub.InterfaceSpec {
        val value = requireNotNull(reference.value) { "IReference enum values cannot be null" }
        return when (enumUnderlyingSignature(elementSignature)) {
            "u4" -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUInt32Methods = mapOf(6 to { marshalEnumUInt32Value(value) }),
            )
            "i8" -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgInt64Methods = mapOf(6 to { marshalEnumInt64Value(value) }),
            )
            "u8" -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUInt64Methods = mapOf(6 to { marshalEnumUInt64Value(value) }),
            )
            else -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgInt32Methods = mapOf(6 to { marshalEnumInt32Value(value) }),
            )
        }
    }

    private fun createIterableHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
        signature: AbiValueSignature.ParameterizedInterface,
    ): ProjectedObjectHandle? {
        val iterable = value as? Iterable<*> ?: return null
        val elementSignature = signature.arguments.singleOrNull() ?: return null
        val elementProjectionTypeKey = projectionTypeKey.arguments.singleOrNull()
            ?: inferProjectionTypeKey(elementSignature)
        val retainedChildren = mutableListOf<AutoCloseable>()
        val iteratorSignature = AbiValueSignature.ParameterizedInterface(
            iid = iteratorIid,
            arguments = listOf(elementSignature),
            rawSignature = WinRtTypeSignature.parameterizedInterface(
                iteratorIidText,
                elementSignature.rawSignature,
            ),
        )
        val stub = JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgObjectMethods = mapOf(
                    6 to iterableFirstMethod(
                        iterable = iterable,
                        elementProjectionTypeKey = elementProjectionTypeKey,
                        iteratorSignature = iteratorSignature,
                        retainedChildren = retainedChildren,
                    ),
                ),
            ),
        )
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun createIteratorHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
        signature: AbiValueSignature.ParameterizedInterface,
    ): ProjectedObjectHandle? {
        val iterator = value as? Iterator<*> ?: return null
        val elementSignature = signature.arguments.singleOrNull() ?: return null
        val elementProjectionTypeKey = projectionTypeKey.arguments.singleOrNull()
            ?: inferProjectionTypeKey(elementSignature)
        val retainedChildren = mutableListOf<AutoCloseable>()
        val state = IteratorState(iterator)
        val interfaceSpec = when (elementSignature) {
            is AbiValueSignature.StringType -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgHStringMethods = mapOf(
                    6 to {
                        val current = state.currentValue ?: error("IIterator.Current was requested without a current value")
                        current as? String
                            ?: error("Expected iterator element to be String, got ${current::class.qualifiedName}")
                    },
                ),
                noArgBooleanMethods = mapOf(
                    7 to { state.hasCurrent },
                    8 to { state.moveNext() },
                ),
            )
            else -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgObjectMethods = mapOf(
                    6 to {
                        val current = state.currentValue ?: error("IIterator.Current was requested without a current value")
                        marshalObjectResultPointer(
                            value = current,
                            projectionTypeKey = elementProjectionTypeKey,
                            signature = elementSignature,
                            retainedChildren = retainedChildren,
                        )
                    },
                ),
                noArgBooleanMethods = mapOf(
                    7 to { state.hasCurrent },
                    8 to { state.moveNext() },
                ),
            )
        }
        val stub = JvmWinRtObjectStub.create(interfaceSpec)
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun createVectorViewHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
        signature: AbiValueSignature.ParameterizedInterface,
    ): ProjectedObjectHandle? {
        val list = value as? List<*> ?: return null
        val elementSignature = signature.arguments.singleOrNull() ?: return null
        val elementProjectionTypeKey = projectionTypeKey.arguments.singleOrNull()
            ?: inferProjectionTypeKey(elementSignature)
        val retainedChildren = mutableListOf<AutoCloseable>()
        val iteratorSignature = AbiValueSignature.ParameterizedInterface(
            iid = iteratorIid,
            arguments = listOf(elementSignature),
            rawSignature = WinRtTypeSignature.parameterizedInterface(
                iteratorIidText,
                elementSignature.rawSignature,
            ),
        )
        val iterableSignature = AbiValueSignature.ParameterizedInterface(
            iid = ParameterizedInterfaceId.createFromSignature(
                WinRtTypeSignature.parameterizedInterface(
                    iterableIidText,
                    elementSignature.rawSignature,
                ),
            ),
            arguments = listOf(elementSignature),
            rawSignature = WinRtTypeSignature.parameterizedInterface(
                iterableIidText,
                elementSignature.rawSignature,
            ),
        )
        val derivedSpec = when (elementSignature) {
            is AbiValueSignature.StringType -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgObjectMethods = mapOf(
                    6 to iterableFirstMethod(
                        iterable = list,
                        elementProjectionTypeKey = elementProjectionTypeKey,
                        iteratorSignature = iteratorSignature,
                        retainedChildren = retainedChildren,
                    ),
                ),
                uint32ArgHStringMethods = mapOf(
                    7 to { index ->
                        val element = list.elementAt(index.toInt())
                        element as? String
                            ?: error("Expected list element at $index to be String, got ${element?.let { it::class.qualifiedName }}")
                    },
                ),
                noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
            )
            else -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgObjectMethods = mapOf(
                    6 to iterableFirstMethod(
                        iterable = list,
                        elementProjectionTypeKey = elementProjectionTypeKey,
                        iteratorSignature = iteratorSignature,
                        retainedChildren = retainedChildren,
                    ),
                ),
                uint32ArgObjectMethods = mapOf(
                    7 to { index ->
                        marshalObjectResultPointer(
                            value = list.elementAt(index.toInt()),
                            projectionTypeKey = elementProjectionTypeKey,
                            signature = elementSignature,
                            retainedChildren = retainedChildren,
                        )
                    },
                ),
                noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
            )
        }
        val baseIterableSpec = JvmWinRtObjectStub.InterfaceSpec(
            iid = iterableSignature.iid,
            noArgObjectMethods = mapOf(
                6 to iterableFirstMethod(
                    iterable = list,
                    elementProjectionTypeKey = elementProjectionTypeKey,
                    iteratorSignature = iteratorSignature,
                    retainedChildren = retainedChildren,
                ),
            ),
        )
        val stub = JvmWinRtObjectStub.create(derivedSpec, baseIterableSpec)
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun createMapViewHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
        signature: AbiValueSignature.ParameterizedInterface,
    ): ProjectedObjectHandle? {
        val map = value as? Map<*, *> ?: return null
        val keySignature = signature.arguments.getOrNull(0) ?: return null
        val valueSignature = signature.arguments.getOrNull(1) ?: return null
        if (keySignature !is AbiValueSignature.StringType) {
            return null
        }
        val keyProjectionTypeKey = projectionTypeKey.arguments.getOrNull(0) ?: inferProjectionTypeKey(keySignature)
        val valueProjectionTypeKey = projectionTypeKey.arguments.getOrNull(1) ?: inferProjectionTypeKey(valueSignature)
        val keyValuePairSignature = AbiValueSignature.ParameterizedInterface(
            iid = keyValuePairIid,
            arguments = listOf(keySignature, valueSignature),
            rawSignature = WinRtTypeSignature.parameterizedInterface(
                keyValuePairIidText,
                keySignature.rawSignature,
                valueSignature.rawSignature,
            ),
        )
        val keyValuePairProjectionTypeKey = ProjectionTypeKey(
            rawType = "kotlin.collections.Map.Entry",
            arguments = listOf(keyProjectionTypeKey, valueProjectionTypeKey),
        )
        val iterableSignature = AbiValueSignature.ParameterizedInterface(
            iid = ParameterizedInterfaceId.createFromSignature(
                WinRtTypeSignature.parameterizedInterface(
                    iterableIidText,
                    keyValuePairSignature.rawSignature,
                ),
            ),
            arguments = listOf(keyValuePairSignature),
            rawSignature = WinRtTypeSignature.parameterizedInterface(
                iterableIidText,
                keyValuePairSignature.rawSignature,
            ),
        )
        val retainedChildren = mutableListOf<AutoCloseable>()
        val entries = map.entries
        val iteratorSignature = AbiValueSignature.ParameterizedInterface(
            iid = iteratorIid,
            arguments = listOf(keyValuePairSignature),
            rawSignature = WinRtTypeSignature.parameterizedInterface(
                iteratorIidText,
                keyValuePairSignature.rawSignature,
            ),
        )
        val firstMethod: () -> ComPtr = iterableFirstMethod(
            iterable = entries,
            elementProjectionTypeKey = keyValuePairProjectionTypeKey.render(),
            iteratorSignature = iteratorSignature,
            retainedChildren = retainedChildren,
        )
        val interfaceSpec = when (valueSignature) {
            is AbiValueSignature.StringType -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
                stringArgHStringMethods = mapOf(
                    7 to { key ->
                        if (!map.containsKey(key)) {
                            error("Map does not contain key '$key'")
                        }
                        val result = map[key]
                        result as? String
                            ?: error("Expected map value for '$key' to be String, got ${result?.let { it::class.qualifiedName }}")
                    },
                ),
                noArgUInt32Methods = mapOf(8 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(9 to { key -> map.containsKey(key) }),
            )
            else -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
                stringArgObjectMethods = mapOf(
                    7 to { key ->
                        if (!map.containsKey(key)) {
                            error("Map does not contain key '$key'")
                        }
                        marshalObjectResultPointer(
                            value = map[key],
                            projectionTypeKey = valueProjectionTypeKey,
                            signature = valueSignature,
                            retainedChildren = retainedChildren,
                        )
                    },
                ),
                noArgUInt32Methods = mapOf(8 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(9 to { key -> map.containsKey(key) }),
            )
        }
        val baseIterableSpec = JvmWinRtObjectStub.InterfaceSpec(
            iid = iterableSignature.iid,
            noArgObjectMethods = mapOf(6 to firstMethod),
        )
        val stub = JvmWinRtObjectStub.create(interfaceSpec, baseIterableSpec)
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun createKeyValuePairHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
        signature: AbiValueSignature.ParameterizedInterface,
    ): ProjectedObjectHandle? {
        val entry = value as? Map.Entry<*, *> ?: return null
        val keySignature = signature.arguments.getOrNull(0) ?: return null
        val valueSignature = signature.arguments.getOrNull(1) ?: return null
        val keyProjectionTypeKey = projectionTypeKey.arguments.getOrNull(0) ?: inferProjectionTypeKey(keySignature)
        val valueProjectionTypeKey = projectionTypeKey.arguments.getOrNull(1) ?: inferProjectionTypeKey(valueSignature)
        val retainedChildren = mutableListOf<AutoCloseable>()
        val interfaceSpec = JvmWinRtObjectStub.InterfaceSpec(
            iid = signature.iid,
            noArgObjectMethods = buildMap {
                if (keySignature !is AbiValueSignature.StringType) {
                    put(
                        6,
                        {
                            marshalObjectResultPointer(
                                value = entry.key,
                                projectionTypeKey = keyProjectionTypeKey,
                                signature = keySignature,
                                retainedChildren = retainedChildren,
                            )
                        },
                    )
                }
                if (valueSignature !is AbiValueSignature.StringType) {
                    put(
                        7,
                        {
                            marshalObjectResultPointer(
                                value = entry.value,
                                projectionTypeKey = valueProjectionTypeKey,
                                signature = valueSignature,
                                retainedChildren = retainedChildren,
                            )
                        },
                    )
                }
            },
            noArgHStringMethods = buildMap {
                if (keySignature is AbiValueSignature.StringType) {
                    put(
                        6,
                        {
                            entry.key as? String
                                ?: error("Expected key to be String, got ${entry.key?.let { it::class.qualifiedName }}")
                        },
                    )
                }
                if (valueSignature is AbiValueSignature.StringType) {
                    put(
                        7,
                        {
                            entry.value as? String
                                ?: error("Expected value to be String, got ${entry.value?.let { it::class.qualifiedName }}")
                        },
                    )
                }
            },
        )
        val stub = JvmWinRtObjectStub.create(interfaceSpec)
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun createBindableIterableHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
    ): ProjectedObjectHandle? {
        val iterable = value as? Iterable<*> ?: return null
        val elementProjectionTypeKey = projectionTypeKey.arguments.singleOrNull() ?: "Object"
        val retainedChildren = mutableListOf<AutoCloseable>()
        val stub = JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(
                iid = bindableIterableIid,
                noArgObjectMethods = mapOf(
                    6 to {
                        val iteratorHandle = requireNotNull(
                            createBindableIteratorHandle(
                                iterable.iterator(),
                                ProjectionTypeKey("kotlin.collections.Iterator", listOf(elementProjectionTypeKey)),
                            ),
                        )
                        retainedChildren += iteratorHandle
                        iteratorHandle.pointer.withAddRef()
                    },
                ),
            ),
        )
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun createBindableIteratorHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
    ): ProjectedObjectHandle? {
        val iterator = value as? Iterator<*> ?: return null
        val elementProjectionTypeKey = projectionTypeKey.arguments.singleOrNull() ?: "Object"
        val retainedChildren = mutableListOf<AutoCloseable>()
        val state = IteratorState(iterator)
        val stub = JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(
                iid = bindableIteratorIid,
                noArgObjectMethods = mapOf(
                    6 to {
                        val current = state.currentValue ?: error("IBindableIterator.Current was requested without a current value")
                        marshalObjectResultPointer(
                            value = current,
                            projectionTypeKey = elementProjectionTypeKey,
                            signature = AbiValueSignature.ObjectType(WinRtTypeSignature.object_()),
                            retainedChildren = retainedChildren,
                        )
                    },
                ),
                noArgBooleanMethods = mapOf(
                    7 to { state.hasCurrent },
                    8 to { state.moveNext() },
                ),
            ),
        )
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun createBindableVectorViewHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
    ): ProjectedObjectHandle? {
        val list = value as? List<*> ?: return null
        val elementProjectionTypeKey = projectionTypeKey.arguments.singleOrNull() ?: "Object"
        val retainedChildren = mutableListOf<AutoCloseable>()
        val firstMethod: () -> ComPtr = {
            val iteratorHandle = requireNotNull(
                createBindableIteratorHandle(
                    list.iterator(),
                    ProjectionTypeKey("kotlin.collections.Iterator", listOf(elementProjectionTypeKey)),
                ),
            )
            retainedChildren += iteratorHandle
            iteratorHandle.pointer.withAddRef()
        }
        val interfaceSpec = JvmWinRtObjectStub.InterfaceSpec(
            iid = bindableVectorViewIid,
            noArgObjectMethods = mapOf(6 to firstMethod),
            uint32ArgObjectMethods = mapOf(
                7 to { index ->
                    marshalObjectResultPointer(
                        value = list.elementAt(index.toInt()),
                        projectionTypeKey = elementProjectionTypeKey,
                        signature = AbiValueSignature.ObjectType(WinRtTypeSignature.object_()),
                        retainedChildren = retainedChildren,
                    )
                },
            ),
            noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
            objectUInt32ArgBooleanMethods = mapOf(
                9 to { pointer, _ -> bindableIndexOf(list, pointer) >= 0 },
            ),
        )
        val baseIterableSpec = JvmWinRtObjectStub.InterfaceSpec(
            iid = bindableIterableIid,
            noArgObjectMethods = mapOf(6 to firstMethod),
        )
        val stub = JvmWinRtObjectStub.create(interfaceSpec, baseIterableSpec)
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun createBindableVectorHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
    ): ProjectedObjectHandle? {
        @Suppress("UNCHECKED_CAST")
        val list = value as? MutableList<Any?> ?: return null
        val elementProjectionTypeKey = projectionTypeKey.arguments.singleOrNull() ?: "Object"
        val retainedChildren = mutableListOf<AutoCloseable>()
        val firstMethod: () -> ComPtr = {
            val iteratorHandle = requireNotNull(
                createBindableIteratorHandle(
                    list.iterator(),
                    ProjectionTypeKey("kotlin.collections.Iterator", listOf(elementProjectionTypeKey)),
                ),
            )
            retainedChildren += iteratorHandle
            iteratorHandle.pointer.withAddRef()
        }
        val getViewMethod: () -> ComPtr = {
            val vectorViewHandle = requireNotNull(
                createBindableVectorViewHandle(
                    list,
                    ProjectionTypeKey("kotlin.collections.List", listOf(elementProjectionTypeKey)),
                ),
            )
            retainedChildren += vectorViewHandle
            vectorViewHandle.pointer.withAddRef()
        }
        val interfaceSpec = JvmWinRtObjectStub.InterfaceSpec(
            iid = bindableVectorIid,
            noArgUnitMethods = mapOf(
                15 to {
                    if (list.isNotEmpty()) {
                        list.removeAt(list.lastIndex)
                    }
                    HResult(0)
                },
                16 to {
                    list.clear()
                    HResult(0)
                },
            ),
            noArgObjectMethods = mapOf(
                6 to firstMethod,
                9 to getViewMethod,
            ),
            noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
            uint32ArgUnitMethods = mapOf(
                13 to { index ->
                    list.removeAt(index.toInt())
                    HResult(0)
                },
            ),
            uint32ArgObjectMethods = mapOf(
                7 to { index ->
                    marshalObjectResultPointer(
                        value = list.elementAt(index.toInt()),
                        projectionTypeKey = elementProjectionTypeKey,
                        signature = AbiValueSignature.ObjectType(WinRtTypeSignature.object_()),
                        retainedChildren = retainedChildren,
                    )
                },
            ),
            uint32ObjectArgUnitMethods = mapOf(
                11 to { index, pointer ->
                    list[index.toInt()] = bindableValueFromPointer(pointer)
                    HResult(0)
                },
                12 to { index, pointer ->
                    list.add(index.toInt(), bindableValueFromPointer(pointer))
                    HResult(0)
                },
            ),
            objectUInt32ArgBooleanMethods = mapOf(
                10 to { pointer, _ -> bindableIndexOf(list, pointer) >= 0 },
            ),
            objectArgUnitMethods = mapOf(
                14 to { pointer ->
                    list.add(bindableValueFromPointer(pointer))
                    HResult(0)
                },
            ),
        )
        val baseIterableSpec = JvmWinRtObjectStub.InterfaceSpec(
            iid = bindableIterableIid,
            noArgObjectMethods = mapOf(6 to firstMethod),
        )
        val stub = JvmWinRtObjectStub.create(interfaceSpec, baseIterableSpec)
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun marshalObjectResultPointer(
        value: Any?,
        projectionTypeKey: String,
        signature: AbiValueSignature,
        retainedChildren: MutableList<AutoCloseable>,
    ): ComPtr {
        val pointer = when (signature) {
            is AbiValueSignature.ParameterizedInterface -> {
                if (value == null) {
                    ComPtr.NULL
                } else if (value is Inspectable) {
                    value.getObjectReferenceForProjectedType(
                        projectionTypeKey,
                        ParameterizedInterfaceId.createFromSignature(signature.rawSignature),
                    )
                } else {
                    val child = requireNotNull(
                        createProjectedHandle(
                            value,
                            parseProjectionTypeKey(projectionTypeKey),
                            signature,
                        ),
                    ) {
                        "Unsupported plain Kotlin value ${value::class.qualifiedName} for projected signature ${signature.rawSignature}"
                    }
                    retainedChildren += child
                    child.pointer
                }
            }
            is AbiValueSignature.ObjectType -> when (value) {
                null -> ComPtr.NULL
                is Inspectable -> value.getInspectableArgumentPointer()
                else -> error(
                    "Projected object values for $projectionTypeKey require an Inspectable value; " +
                        "got ${value::class.qualifiedName}",
                )
            }
            is AbiValueSignature.StringType -> error("String values must use the HSTRING result path")
        }
        return pointer.withAddRef()
    }

    private fun inferProjectionTypeKey(signature: AbiValueSignature): String {
        return when (signature) {
            is AbiValueSignature.StringType -> "String"
            is AbiValueSignature.ObjectType -> if (signature.rawSignature.startsWith("struct(")) {
                signature.rawSignature.removePrefix("struct(").substringBefore(';')
            } else if (signature.rawSignature.startsWith("enum(")) {
                signature.rawSignature.removePrefix("enum(").substringBefore(';')
            } else {
                "Object"
            }
            is AbiValueSignature.ParameterizedInterface -> when (signature.iid.canonical) {
                iterableIid.canonical,
                bindableIterableIid.canonical,
                -> "kotlin.collections.Iterable<${inferProjectionTypeKey(signature.arguments.single())}>"
                iteratorIid.canonical,
                bindableIteratorIid.canonical,
                -> "kotlin.collections.Iterator<${inferProjectionTypeKey(signature.arguments.single())}>"
                vectorViewIid.canonical -> "kotlin.collections.List<${inferProjectionTypeKey(signature.arguments.single())}>"
                mapViewIid.canonical -> "kotlin.collections.Map<${signature.arguments.joinToString(", ") { inferProjectionTypeKey(it) }}>"
                keyValuePairIid.canonical ->
                    "kotlin.collections.Map.Entry<${signature.arguments.joinToString(", ") { inferProjectionTypeKey(it) }}>"
                iReferenceIid.canonical -> "dev.winrt.core.IReference<${inferProjectionTypeKey(signature.arguments.single())}>"
                else -> "Object"
            }
        }
    }

    private fun marshalStructAbiValue(value: Any): ComStructValue {
        val toAbi = value::class.java.methods.firstOrNull { method ->
            method.name == "toAbi" && method.parameterCount == 0
        } ?: error("Projected struct values for ${value::class.qualifiedName} require a public toAbi() method")
        return toAbi.invoke(value) as? ComStructValue
            ?: error("Projected struct ${value::class.qualifiedName}.toAbi() must return ComStructValue")
    }

    private fun enumUnderlyingSignature(signature: String): String {
        return signature.removePrefix("enum(")
            .removeSuffix(")")
            .substringAfter(';', "i4")
    }

    private fun marshalEnumInt32Value(value: Any): Int = when (val rawValue = readProjectedEnumValue(value)) {
        is Int -> rawValue
        is UInt -> rawValue.toInt()
        is Short -> rawValue.toInt()
        is UShort -> rawValue.toInt()
        is Byte -> rawValue.toInt()
        is UByte -> rawValue.toInt()
        is Char -> rawValue.code
        is Long -> rawValue.toInt()
        is ULong -> rawValue.toLong().toInt()
        else -> (rawValue as? Number)?.toInt()
            ?: error("Unsupported projected enum backing value ${rawValue::class.qualifiedName}")
    }

    private fun marshalEnumUInt32Value(value: Any): UInt = when (val rawValue = readProjectedEnumValue(value)) {
        is UInt -> rawValue
        is Int -> rawValue.toUInt()
        is Short -> rawValue.toInt().toUInt()
        is UShort -> rawValue.toUInt()
        is Byte -> rawValue.toInt().toUInt()
        is UByte -> rawValue.toUInt()
        is Char -> rawValue.code.toUInt()
        is Long -> rawValue.toUInt()
        is ULong -> rawValue.toUInt()
        else -> (rawValue as? Number)?.toLong()?.toUInt()
            ?: error("Unsupported projected enum backing value ${rawValue::class.qualifiedName}")
    }

    private fun marshalEnumInt64Value(value: Any): Long = when (val rawValue = readProjectedEnumValue(value)) {
        is Long -> rawValue
        is ULong -> rawValue.toLong()
        is Int -> rawValue.toLong()
        is UInt -> rawValue.toLong()
        is Short -> rawValue.toLong()
        is UShort -> rawValue.toLong()
        is Byte -> rawValue.toLong()
        is UByte -> rawValue.toLong()
        is Char -> rawValue.code.toLong()
        else -> (rawValue as? Number)?.toLong()
            ?: error("Unsupported projected enum backing value ${rawValue::class.qualifiedName}")
    }

    private fun marshalEnumUInt64Value(value: Any): ULong = when (val rawValue = readProjectedEnumValue(value)) {
        is ULong -> rawValue
        is Long -> rawValue.toULong()
        is UInt -> rawValue.toULong()
        is Int -> rawValue.toUInt().toULong()
        is UShort -> rawValue.toULong()
        is Short -> rawValue.toUInt().toULong()
        is UByte -> rawValue.toULong()
        is Byte -> rawValue.toUInt().toULong()
        is Char -> rawValue.code.toULong()
        else -> (rawValue as? Number)?.toLong()?.toULong()
            ?: error("Unsupported projected enum backing value ${rawValue::class.qualifiedName}")
    }

    private fun readProjectedEnumValue(value: Any): Any {
        val getter = value::class.java.methods.firstOrNull { method ->
            method.parameterCount == 0 && (method.name == "getValue" || method.name.startsWith("getValue-"))
        }
        if (getter != null) {
            return getter.invoke(value)
                ?: error("Projected enum value getter for ${value::class.qualifiedName} returned null")
        }

        val field = value::class.java.fields.firstOrNull { it.name == "value" }
            ?: error("Projected enum values for ${value::class.qualifiedName} require a public val value")
        return field.get(value)
            ?: error("Projected enum value field for ${value::class.qualifiedName} returned null")
    }

    private fun parseParameterizedInterfaceSignature(signature: String): AbiValueSignature.ParameterizedInterface? {
        if (!signature.startsWith("pinterface(") || !signature.endsWith(")")) {
            return null
        }
        val content = signature.removePrefix("pinterface(").removeSuffix(")")
        val parts = splitTopLevel(content, ';')
        if (parts.isEmpty()) {
            return null
        }
        val iid = guidOf(parts.first().removePrefix("{").removeSuffix("}"))
        return AbiValueSignature.ParameterizedInterface(
            iid = iid,
            arguments = parts.drop(1).map(::parseAbiValueSignature),
            rawSignature = signature,
        )
    }

    private fun parseRawInterfaceSignature(signature: String): Guid? {
        if (!signature.startsWith("{") || !signature.endsWith("}")) {
            return null
        }
        return guidOf(signature.removePrefix("{").removeSuffix("}"))
    }

    private fun parseAbiValueSignature(signature: String): AbiValueSignature {
        return when {
            signature == "string" -> AbiValueSignature.StringType(signature)
            signature.startsWith("pinterface(") -> parseParameterizedInterfaceSignature(signature)
                ?: AbiValueSignature.ObjectType(signature)
            else -> AbiValueSignature.ObjectType(signature)
        }
    }

    private fun parseProjectionTypeKey(projectionTypeKey: String): ProjectionTypeKey {
        val rawType = projectionTypeKey.substringBefore('<').trim()
        val argumentSource = projectionTypeKey.substringAfter('<', "").substringBeforeLast('>', "")
        if (argumentSource.isBlank()) {
            return ProjectionTypeKey(rawType, emptyList())
        }
        return ProjectionTypeKey(rawType, splitTopLevel(argumentSource, ',').map(String::trim))
    }

    private fun splitTopLevel(source: String, separator: Char): List<String> {
        if (source.isBlank()) {
            return emptyList()
        }
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var parenthesisDepth = 0
        var angleDepth = 0
        source.forEach { character ->
            when (character) {
                '(' -> parenthesisDepth += 1
                ')' -> parenthesisDepth -= 1
                '<' -> angleDepth += 1
                '>' -> angleDepth -= 1
            }
            if (character == separator && parenthesisDepth == 0 && angleDepth == 0) {
                parts += current.toString()
                current.setLength(0)
            } else {
                current.append(character)
            }
        }
        parts += current.toString()
        return parts
    }

    private fun iterableFirstMethod(
        iterable: Iterable<*>,
        elementProjectionTypeKey: String,
        iteratorSignature: AbiValueSignature.ParameterizedInterface,
        retainedChildren: MutableList<AutoCloseable>,
    ): () -> ComPtr {
        return {
            val iteratorHandle = requireNotNull(
                createIteratorHandle(
                    iterable.iterator(),
                    ProjectionTypeKey("kotlin.collections.Iterator", listOf(elementProjectionTypeKey)),
                    iteratorSignature,
                ),
            )
            retainedChildren += iteratorHandle
            iteratorHandle.pointer.withAddRef()
        }
    }

    private fun bindableIndexOf(list: List<*>, pointer: ComPtr): Int {
        return list.indexOfFirst { element ->
            when (element) {
                null -> pointer.isNull
                is Inspectable -> element.pointer == pointer
                else -> false
            }
        }
    }

    private fun bindableValueFromPointer(pointer: ComPtr): Inspectable = Inspectable(pointer)

    private class IteratorState(
        private val iterator: Iterator<*>,
    ) {
        var currentValue: Any? = null
            private set

        var hasCurrent: Boolean = false
            private set

        init {
            advanceInitial()
        }

        fun moveNext(): Boolean {
            if (iterator.hasNext()) {
                currentValue = iterator.next()
                hasCurrent = true
                return true
            }
            currentValue = null
            hasCurrent = false
            return false
        }

        private fun advanceInitial() {
            if (iterator.hasNext()) {
                currentValue = iterator.next()
                hasCurrent = true
            }
        }
    }

    private data class ProjectionTypeKey(
        val rawType: String,
        val arguments: List<String>,
    ) {
        fun render(): String {
            return if (arguments.isEmpty()) {
                rawType
            } else {
                "$rawType<${arguments.joinToString(", ")}>"
            }
        }
    }

    private sealed class AbiValueSignature(
        open val rawSignature: String,
    ) {
        data class StringType(
            override val rawSignature: String,
        ) : AbiValueSignature(rawSignature)

        data class ObjectType(
            override val rawSignature: String,
        ) : AbiValueSignature(rawSignature)

        data class ParameterizedInterface(
            val iid: Guid,
            val arguments: List<AbiValueSignature>,
            override val rawSignature: String,
        ) : AbiValueSignature(rawSignature)
    }
    private class ProjectedObjectHandle(
        private val stub: JvmWinRtObjectStub,
        retainedChildren: List<AutoCloseable>,
    ) : AutoCloseable {
        private val retainedChildren: MutableList<AutoCloseable> = retainedChildren.toMutableList()

        val pointer: ComPtr
            get() = stub.primaryPointer

        override fun close() {
            retainedChildren.asReversed().forEach(AutoCloseable::close)
            retainedChildren.clear()
            stub.close()
        }
    }

    private fun ComPtr.withAddRef(): ComPtr {
        if (!isNull) {
            PlatformComInterop.addRef(this)
        }
        return this
    }
}

private val Guid.canonical: String
    get() = toString()
