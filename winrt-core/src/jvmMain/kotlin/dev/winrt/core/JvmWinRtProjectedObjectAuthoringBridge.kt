package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComStructValue
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import java.util.IdentityHashMap

internal actual object WinRtProjectedObjectAuthoringBridge {
    private val iterableIidText = "faa585ea-6214-4217-afda-7f46de5869b3"
    private val iteratorIidText = "6a79e863-4300-459a-9966-cbb660963ee1"
    private val vectorIidText = "913337e9-11a1-4345-a3a2-4e7f956e222d"
    private val vectorViewIidText = "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56"
    private val mapIidText = "3c2925fe-8519-45c1-aa79-197b6718c1c1"
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
    private val vectorIid = guidOf(vectorIidText)
    private val vectorViewIid = guidOf(vectorViewIidText)
    private val mapIid = guidOf(mapIidText)
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
            vectorIid.canonical -> createVectorHandle(value, projectionTypeKey, signature)
            vectorViewIid.canonical -> createVectorViewHandle(value, projectionTypeKey, signature)
            mapIid.canonical -> createMapHandle(value, projectionTypeKey, signature)
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
        val primitiveKind = primitiveAbiKind(elementSignature)
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
            primitiveKind != null -> createPrimitiveReferenceInterfaceSpec(signature, reference, primitiveKind)
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
        val primitiveKind = primitiveAbiKind(elementSignature)
        val interfaceSpec = when {
            elementSignature is AbiValueSignature.StringType -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgHStringMethods = mapOf(
                    6 to {
                        val current = state.requireCurrentValue("IIterator")
                        current as? String
                            ?: error("Expected iterator element to be String, got ${current?.let { it::class.qualifiedName }}")
                    },
                ),
                noArgBooleanMethods = mapOf(
                    7 to { state.hasCurrent },
                    8 to { state.moveNext() },
                ),
            )
            primitiveKind != null -> createPrimitiveIteratorInterfaceSpec(signature, state, primitiveKind)
            else -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgObjectMethods = mapOf(
                    6 to {
                        marshalObjectResultPointer(
                            value = state.requireCurrentValue("IIterator"),
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
        val firstMethod = iterableFirstMethod(
            iterable = list,
            elementProjectionTypeKey = elementProjectionTypeKey,
            iteratorSignature = iteratorSignature,
            retainedChildren = retainedChildren,
        )
        val primitiveKind = primitiveAbiKind(elementSignature)
        val derivedSpec = when {
            elementSignature is AbiValueSignature.StringType -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
                uint32ArgHStringMethods = mapOf(
                    7 to { index ->
                        val element = list.elementAt(index.toInt())
                        element as? String
                            ?: error("Expected list element at $index to be String, got ${element?.let { it::class.qualifiedName }}")
                    },
                ),
                noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
            )
            primitiveKind != null -> createPrimitiveVectorViewInterfaceSpec(
                iid = signature.iid,
                list = list,
                firstMethod = firstMethod,
                primitiveKind = primitiveKind,
            )
            else -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
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
            noArgObjectMethods = mapOf(6 to firstMethod),
        )
        val stub = JvmWinRtObjectStub.create(derivedSpec, baseIterableSpec)
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun createVectorHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
        signature: AbiValueSignature.ParameterizedInterface,
    ): ProjectedObjectHandle? {
        @Suppress("UNCHECKED_CAST")
        val list = value as? MutableList<Any?> ?: return null
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
        val vectorViewSignature = AbiValueSignature.ParameterizedInterface(
            iid = ParameterizedInterfaceId.createFromSignature(
                WinRtTypeSignature.parameterizedInterface(
                    vectorViewIidText,
                    elementSignature.rawSignature,
                ),
            ),
            arguments = listOf(elementSignature),
            rawSignature = WinRtTypeSignature.parameterizedInterface(
                vectorViewIidText,
                elementSignature.rawSignature,
            ),
        )
        val firstMethod: () -> ComPtr = iterableFirstMethod(
            iterable = list,
            elementProjectionTypeKey = elementProjectionTypeKey,
            iteratorSignature = iteratorSignature,
            retainedChildren = retainedChildren,
        )
        val getViewMethod: () -> ComPtr = {
            val vectorViewHandle = requireNotNull(
                createVectorViewHandle(
                    list,
                    ProjectionTypeKey("kotlin.collections.List", listOf(elementProjectionTypeKey)),
                    vectorViewSignature,
                ),
            )
            retainedChildren += vectorViewHandle
            vectorViewHandle.pointer.withAddRef()
        }
        val primitiveKind = primitiveAbiKind(elementSignature)
        val interfaceSpec = when {
            elementSignature is AbiValueSignature.StringType -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUnitMethods = mapOf(
                    15 to {
                        if (list.isEmpty()) {
                            KnownHResults.E_BOUNDS
                        } else {
                            list.removeAt(list.lastIndex)
                            HResult(0)
                        }
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
                uint32StringArgUnitMethods = mapOf(
                    11 to { index, element ->
                        list[index.toInt()] = element
                        HResult(0)
                    },
                    12 to { index, element ->
                        list.add(index.toInt(), element)
                        HResult(0)
                    },
                ),
                uint32ArgHStringMethods = mapOf(
                    7 to { index ->
                        list.elementAt(index.toInt()) as? String
                            ?: error(
                                "Expected mutable list element at $index to be String, got " +
                                    "${list.elementAt(index.toInt())?.let { it::class.qualifiedName }}",
                            )
                    },
                ),
                stringArgUnitMethods = mapOf(
                    14 to { element ->
                        list.add(element)
                        HResult(0)
                    },
                ),
                stringUInt32ArgBooleanMethods = mapOf(
                    10 to { element, _ -> list.indexOf(element) >= 0 },
                ),
            )
            primitiveKind != null -> createPrimitiveVectorInterfaceSpec(
                iid = signature.iid,
                list = list,
                firstMethod = firstMethod,
                getViewMethod = getViewMethod,
                primitiveKind = primitiveKind,
                elementProjectionTypeKey = elementProjectionTypeKey,
            )
            else -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUnitMethods = mapOf(
                    15 to {
                        if (list.isEmpty()) {
                            KnownHResults.E_BOUNDS
                        } else {
                            list.removeAt(list.lastIndex)
                            HResult(0)
                        }
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
                            signature = elementSignature,
                            retainedChildren = retainedChildren,
                        )
                    },
                ),
                uint32ObjectArgUnitMethods = mapOf(
                    11 to { index, pointer ->
                        list[index.toInt()] = projectObjectValueFromPointer(pointer, elementProjectionTypeKey, elementSignature)
                        HResult(0)
                    },
                    12 to { index, pointer ->
                        list.add(index.toInt(), projectObjectValueFromPointer(pointer, elementProjectionTypeKey, elementSignature))
                        HResult(0)
                    },
                ),
                objectUInt32ArgBooleanMethods = mapOf(
                    10 to { pointer, _ -> inspectableIndexOf(list, pointer) >= 0 },
                ),
                objectArgUnitMethods = mapOf(
                    14 to { pointer ->
                        list.add(projectObjectValueFromPointer(pointer, elementProjectionTypeKey, elementSignature))
                        HResult(0)
                    },
                ),
            )
        }
        val vectorViewSpec = when {
            elementSignature is AbiValueSignature.StringType -> JvmWinRtObjectStub.InterfaceSpec(
                iid = vectorViewSignature.iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
                uint32ArgHStringMethods = mapOf(
                    7 to { index ->
                        list.elementAt(index.toInt()) as? String
                            ?: error(
                                "Expected mutable list element at $index to be String, got " +
                                    "${list.elementAt(index.toInt())?.let { it::class.qualifiedName }}",
                            )
                    },
                ),
                noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
                stringUInt32ArgBooleanMethods = mapOf(
                    9 to { element, _ -> list.indexOf(element) >= 0 },
                ),
            )
            primitiveKind != null -> createPrimitiveVectorViewInterfaceSpec(
                iid = vectorViewSignature.iid,
                list = list,
                firstMethod = firstMethod,
                primitiveKind = primitiveKind,
            )
            else -> JvmWinRtObjectStub.InterfaceSpec(
                iid = vectorViewSignature.iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
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
                objectUInt32ArgBooleanMethods = mapOf(
                    9 to { pointer, _ -> inspectableIndexOf(list, pointer) >= 0 },
                ),
            )
        }
        val baseIterableSpec = JvmWinRtObjectStub.InterfaceSpec(
            iid = iterableSignature.iid,
            noArgObjectMethods = mapOf(6 to firstMethod),
        )
        val stub = JvmWinRtObjectStub.create(interfaceSpec, vectorViewSpec, baseIterableSpec)
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun createPrimitiveReferenceInterfaceSpec(
        signature: AbiValueSignature.ParameterizedInterface,
        reference: IReference<*>,
        primitiveKind: PrimitiveAbiKind,
    ): JvmWinRtObjectStub.InterfaceSpec {
        val value = requireNotNull(reference.value) { "IReference primitive values cannot be null" }
        return when (primitiveKind) {
            PrimitiveAbiKind.BOOLEAN -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgBooleanMethods = mapOf(6 to { marshalPrimitiveBooleanValue(value) }),
            )
            PrimitiveAbiKind.INT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgInt32Methods = mapOf(6 to { marshalPrimitiveInt32Value(value) }),
            )
            PrimitiveAbiKind.UINT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUInt32Methods = mapOf(6 to { marshalPrimitiveUInt32Value(value) }),
            )
            PrimitiveAbiKind.INT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgInt64Methods = mapOf(6 to { marshalPrimitiveInt64Value(value) }),
            )
            PrimitiveAbiKind.UINT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUInt64Methods = mapOf(6 to { marshalPrimitiveUInt64Value(value) }),
            )
            PrimitiveAbiKind.FLOAT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgFloat32Methods = mapOf(6 to { marshalPrimitiveFloat32Value(value) }),
            )
            PrimitiveAbiKind.FLOAT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgFloat64Methods = mapOf(6 to { marshalPrimitiveFloat64Value(value) }),
            )
        }
    }

    private fun createPrimitiveIteratorInterfaceSpec(
        signature: AbiValueSignature.ParameterizedInterface,
        state: IteratorState,
        primitiveKind: PrimitiveAbiKind,
    ): JvmWinRtObjectStub.InterfaceSpec {
        val stateMethods = mapOf(
            7 to { state.hasCurrent },
            8 to { state.moveNext() },
        )
        return when (primitiveKind) {
            PrimitiveAbiKind.BOOLEAN -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgBooleanMethods = stateMethods + mapOf(
                    6 to { marshalPrimitiveBooleanValue(currentIteratorValue(state)) },
                ),
            )
            PrimitiveAbiKind.INT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgInt32Methods = mapOf(6 to { marshalPrimitiveInt32Value(currentIteratorValue(state)) }),
                noArgBooleanMethods = stateMethods,
            )
            PrimitiveAbiKind.UINT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUInt32Methods = mapOf(6 to { marshalPrimitiveUInt32Value(currentIteratorValue(state)) }),
                noArgBooleanMethods = stateMethods,
            )
            PrimitiveAbiKind.INT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgInt64Methods = mapOf(6 to { marshalPrimitiveInt64Value(currentIteratorValue(state)) }),
                noArgBooleanMethods = stateMethods,
            )
            PrimitiveAbiKind.UINT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUInt64Methods = mapOf(6 to { marshalPrimitiveUInt64Value(currentIteratorValue(state)) }),
                noArgBooleanMethods = stateMethods,
            )
            PrimitiveAbiKind.FLOAT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgFloat32Methods = mapOf(6 to { marshalPrimitiveFloat32Value(currentIteratorValue(state)) }),
                noArgBooleanMethods = stateMethods,
            )
            PrimitiveAbiKind.FLOAT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgFloat64Methods = mapOf(6 to { marshalPrimitiveFloat64Value(currentIteratorValue(state)) }),
                noArgBooleanMethods = stateMethods,
            )
        }
    }

    private fun createPrimitiveVectorViewInterfaceSpec(
        iid: Guid,
        list: List<*>,
        firstMethod: () -> ComPtr,
        primitiveKind: PrimitiveAbiKind,
    ): JvmWinRtObjectStub.InterfaceSpec {
        return when (primitiveKind) {
            PrimitiveAbiKind.BOOLEAN -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
                uint32ArgBooleanMethods = mapOf(7 to { index -> marshalPrimitiveBooleanValue(list.elementAt(index.toInt())) }),
                noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
                booleanUInt32ArgBooleanMethods = mapOf(
                    9 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
            )
            PrimitiveAbiKind.INT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
                uint32ArgInt32Methods = mapOf(7 to { index -> marshalPrimitiveInt32Value(list.elementAt(index.toInt())) }),
                noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
                int32UInt32ArgBooleanMethods = mapOf(
                    9 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
            )
            PrimitiveAbiKind.UINT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
                uint32ArgUInt32Methods = mapOf(7 to { index -> marshalPrimitiveUInt32Value(list.elementAt(index.toInt())) }),
                noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
                uint32UInt32ArgBooleanMethods = mapOf(
                    9 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
            )
            PrimitiveAbiKind.INT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
                uint32ArgInt64Methods = mapOf(7 to { index -> marshalPrimitiveInt64Value(list.elementAt(index.toInt())) }),
                noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
                int64UInt32ArgBooleanMethods = mapOf(
                    9 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
            )
            PrimitiveAbiKind.UINT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
                uint32ArgUInt64Methods = mapOf(7 to { index -> marshalPrimitiveUInt64Value(list.elementAt(index.toInt())) }),
                noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
                uint64UInt32ArgBooleanMethods = mapOf(
                    9 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
            )
            PrimitiveAbiKind.FLOAT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
                uint32ArgFloat32Methods = mapOf(7 to { index -> marshalPrimitiveFloat32Value(list.elementAt(index.toInt())) }),
                noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
                float32UInt32ArgBooleanMethods = mapOf(
                    9 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
            )
            PrimitiveAbiKind.FLOAT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgObjectMethods = mapOf(6 to firstMethod),
                uint32ArgFloat64Methods = mapOf(7 to { index -> marshalPrimitiveFloat64Value(list.elementAt(index.toInt())) }),
                noArgUInt32Methods = mapOf(8 to { list.size.toUInt() }),
                float64UInt32ArgBooleanMethods = mapOf(
                    9 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
            )
        }
    }

    private fun createPrimitiveVectorInterfaceSpec(
        iid: Guid,
        list: MutableList<Any?>,
        firstMethod: () -> ComPtr,
        getViewMethod: () -> ComPtr,
        primitiveKind: PrimitiveAbiKind,
        elementProjectionTypeKey: String,
    ): JvmWinRtObjectStub.InterfaceSpec {
        val noArgUnitMethods = mapOf(
            15 to {
                if (list.isEmpty()) {
                    KnownHResults.E_BOUNDS
                } else {
                    list.removeAt(list.lastIndex)
                    HResult(0)
                }
            },
            16 to {
                list.clear()
                HResult(0)
            },
        )
        val noArgObjectMethods = mapOf(
            6 to firstMethod,
            9 to getViewMethod,
        )
        val noArgUInt32Methods = mapOf(8 to { list.size.toUInt() })
        val removeAtMethods = mapOf(
            13 to { index: UInt ->
                list.removeAt(index.toInt())
                HResult(0)
            },
        )
        return when (primitiveKind) {
            PrimitiveAbiKind.BOOLEAN -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgUnitMethods = noArgUnitMethods,
                noArgObjectMethods = noArgObjectMethods,
                noArgUInt32Methods = noArgUInt32Methods,
                uint32ArgUnitMethods = removeAtMethods,
                uint32ArgBooleanMethods = mapOf(7 to { index -> marshalPrimitiveBooleanValue(list.elementAt(index.toInt())) }),
                uint32BooleanArgUnitMethods = mapOf(
                    11 to { index, element ->
                        list[index.toInt()] = projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element)
                        HResult(0)
                    },
                    12 to { index, element ->
                        list.add(index.toInt(), projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
                booleanUInt32ArgBooleanMethods = mapOf(
                    10 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
                booleanArgUnitMethods = mapOf(
                    14 to { element ->
                        list.add(projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
            )
            PrimitiveAbiKind.INT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgUnitMethods = noArgUnitMethods,
                noArgObjectMethods = noArgObjectMethods,
                noArgUInt32Methods = noArgUInt32Methods,
                uint32ArgUnitMethods = removeAtMethods,
                uint32ArgInt32Methods = mapOf(7 to { index -> marshalPrimitiveInt32Value(list.elementAt(index.toInt())) }),
                uint32Int32ArgUnitMethods = mapOf(
                    11 to { index, element ->
                        list[index.toInt()] = projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element)
                        HResult(0)
                    },
                    12 to { index, element ->
                        list.add(index.toInt(), projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
                int32UInt32ArgBooleanMethods = mapOf(
                    10 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
                int32ArgUnitMethods = mapOf(
                    14 to { element ->
                        list.add(projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
            )
            PrimitiveAbiKind.UINT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgUnitMethods = noArgUnitMethods,
                noArgObjectMethods = noArgObjectMethods,
                noArgUInt32Methods = noArgUInt32Methods,
                uint32ArgUnitMethods = removeAtMethods + mapOf(
                    14 to { element ->
                        list.add(projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
                uint32ArgUInt32Methods = mapOf(7 to { index -> marshalPrimitiveUInt32Value(list.elementAt(index.toInt())) }),
                uint32UInt32ArgUnitMethods = mapOf(
                    11 to { index, element ->
                        list[index.toInt()] = projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element)
                        HResult(0)
                    },
                    12 to { index, element ->
                        list.add(index.toInt(), projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
                uint32UInt32ArgBooleanMethods = mapOf(
                    10 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
            )
            PrimitiveAbiKind.INT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgUnitMethods = noArgUnitMethods,
                noArgObjectMethods = noArgObjectMethods,
                noArgUInt32Methods = noArgUInt32Methods,
                uint32ArgUnitMethods = removeAtMethods,
                uint32ArgInt64Methods = mapOf(7 to { index -> marshalPrimitiveInt64Value(list.elementAt(index.toInt())) }),
                uint32Int64ArgUnitMethods = mapOf(
                    11 to { index, element ->
                        list[index.toInt()] = projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element)
                        HResult(0)
                    },
                    12 to { index, element ->
                        list.add(index.toInt(), projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
                int64UInt32ArgBooleanMethods = mapOf(
                    10 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
                int64ArgUnitMethods = mapOf(
                    14 to { element ->
                        list.add(projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
            )
            PrimitiveAbiKind.UINT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgUnitMethods = noArgUnitMethods,
                noArgObjectMethods = noArgObjectMethods,
                noArgUInt32Methods = noArgUInt32Methods,
                uint32ArgUnitMethods = removeAtMethods,
                uint32ArgUInt64Methods = mapOf(7 to { index -> marshalPrimitiveUInt64Value(list.elementAt(index.toInt())) }),
                uint32UInt64ArgUnitMethods = mapOf(
                    11 to { index, element ->
                        list[index.toInt()] = projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element)
                        HResult(0)
                    },
                    12 to { index, element ->
                        list.add(index.toInt(), projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
                uint64UInt32ArgBooleanMethods = mapOf(
                    10 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
                uint64ArgUnitMethods = mapOf(
                    14 to { element ->
                        list.add(projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
            )
            PrimitiveAbiKind.FLOAT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgUnitMethods = noArgUnitMethods,
                noArgObjectMethods = noArgObjectMethods,
                noArgUInt32Methods = noArgUInt32Methods,
                uint32ArgUnitMethods = removeAtMethods,
                uint32ArgFloat32Methods = mapOf(7 to { index -> marshalPrimitiveFloat32Value(list.elementAt(index.toInt())) }),
                uint32Float32ArgUnitMethods = mapOf(
                    11 to { index, element ->
                        list[index.toInt()] = projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element)
                        HResult(0)
                    },
                    12 to { index, element ->
                        list.add(index.toInt(), projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
                float32UInt32ArgBooleanMethods = mapOf(
                    10 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
                float32ArgUnitMethods = mapOf(
                    14 to { element ->
                        list.add(projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
            )
            PrimitiveAbiKind.FLOAT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = iid,
                noArgUnitMethods = noArgUnitMethods,
                noArgObjectMethods = noArgObjectMethods,
                noArgUInt32Methods = noArgUInt32Methods,
                uint32ArgUnitMethods = removeAtMethods,
                uint32ArgFloat64Methods = mapOf(7 to { index -> marshalPrimitiveFloat64Value(list.elementAt(index.toInt())) }),
                uint32Float64ArgUnitMethods = mapOf(
                    11 to { index, element ->
                        list[index.toInt()] = projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element)
                        HResult(0)
                    },
                    12 to { index, element ->
                        list.add(index.toInt(), projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
                float64UInt32ArgBooleanMethods = mapOf(
                    10 to { element, _ -> primitiveIndexOf(list, primitiveKind, element) >= 0 },
                ),
                float64ArgUnitMethods = mapOf(
                    14 to { element ->
                        list.add(projectPrimitiveValueFromAbi(primitiveKind, elementProjectionTypeKey, element))
                        HResult(0)
                    },
                ),
            )
        }
    }

    private fun createMapViewHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
        signature: AbiValueSignature.ParameterizedInterface,
    ): ProjectedObjectHandle? {
        @Suppress("UNCHECKED_CAST")
        val map = value as? Map<String, Any?> ?: return null
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
        val primitiveKind = primitiveAbiKind(valueSignature)
        val lookupValue: (String) -> Any? = { key ->
            if (!map.containsKey(key)) {
                throw IndexOutOfBoundsException("Map does not contain key '$key'")
            }
            map[key]
        }
        val splitMethod = mapViewSplitMethod(
            map = map,
            projectionTypeKey = projectionTypeKey,
            signature = signature,
            retainedChildren = retainedChildren,
        )
        val interfaceSpec = when {
            valueSignature is AbiValueSignature.StringType -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                stringArgHStringMethods = mapOf(
                    6 to {
                        val result = lookupValue(it)
                        result as? String
                            ?: error("Expected map value for '$it' to be String, got ${result?.let { value -> value::class.qualifiedName }}")
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            primitiveKind == PrimitiveAbiKind.BOOLEAN -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(
                    6 to { key -> marshalPrimitiveBooleanValue(lookupValue(key)) },
                    8 to { key -> map.containsKey(key) },
                ),
            )
            primitiveKind == PrimitiveAbiKind.INT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                stringArgInt32Methods = mapOf(6 to { key -> marshalPrimitiveInt32Value(lookupValue(key)) }),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            primitiveKind == PrimitiveAbiKind.UINT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                stringArgUInt32Methods = mapOf(6 to { key -> marshalPrimitiveUInt32Value(lookupValue(key)) }),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            primitiveKind == PrimitiveAbiKind.INT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                stringArgInt64Methods = mapOf(6 to { key -> marshalPrimitiveInt64Value(lookupValue(key)) }),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            primitiveKind == PrimitiveAbiKind.UINT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                stringArgUInt64Methods = mapOf(6 to { key -> marshalPrimitiveUInt64Value(lookupValue(key)) }),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            primitiveKind == PrimitiveAbiKind.FLOAT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                stringArgFloat32Methods = mapOf(6 to { key -> marshalPrimitiveFloat32Value(lookupValue(key)) }),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            primitiveKind == PrimitiveAbiKind.FLOAT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                stringArgFloat64Methods = mapOf(6 to { key -> marshalPrimitiveFloat64Value(lookupValue(key)) }),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            else -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                stringArgObjectMethods = mapOf(
                    6 to { key ->
                        marshalObjectResultPointer(
                            value = lookupValue(key),
                            projectionTypeKey = valueProjectionTypeKey,
                            signature = valueSignature,
                            retainedChildren = retainedChildren,
                        )
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
        }.copy(noArgTwoObjectMethods = mapOf(9 to splitMethod))
        val baseIterableSpec = JvmWinRtObjectStub.InterfaceSpec(
            iid = iterableSignature.iid,
            noArgObjectMethods = mapOf(6 to firstMethod),
        )
        val stub = JvmWinRtObjectStub.create(interfaceSpec, baseIterableSpec)
        return ProjectedObjectHandle(stub, retainedChildren)
    }

    private fun createMapHandle(
        value: Any,
        projectionTypeKey: ProjectionTypeKey,
        signature: AbiValueSignature.ParameterizedInterface,
    ): ProjectedObjectHandle? {
        @Suppress("UNCHECKED_CAST")
        val map = value as? MutableMap<String, Any?> ?: return null
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
        val mapViewSignature = AbiValueSignature.ParameterizedInterface(
            iid = ParameterizedInterfaceId.createFromSignature(
                WinRtTypeSignature.parameterizedInterface(
                    mapViewIidText,
                    keySignature.rawSignature,
                    valueSignature.rawSignature,
                ),
            ),
            arguments = listOf(keySignature, valueSignature),
            rawSignature = WinRtTypeSignature.parameterizedInterface(
                mapViewIidText,
                keySignature.rawSignature,
                valueSignature.rawSignature,
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
        val primitiveKind = primitiveAbiKind(valueSignature)
        val lookupValue: (String) -> Any? = { key ->
            if (!map.containsKey(key)) {
                throw IndexOutOfBoundsException("Map does not contain key '$key'")
            }
            map[key]
        }
        val getViewMethod: () -> ComPtr = {
            val viewHandle = requireNotNull(
                createMapViewHandle(
                    map,
                    ProjectionTypeKey("kotlin.collections.Map", listOf(keyProjectionTypeKey, valueProjectionTypeKey)),
                    mapViewSignature,
                ),
            )
            retainedChildren += viewHandle
            viewHandle.pointer.withAddRef()
        }
        val removeMethod: (String) -> HResult = { key -> removeStringKeyedMapEntry(map, key) }
        val splitMethod = mapViewSplitMethod(
            map = map,
            projectionTypeKey = ProjectionTypeKey("kotlin.collections.Map", listOf(keyProjectionTypeKey, valueProjectionTypeKey)),
            signature = mapViewSignature,
            retainedChildren = retainedChildren,
        )
        val interfaceSpec = when {
            valueSignature is AbiValueSignature.StringType -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUnitMethods = mapOf(
                    12 to {
                        map.clear()
                        HResult(0)
                    },
                ),
                noArgObjectMethods = mapOf(
                    9 to getViewMethod,
                ),
                stringArgUnitMethods = mapOf(11 to removeMethod),
                stringArgHStringMethods = mapOf(
                    6 to {
                        val result = lookupValue(it)
                        result as? String
                            ?: error("Expected map value for '$it' to be String, got ${result?.let { value -> value::class.qualifiedName }}")
                    },
                ),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
                stringStringArgBooleanMethods = mapOf(
                    10 to { key, element ->
                        val replaced = map.containsKey(key)
                        map[key] = element
                        replaced
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
            )
            primitiveKind == PrimitiveAbiKind.BOOLEAN -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUnitMethods = mapOf(
                    12 to {
                        map.clear()
                        HResult(0)
                    },
                ),
                noArgObjectMethods = mapOf(
                    9 to getViewMethod,
                ),
                stringArgUnitMethods = mapOf(11 to removeMethod),
                stringArgBooleanMethods = mapOf(
                    6 to { key -> marshalPrimitiveBooleanValue(lookupValue(key)) },
                    8 to { key -> map.containsKey(key) },
                ),
                stringBooleanArgBooleanMethods = mapOf(
                    10 to { key, element ->
                        val replaced = map.containsKey(key)
                        map[key] = projectPrimitiveValueFromAbi(primitiveKind, valueProjectionTypeKey, element)
                        replaced
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
            )
            primitiveKind == PrimitiveAbiKind.INT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUnitMethods = mapOf(
                    12 to {
                        map.clear()
                        HResult(0)
                    },
                ),
                noArgObjectMethods = mapOf(
                    9 to getViewMethod,
                ),
                stringArgUnitMethods = mapOf(11 to removeMethod),
                stringArgInt32Methods = mapOf(6 to { key -> marshalPrimitiveInt32Value(lookupValue(key)) }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
                stringInt32ArgBooleanMethods = mapOf(
                    10 to { key, element ->
                        val replaced = map.containsKey(key)
                        map[key] = projectPrimitiveValueFromAbi(primitiveKind, valueProjectionTypeKey, element)
                        replaced
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
            )
            primitiveKind == PrimitiveAbiKind.UINT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUnitMethods = mapOf(
                    12 to {
                        map.clear()
                        HResult(0)
                    },
                ),
                noArgObjectMethods = mapOf(
                    9 to getViewMethod,
                ),
                stringArgUnitMethods = mapOf(11 to removeMethod),
                stringArgUInt32Methods = mapOf(6 to { key -> marshalPrimitiveUInt32Value(lookupValue(key)) }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
                stringUInt32ArgBooleanMethods = mapOf(
                    10 to { key, element ->
                        val replaced = map.containsKey(key)
                        map[key] = projectPrimitiveValueFromAbi(primitiveKind, valueProjectionTypeKey, element)
                        replaced
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
            )
            primitiveKind == PrimitiveAbiKind.INT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUnitMethods = mapOf(
                    12 to {
                        map.clear()
                        HResult(0)
                    },
                ),
                noArgObjectMethods = mapOf(
                    9 to getViewMethod,
                ),
                stringArgUnitMethods = mapOf(11 to removeMethod),
                stringArgInt64Methods = mapOf(6 to { key -> marshalPrimitiveInt64Value(lookupValue(key)) }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
                stringInt64ArgBooleanMethods = mapOf(
                    10 to { key, element ->
                        val replaced = map.containsKey(key)
                        map[key] = projectPrimitiveValueFromAbi(primitiveKind, valueProjectionTypeKey, element)
                        replaced
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
            )
            primitiveKind == PrimitiveAbiKind.UINT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUnitMethods = mapOf(
                    12 to {
                        map.clear()
                        HResult(0)
                    },
                ),
                noArgObjectMethods = mapOf(
                    9 to getViewMethod,
                ),
                stringArgUnitMethods = mapOf(11 to removeMethod),
                stringArgUInt64Methods = mapOf(6 to { key -> marshalPrimitiveUInt64Value(lookupValue(key)) }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
                stringUInt64ArgBooleanMethods = mapOf(
                    10 to { key, element ->
                        val replaced = map.containsKey(key)
                        map[key] = projectPrimitiveValueFromAbi(primitiveKind, valueProjectionTypeKey, element)
                        replaced
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
            )
            primitiveKind == PrimitiveAbiKind.FLOAT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUnitMethods = mapOf(
                    12 to {
                        map.clear()
                        HResult(0)
                    },
                ),
                noArgObjectMethods = mapOf(
                    9 to getViewMethod,
                ),
                stringArgUnitMethods = mapOf(11 to removeMethod),
                stringArgFloat32Methods = mapOf(6 to { key -> marshalPrimitiveFloat32Value(lookupValue(key)) }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
                stringFloat32ArgBooleanMethods = mapOf(
                    10 to { key, element ->
                        val replaced = map.containsKey(key)
                        map[key] = projectPrimitiveValueFromAbi(primitiveKind, valueProjectionTypeKey, element)
                        replaced
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
            )
            primitiveKind == PrimitiveAbiKind.FLOAT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUnitMethods = mapOf(
                    12 to {
                        map.clear()
                        HResult(0)
                    },
                ),
                noArgObjectMethods = mapOf(
                    9 to getViewMethod,
                ),
                stringArgUnitMethods = mapOf(11 to removeMethod),
                stringArgFloat64Methods = mapOf(6 to { key -> marshalPrimitiveFloat64Value(lookupValue(key)) }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
                stringFloat64ArgBooleanMethods = mapOf(
                    10 to { key, element ->
                        val replaced = map.containsKey(key)
                        map[key] = projectPrimitiveValueFromAbi(primitiveKind, valueProjectionTypeKey, element)
                        replaced
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
            )
            else -> JvmWinRtObjectStub.InterfaceSpec(
                iid = signature.iid,
                noArgUnitMethods = mapOf(
                    12 to {
                        map.clear()
                        HResult(0)
                    },
                ),
                noArgObjectMethods = mapOf(
                    9 to getViewMethod,
                ),
                stringArgUnitMethods = mapOf(11 to removeMethod),
                stringArgObjectMethods = mapOf(
                    6 to { key ->
                        marshalObjectResultPointer(
                            value = lookupValue(key),
                            projectionTypeKey = valueProjectionTypeKey,
                            signature = valueSignature,
                            retainedChildren = retainedChildren,
                        )
                    },
                ),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
                stringObjectArgBooleanMethods = mapOf(
                    10 to { key, pointer ->
                        val replaced = map.containsKey(key)
                        map[key] = projectObjectValueFromPointer(pointer, valueProjectionTypeKey, valueSignature)
                        replaced
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
            )
        }
        val mapViewSpec = when {
            valueSignature is AbiValueSignature.StringType -> JvmWinRtObjectStub.InterfaceSpec(
                iid = mapViewSignature.iid,
                stringArgHStringMethods = mapOf(
                    6 to {
                        val result = lookupValue(it)
                        result as? String
                            ?: error("Expected map value for '$it' to be String, got ${result?.let { value -> value::class.qualifiedName }}")
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            primitiveKind == PrimitiveAbiKind.BOOLEAN -> JvmWinRtObjectStub.InterfaceSpec(
                iid = mapViewSignature.iid,
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(
                    6 to { key -> marshalPrimitiveBooleanValue(lookupValue(key)) },
                    8 to { key -> map.containsKey(key) },
                ),
            )
            primitiveKind == PrimitiveAbiKind.INT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = mapViewSignature.iid,
                stringArgInt32Methods = mapOf(6 to { key -> marshalPrimitiveInt32Value(lookupValue(key)) }),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            primitiveKind == PrimitiveAbiKind.UINT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = mapViewSignature.iid,
                stringArgUInt32Methods = mapOf(6 to { key -> marshalPrimitiveUInt32Value(lookupValue(key)) }),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            primitiveKind == PrimitiveAbiKind.INT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = mapViewSignature.iid,
                stringArgInt64Methods = mapOf(6 to { key -> marshalPrimitiveInt64Value(lookupValue(key)) }),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            primitiveKind == PrimitiveAbiKind.UINT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = mapViewSignature.iid,
                stringArgUInt64Methods = mapOf(6 to { key -> marshalPrimitiveUInt64Value(lookupValue(key)) }),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            primitiveKind == PrimitiveAbiKind.FLOAT32 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = mapViewSignature.iid,
                stringArgFloat32Methods = mapOf(6 to { key -> marshalPrimitiveFloat32Value(lookupValue(key)) }),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            primitiveKind == PrimitiveAbiKind.FLOAT64 -> JvmWinRtObjectStub.InterfaceSpec(
                iid = mapViewSignature.iid,
                stringArgFloat64Methods = mapOf(6 to { key -> marshalPrimitiveFloat64Value(lookupValue(key)) }),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
            else -> JvmWinRtObjectStub.InterfaceSpec(
                iid = mapViewSignature.iid,
                stringArgObjectMethods = mapOf(
                    6 to { key ->
                        marshalObjectResultPointer(
                            value = lookupValue(key),
                            projectionTypeKey = valueProjectionTypeKey,
                            signature = valueSignature,
                            retainedChildren = retainedChildren,
                        )
                    },
                ),
                noArgUInt32Methods = mapOf(7 to { map.size.toUInt() }),
                stringArgBooleanMethods = mapOf(8 to { key -> map.containsKey(key) }),
            )
        }.copy(noArgTwoObjectMethods = mapOf(9 to splitMethod))
        val baseIterableSpec = JvmWinRtObjectStub.InterfaceSpec(
            iid = iterableSignature.iid,
            noArgObjectMethods = mapOf(6 to firstMethod),
        )
        val stub = JvmWinRtObjectStub.create(interfaceSpec, mapViewSpec, baseIterableSpec)
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
        val valuePrimitiveKind = primitiveAbiKind(valueSignature)
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
                if (valueSignature !is AbiValueSignature.StringType && valuePrimitiveKind == null) {
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
            noArgBooleanMethods = buildMap {
                if (valuePrimitiveKind == PrimitiveAbiKind.BOOLEAN) {
                    put(7) { marshalPrimitiveBooleanValue(entry.value) }
                }
            },
            noArgInt32Methods = buildMap {
                if (valuePrimitiveKind == PrimitiveAbiKind.INT32) {
                    put(7) { marshalPrimitiveInt32Value(entry.value) }
                }
            },
            noArgUInt32Methods = buildMap {
                if (valuePrimitiveKind == PrimitiveAbiKind.UINT32) {
                    put(7) { marshalPrimitiveUInt32Value(entry.value) }
                }
            },
            noArgInt64Methods = buildMap {
                if (valuePrimitiveKind == PrimitiveAbiKind.INT64) {
                    put(7) { marshalPrimitiveInt64Value(entry.value) }
                }
            },
            noArgUInt64Methods = buildMap {
                if (valuePrimitiveKind == PrimitiveAbiKind.UINT64) {
                    put(7) { marshalPrimitiveUInt64Value(entry.value) }
                }
            },
            noArgFloat32Methods = buildMap {
                if (valuePrimitiveKind == PrimitiveAbiKind.FLOAT32) {
                    put(7) { marshalPrimitiveFloat32Value(entry.value) }
                }
            },
            noArgFloat64Methods = buildMap {
                if (valuePrimitiveKind == PrimitiveAbiKind.FLOAT64) {
                    put(7) { marshalPrimitiveFloat64Value(entry.value) }
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
                        marshalObjectResultPointer(
                            value = state.requireCurrentValue("IBindableIterator"),
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
                9 to { pointer, _ -> inspectableIndexOf(list, pointer) >= 0 },
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
                    if (list.isEmpty()) {
                        KnownHResults.E_BOUNDS
                    } else {
                        list.removeAt(list.lastIndex)
                        HResult(0)
                    }
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
                    list[index.toInt()] = projectObjectValueFromPointer(
                        pointer,
                        elementProjectionTypeKey,
                        AbiValueSignature.ObjectType(WinRtTypeSignature.object_()),
                    )
                    HResult(0)
                },
                12 to { index, pointer ->
                    list.add(
                        index.toInt(),
                        projectObjectValueFromPointer(
                            pointer,
                            elementProjectionTypeKey,
                            AbiValueSignature.ObjectType(WinRtTypeSignature.object_()),
                        ),
                    )
                    HResult(0)
                },
            ),
            objectUInt32ArgBooleanMethods = mapOf(
                10 to { pointer, _ -> inspectableIndexOf(list, pointer) >= 0 },
            ),
            objectArgUnitMethods = mapOf(
                14 to { pointer ->
                    list.add(
                        projectObjectValueFromPointer(
                            pointer,
                            elementProjectionTypeKey,
                            AbiValueSignature.ObjectType(WinRtTypeSignature.object_()),
                        ),
                    )
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
            is AbiValueSignature.ObjectType -> primitiveAbiKind(signature)?.let {
                error("Primitive values for $projectionTypeKey must use the primitive ABI path")
            } ?: when (value) {
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
            is AbiValueSignature.ObjectType -> primitiveAbiKind(signature)?.wrapperQualifiedType ?: if (signature.rawSignature.startsWith("struct(")) {
                signature.rawSignature.removePrefix("struct(").substringBefore(';')
            } else if (signature.rawSignature.startsWith("rc(")) {
                signature.rawSignature.removePrefix("rc(").substringBefore(';')
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
                vectorIid.canonical -> "kotlin.collections.MutableList<${inferProjectionTypeKey(signature.arguments.single())}>"
                vectorViewIid.canonical -> "kotlin.collections.List<${inferProjectionTypeKey(signature.arguments.single())}>"
                mapIid.canonical -> "kotlin.collections.MutableMap<${signature.arguments.joinToString(", ") { inferProjectionTypeKey(it) }}>"
                mapViewIid.canonical -> "kotlin.collections.Map<${signature.arguments.joinToString(", ") { inferProjectionTypeKey(it) }}>"
                keyValuePairIid.canonical ->
                    "kotlin.collections.Map.Entry<${signature.arguments.joinToString(", ") { inferProjectionTypeKey(it) }}>"
                iReferenceIid.canonical -> "dev.winrt.core.IReference<${inferProjectionTypeKey(signature.arguments.single())}>"
                else -> "Object"
            }
        }
    }

    private fun primitiveAbiKind(signature: AbiValueSignature): PrimitiveAbiKind? {
        return when (signature) {
            is AbiValueSignature.ObjectType -> PrimitiveAbiKind.fromRawSignature(signature.rawSignature)
            else -> null
        }
    }

    private fun currentIteratorValue(state: IteratorState): Any? {
        return state.requireCurrentValue("IIterator")
    }

    private fun splitStringKeyedMapView(map: Map<String, Any?>): Pair<Map<String, Any?>?, Map<String, Any?>?> {
        if (map.size < 2) {
            return null to null
        }
        val sortedEntries = map.entries.sortedBy(Map.Entry<String, Any?>::key)
        val splitIndex = (sortedEntries.size + 1) / 2
        val first = sortedEntries
            .subList(0, splitIndex)
            .associateTo(linkedMapOf<String, Any?>()) { it.key to it.value }
        val second = sortedEntries
            .subList(splitIndex, sortedEntries.size)
            .associateTo(linkedMapOf<String, Any?>()) { it.key to it.value }
        return first to second
    }

    private fun mapViewSplitMethod(
        map: Map<String, Any?>,
        projectionTypeKey: ProjectionTypeKey,
        signature: AbiValueSignature.ParameterizedInterface,
        retainedChildren: MutableList<AutoCloseable>,
    ): () -> Pair<ComPtr, ComPtr> {
        return {
            val (firstPartition, secondPartition) = splitStringKeyedMapView(map)
            fun createPartitionPointer(partition: Map<String, Any?>?): ComPtr {
                if (partition == null) {
                    return ComPtr.NULL
                }
                val handle = requireNotNull(createMapViewHandle(partition, projectionTypeKey, signature))
                retainedChildren += handle
                return handle.pointer.withAddRef()
            }
            createPartitionPointer(firstPartition) to createPartitionPointer(secondPartition)
        }
    }

    private fun removeStringKeyedMapEntry(map: MutableMap<String, Any?>, key: String): HResult {
        if (!map.containsKey(key)) {
            return KnownHResults.E_BOUNDS
        }
        map.remove(key)
        return HResult(0)
    }

    private fun primitiveIndexOf(list: List<*>, primitiveKind: PrimitiveAbiKind, value: Any): Int {
        return list.indexOfFirst { element ->
            when (primitiveKind) {
                PrimitiveAbiKind.BOOLEAN -> marshalPrimitiveBooleanValue(element) == value as Boolean
                PrimitiveAbiKind.INT32 -> marshalPrimitiveInt32Value(element) == value as Int
                PrimitiveAbiKind.UINT32 -> marshalPrimitiveUInt32Value(element) == value as UInt
                PrimitiveAbiKind.INT64 -> marshalPrimitiveInt64Value(element) == value as Long
                PrimitiveAbiKind.UINT64 -> marshalPrimitiveUInt64Value(element) == value as ULong
                PrimitiveAbiKind.FLOAT32 -> marshalPrimitiveFloat32Value(element) == value as Float
                PrimitiveAbiKind.FLOAT64 -> marshalPrimitiveFloat64Value(element) == value as Double
            }
        }
    }

    private fun projectPrimitiveValueFromAbi(
        primitiveKind: PrimitiveAbiKind,
        projectionTypeKey: String,
        value: Any,
    ): Any {
        val rawType = parseProjectionTypeKey(projectionTypeKey).rawType
        return when (primitiveKind) {
            PrimitiveAbiKind.BOOLEAN -> if (matchesKotlinPrimitiveType(rawType, primitiveKind)) {
                value as Boolean
            } else {
                WinRtBoolean(value as Boolean)
            }
            PrimitiveAbiKind.INT32 -> if (matchesKotlinPrimitiveType(rawType, primitiveKind)) {
                value as Int
            } else {
                Int32(value as Int)
            }
            PrimitiveAbiKind.UINT32 -> if (matchesKotlinPrimitiveType(rawType, primitiveKind)) {
                value as UInt
            } else {
                UInt32(value as UInt)
            }
            PrimitiveAbiKind.INT64 -> if (matchesKotlinPrimitiveType(rawType, primitiveKind)) {
                value as Long
            } else {
                Int64(value as Long)
            }
            PrimitiveAbiKind.UINT64 -> if (matchesKotlinPrimitiveType(rawType, primitiveKind)) {
                value as ULong
            } else {
                UInt64(value as ULong)
            }
            PrimitiveAbiKind.FLOAT32 -> if (matchesKotlinPrimitiveType(rawType, primitiveKind)) {
                value as Float
            } else {
                Float32(value as Float)
            }
            PrimitiveAbiKind.FLOAT64 -> if (matchesKotlinPrimitiveType(rawType, primitiveKind)) {
                value as Double
            } else {
                Float64(value as Double)
            }
        }
    }

    private fun marshalPrimitiveBooleanValue(value: Any?): Boolean {
        return when (value) {
            is WinRtBoolean -> value.value
            is Boolean -> value
            else -> error("Expected WinRT Boolean value, got ${value?.let { it::class.qualifiedName }}")
        }
    }

    private fun marshalPrimitiveInt32Value(value: Any?): Int {
        return when (value) {
            is Int32 -> value.value
            is Int -> value
            else -> error("Expected WinRT Int32 value, got ${value?.let { it::class.qualifiedName }}")
        }
    }

    private fun marshalPrimitiveUInt32Value(value: Any?): UInt {
        return when (value) {
            is UInt32 -> value.value
            is UInt -> value
            else -> error("Expected WinRT UInt32 value, got ${value?.let { it::class.qualifiedName }}")
        }
    }

    private fun marshalPrimitiveInt64Value(value: Any?): Long {
        return when (value) {
            is Int64 -> value.value
            is Long -> value
            else -> error("Expected WinRT Int64 value, got ${value?.let { it::class.qualifiedName }}")
        }
    }

    private fun marshalPrimitiveUInt64Value(value: Any?): ULong {
        return when (value) {
            is UInt64 -> value.value
            is ULong -> value
            else -> error("Expected WinRT UInt64 value, got ${value?.let { it::class.qualifiedName }}")
        }
    }

    private fun marshalPrimitiveFloat32Value(value: Any?): Float {
        return when (value) {
            is Float32 -> value.value
            is Float -> value
            else -> error("Expected WinRT Float32 value, got ${value?.let { it::class.qualifiedName }}")
        }
    }

    private fun marshalPrimitiveFloat64Value(value: Any?): Double {
        return when (value) {
            is Float64 -> value.value
            is Double -> value
            else -> error("Expected WinRT Float64 value, got ${value?.let { it::class.qualifiedName }}")
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

    private fun inspectableIndexOf(list: List<*>, pointer: ComPtr): Int {
        return list.indexOfFirst { element ->
            when (element) {
                null -> pointer.isNull
                is Inspectable -> element.pointer == pointer
                else -> false
            }
        }
    }

    private fun projectObjectValueFromPointer(
        pointer: ComPtr,
        projectionTypeKey: String,
        signature: AbiValueSignature,
    ): Any? {
        if (pointer.isNull) {
            return null
        }
        val inspectable = Inspectable(pointer)
        val rawProjectionType = parseProjectionTypeKey(projectionTypeKey).rawType
        if (rawProjectionType == "Object" || rawProjectionType == Inspectable::class.qualifiedName) {
            return inspectable
        }

        val projectedClass = resolveProjectedClass(rawProjectionType)
            ?: error("Unsupported projected type $projectionTypeKey for ABI pointer re-projection")
        projectedCompanion(projectedClass)?.let { companion ->
            projectedCompanionFactory(companion)?.let { factory ->
                return factory.invoke(companion, inspectable)
            }
        }
        instantiateProjectedClass(
            projectedClass = projectedClass,
            pointer = pointer,
            stringArguments = if (signature is AbiValueSignature.ParameterizedInterface) {
                signature.arguments.map { it.rawSignature }
            } else {
                emptyList()
            },
        )?.let { return it }
        error("Unsupported projected type $projectionTypeKey for ABI pointer re-projection")
    }

    private fun resolveProjectedClass(rawProjectionType: String): Class<*>? {
        return sequenceOf(
            rawProjectionType,
            toJvmQualifiedTypeName(rawProjectionType),
        ).distinct().mapNotNull { candidate ->
            runCatching { Class.forName(candidate) }.getOrNull()
        }.firstOrNull()
    }

    private fun projectedCompanion(projectedClass: Class<*>): Any? {
        return runCatching { projectedClass.getField("Companion").get(null) }.getOrNull()
    }

    private fun projectedCompanionFactory(companion: Any): java.lang.reflect.Method? {
        return companion.javaClass.methods.firstOrNull { method ->
            method.name == "from" &&
                method.parameterTypes.contentEquals(arrayOf(Inspectable::class.java))
        }
    }

    private fun instantiateProjectedClass(
        projectedClass: Class<*>,
        pointer: ComPtr,
        stringArguments: List<String>,
    ): Any? {
        projectedClass.constructors.firstOrNull { constructor ->
            constructor.parameterTypes.contentEquals(arrayOf(ComPtr::class.java))
        }?.let { constructor ->
            return constructor.newInstance(pointer)
        }
        projectedClass.constructors.firstOrNull { constructor ->
            val parameterTypes = constructor.parameterTypes
            parameterTypes.size == stringArguments.size + 1 &&
                parameterTypes.firstOrNull() == Long::class.javaPrimitiveType &&
                parameterTypes.drop(1).all { it == String::class.java }
        }?.let { constructor ->
            return constructor.newInstance(pointer.value.rawValue, *stringArguments.toTypedArray())
        }
        projectedClass.constructors.firstOrNull { constructor ->
            val parameterTypes = constructor.parameterTypes
            parameterTypes.size == stringArguments.size + 2 &&
                parameterTypes.firstOrNull() == Long::class.javaPrimitiveType &&
                parameterTypes.lastOrNull()?.name == "kotlin.jvm.internal.DefaultConstructorMarker" &&
                parameterTypes.drop(1).dropLast(1).all { it == String::class.java }
        }?.let { constructor ->
            return constructor.newInstance(pointer.value.rawValue, *stringArguments.toTypedArray(), null)
        }
        return null
    }

    private fun toJvmQualifiedTypeName(rawProjectionType: String): String {
        if (!rawProjectionType.contains('.')) {
            return rawProjectionType
        }
        val parts = rawProjectionType.split('.')
        return buildString {
            append(parts.dropLast(1).joinToString(".") { it.lowercase() })
            append('.')
            append(parts.last())
        }
    }

    private class IteratorState(
        private val iterator: Iterator<*>,
    ) {
        private var initialStatePending = true
        private var currentValue: Any? = null
        private var hasCurrentValue: Boolean = false

        val hasCurrent: Boolean
            get() {
                ensureInitialized()
                return hasCurrentValue
            }

        fun moveNext(): Boolean {
            if (initialStatePending) {
                initialStatePending = false
                return advance()
            }
            return advance()
        }

        fun requireCurrentValue(iteratorInterfaceName: String): Any? {
            ensureInitialized()
            if (!hasCurrentValue) {
                throw WinRtException(KnownHResults.E_BOUNDS)
            }
            return currentValue
        }

        private fun ensureInitialized() {
            if (!initialStatePending) {
                return
            }
            initialStatePending = false
            advance()
        }

        private fun advance(): Boolean {
            if (iterator.hasNext()) {
                currentValue = iterator.next()
                hasCurrentValue = true
                return true
            }
            currentValue = null
            hasCurrentValue = false
            return false
        }
    }

    private enum class PrimitiveAbiKind(
        val rawSignature: String,
        val wrapperQualifiedType: String,
        val wrapperSimpleType: String,
        val kotlinQualifiedType: String,
        val kotlinSimpleType: String,
    ) {
        BOOLEAN("b1", "dev.winrt.core.WinRtBoolean", "WinRtBoolean", "kotlin.Boolean", "Boolean"),
        INT32("i4", "dev.winrt.core.Int32", "Int32", "kotlin.Int", "Int"),
        UINT32("u4", "dev.winrt.core.UInt32", "UInt32", "kotlin.UInt", "UInt"),
        INT64("i8", "dev.winrt.core.Int64", "Int64", "kotlin.Long", "Long"),
        UINT64("u8", "dev.winrt.core.UInt64", "UInt64", "kotlin.ULong", "ULong"),
        FLOAT32("f4", "dev.winrt.core.Float32", "Float32", "kotlin.Float", "Float"),
        FLOAT64("f8", "dev.winrt.core.Float64", "Float64", "kotlin.Double", "Double");

        companion object {
            fun fromRawSignature(rawSignature: String): PrimitiveAbiKind? =
                entries.firstOrNull { it.rawSignature == rawSignature }
        }
    }

    private fun matchesKotlinPrimitiveType(rawType: String, primitiveKind: PrimitiveAbiKind): Boolean {
        return rawType == primitiveKind.kotlinSimpleType ||
            rawType == primitiveKind.kotlinQualifiedType
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
