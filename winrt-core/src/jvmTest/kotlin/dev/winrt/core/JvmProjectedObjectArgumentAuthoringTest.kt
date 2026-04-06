package dev.winrt.core

import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.HResult
import dev.winrt.kom.HString
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireBoolean
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import windows.foundation.IAsyncOperation
import windows.data.json.IJsonValue
import windows.data.json.JsonValue

class JvmProjectedObjectArgumentAuthoringTest {
    private enum class ExampleMode(
        val value: UInt,
    ) {
        Alpha(1u),
        Beta(2u),
    }

    private data class PrimitiveVectorCase<T : Any>(
        val projectionType: String,
        val signature: String,
        val initial: List<Any>,
        val first: T,
        val second: T,
        val replacement: T,
        val inserted: T,
        val appended: T,
        val current: (ComPtr) -> T,
        val getAt: (ComPtr, UInt) -> T,
        val unwrap: (Any?) -> T,
    )

    private data class PrimitiveMapCase<T : Any>(
        val projectionType: String,
        val signature: String,
        val initialFirst: Any,
        val initialSecond: Any,
        val first: T,
        val second: T,
        val replacement: T,
        val inserted: T,
        val lookup: (ComPtr, String) -> T,
        val currentValue: (ComPtr) -> T,
        val unwrap: (Any?) -> T,
    )

    private data class CallerAllocatedStringArrayResult(
        val values: List<String>,
        val count: UInt,
    )

    private data class CallerAllocatedInt32ArrayResult(
        val values: List<Int>,
        val count: UInt,
    )

    private data class IndexOfResult(
        val found: Boolean,
        val index: UInt,
    )

    private val linker = Linker.nativeLinker()
    private val noArgCallerAllocatedArrayHandle = linker.downcallHandle(
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
        ),
    )
    private val uint32CallerAllocatedArrayHandle = linker.downcallHandle(
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
        ),
    )
    private val callerAllocatedArrayArgHandle = linker.downcallHandle(
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
        ),
    )
    private val addressIndexOfHandle = linker.downcallHandle(
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
        ),
    )
    private val intIndexOfHandle = linker.downcallHandle(
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
        ),
    )
    private val longIndexOfHandle = linker.downcallHandle(
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
        ),
    )
    private val floatIndexOfHandle = linker.downcallHandle(
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_FLOAT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
        ),
    )
    private val doubleIndexOfHandle = linker.downcallHandle(
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_DOUBLE,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
        ),
    )

    private fun vtableEntry(instance: ComPtr, index: Int): MemorySegment {
        val objectPointer = MemorySegment.ofAddress(instance.value.rawValue).reinterpret(ValueLayout.ADDRESS.byteSize())
        val vtablePointer = objectPointer.get(ValueLayout.ADDRESS, 0L)
        return vtablePointer
            .reinterpret((index + 1L) * ValueLayout.ADDRESS.byteSize())
            .getAtIndex(ValueLayout.ADDRESS, index.toLong())
    }

    private fun invokeStringGetManyNoArg(
        instance: ComPtr,
        vtableIndex: Int,
        requestedSize: Int,
    ): Result<CallerAllocatedStringArrayResult> {
        return runCatching {
            require(requestedSize >= 0)
            val function = vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val items = if (requestedSize == 0) {
                    MemorySegment.NULL
                } else {
                    arena.allocate(
                        ValueLayout.ADDRESS.byteSize() * requestedSize.toLong(),
                        ValueLayout.ADDRESS.byteAlignment(),
                    )
                }
                val resultCount = arena.allocate(ValueLayout.JAVA_INT)
                val hResult = HResult(
                    noArgCallerAllocatedArrayHandle.bindTo(function).invokeWithArguments(
                        MemorySegment.ofAddress(instance.value.rawValue),
                        requestedSize,
                        items,
                        resultCount,
                    ) as Int,
                )
                hResult.requireSuccess("invokeStringGetManyNoArg($vtableIndex)")
                CallerAllocatedStringArrayResult(
                    values = readStringCallerAllocatedBuffer(items, requestedSize),
                    count = resultCount.get(ValueLayout.JAVA_INT, 0L).toUInt(),
                )
            }
        }
    }

    private fun invokeStringGetManyWithUInt32Arg(
        instance: ComPtr,
        vtableIndex: Int,
        startIndex: UInt,
        requestedSize: Int,
    ): Result<CallerAllocatedStringArrayResult> {
        return runCatching {
            require(requestedSize >= 0)
            val function = vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val items = if (requestedSize == 0) {
                    MemorySegment.NULL
                } else {
                    arena.allocate(
                        ValueLayout.ADDRESS.byteSize() * requestedSize.toLong(),
                        ValueLayout.ADDRESS.byteAlignment(),
                    )
                }
                val resultCount = arena.allocate(ValueLayout.JAVA_INT)
                val hResult = HResult(
                    uint32CallerAllocatedArrayHandle.bindTo(function).invokeWithArguments(
                        MemorySegment.ofAddress(instance.value.rawValue),
                        startIndex.toInt(),
                        requestedSize,
                        items,
                        resultCount,
                    ) as Int,
                )
                hResult.requireSuccess("invokeStringGetManyWithUInt32Arg($vtableIndex)")
                CallerAllocatedStringArrayResult(
                    values = readStringCallerAllocatedBuffer(items, requestedSize),
                    count = resultCount.get(ValueLayout.JAVA_INT, 0L).toUInt(),
                )
            }
        }
    }

    private fun invokeInt32GetManyWithUInt32Arg(
        instance: ComPtr,
        vtableIndex: Int,
        startIndex: UInt,
        requestedSize: Int,
    ): Result<CallerAllocatedInt32ArrayResult> {
        return runCatching {
            require(requestedSize >= 0)
            val function = vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val items = if (requestedSize == 0) {
                    MemorySegment.NULL
                } else {
                    arena.allocate(
                        ValueLayout.JAVA_INT.byteSize() * requestedSize.toLong(),
                        ValueLayout.JAVA_INT.byteAlignment(),
                    )
                }
                val resultCount = arena.allocate(ValueLayout.JAVA_INT)
                val hResult = HResult(
                    uint32CallerAllocatedArrayHandle.bindTo(function).invokeWithArguments(
                        MemorySegment.ofAddress(instance.value.rawValue),
                        startIndex.toInt(),
                        requestedSize,
                        items,
                        resultCount,
                    ) as Int,
                )
                hResult.requireSuccess("invokeInt32GetManyWithUInt32Arg($vtableIndex)")
                CallerAllocatedInt32ArrayResult(
                    values = List(requestedSize) { index ->
                        if (requestedSize == 0) {
                            0
                        } else {
                            items.getAtIndex(ValueLayout.JAVA_INT, index.toLong())
                        }
                    },
                    count = resultCount.get(ValueLayout.JAVA_INT, 0L).toUInt(),
                )
            }
        }
    }

    private fun invokeObjectGetManyNoArg(
        instance: ComPtr,
        vtableIndex: Int,
        requestedSize: Int,
    ): Result<UInt> {
        return runCatching {
            require(requestedSize >= 0)
            val function = vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val items = if (requestedSize == 0) {
                    MemorySegment.NULL
                } else {
                    arena.allocate(
                        ValueLayout.ADDRESS.byteSize() * requestedSize.toLong(),
                        ValueLayout.ADDRESS.byteAlignment(),
                    )
                }
                val resultCount = arena.allocate(ValueLayout.JAVA_INT)
                val hResult = HResult(
                    noArgCallerAllocatedArrayHandle.bindTo(function).invokeWithArguments(
                        MemorySegment.ofAddress(instance.value.rawValue),
                        requestedSize,
                        items,
                        resultCount,
                    ) as Int,
                )
                hResult.requireSuccess("invokeObjectGetManyNoArg($vtableIndex)")
                repeat(requestedSize) { index ->
                    if (items != MemorySegment.NULL) {
                        val address = items.getAtIndex(ValueLayout.ADDRESS, index.toLong()).address()
                        if (address != 0L) {
                            PlatformComInterop.release(ComPtr(AbiIntPtr(address)))
                        }
                    }
                }
                resultCount.get(ValueLayout.JAVA_INT, 0L).toUInt()
            }
        }
    }

    private fun invokeStringReplaceAll(
        instance: ComPtr,
        vtableIndex: Int,
        values: List<String>,
    ): Result<Unit> {
        return runCatching {
            val function = vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val items = if (values.isEmpty()) {
                    MemorySegment.NULL
                } else {
                    arena.allocate(
                        ValueLayout.ADDRESS.byteSize() * values.size.toLong(),
                        ValueLayout.ADDRESS.byteAlignment(),
                    )
                }
                val hStrings = values.map(HString::fromKotlin)
                try {
                    hStrings.forEachIndexed { index, value ->
                        items.setAtIndex(ValueLayout.ADDRESS, index.toLong(), MemorySegment.ofAddress(value.raw))
                    }
                    val hResult = HResult(
                        callerAllocatedArrayArgHandle.bindTo(function).invokeWithArguments(
                            MemorySegment.ofAddress(instance.value.rawValue),
                            values.size,
                            items,
                        ) as Int,
                    )
                    hResult.requireSuccess("invokeStringReplaceAll($vtableIndex)")
                } finally {
                    hStrings.asReversed().forEach(HString::close)
                }
            }
        }
    }

    private fun invokeInt32ReplaceAll(
        instance: ComPtr,
        vtableIndex: Int,
        values: IntArray,
    ): Result<Unit> {
        return runCatching {
            val function = vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val items = if (values.isEmpty()) {
                    MemorySegment.NULL
                } else {
                    arena.allocate(
                        ValueLayout.JAVA_INT.byteSize() * values.size.toLong(),
                        ValueLayout.JAVA_INT.byteAlignment(),
                    ).also { buffer ->
                        values.forEachIndexed { index, value ->
                            buffer.setAtIndex(ValueLayout.JAVA_INT, index.toLong(), value)
                        }
                    }
                }
                val hResult = HResult(
                    callerAllocatedArrayArgHandle.bindTo(function).invokeWithArguments(
                        MemorySegment.ofAddress(instance.value.rawValue),
                        values.size,
                        items,
                    ) as Int,
                )
                hResult.requireSuccess("invokeInt32ReplaceAll($vtableIndex)")
            }
        }
    }

    private fun invokeAddressIndexOf(
        instance: ComPtr,
        vtableIndex: Int,
        address: Long,
        operation: String,
    ): Result<IndexOfResult> {
        return runCatching {
            val function = vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val resultIndex = arena.allocate(ValueLayout.JAVA_INT)
                val resultFound = arena.allocate(ValueLayout.JAVA_INT)
                val hResult = HResult(
                    addressIndexOfHandle.bindTo(function).invokeWithArguments(
                        MemorySegment.ofAddress(instance.value.rawValue),
                        if (address == 0L) MemorySegment.NULL else MemorySegment.ofAddress(address),
                        resultIndex,
                        resultFound,
                    ) as Int,
                )
                hResult.requireSuccess(operation)
                IndexOfResult(
                    found = resultFound.get(ValueLayout.JAVA_INT, 0L) != 0,
                    index = resultIndex.get(ValueLayout.JAVA_INT, 0L).toUInt(),
                )
            }
        }
    }

    private fun invokeIntIndexOf(
        instance: ComPtr,
        vtableIndex: Int,
        value: Int,
        operation: String,
    ): Result<IndexOfResult> {
        return runCatching {
            val function = vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val resultIndex = arena.allocate(ValueLayout.JAVA_INT)
                val resultFound = arena.allocate(ValueLayout.JAVA_INT)
                val hResult = HResult(
                    intIndexOfHandle.bindTo(function).invokeWithArguments(
                        MemorySegment.ofAddress(instance.value.rawValue),
                        value,
                        resultIndex,
                        resultFound,
                    ) as Int,
                )
                hResult.requireSuccess(operation)
                IndexOfResult(
                    found = resultFound.get(ValueLayout.JAVA_INT, 0L) != 0,
                    index = resultIndex.get(ValueLayout.JAVA_INT, 0L).toUInt(),
                )
            }
        }
    }

    private fun invokeLongIndexOf(
        instance: ComPtr,
        vtableIndex: Int,
        value: Long,
        operation: String,
    ): Result<IndexOfResult> {
        return runCatching {
            val function = vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val resultIndex = arena.allocate(ValueLayout.JAVA_INT)
                val resultFound = arena.allocate(ValueLayout.JAVA_INT)
                val hResult = HResult(
                    longIndexOfHandle.bindTo(function).invokeWithArguments(
                        MemorySegment.ofAddress(instance.value.rawValue),
                        value,
                        resultIndex,
                        resultFound,
                    ) as Int,
                )
                hResult.requireSuccess(operation)
                IndexOfResult(
                    found = resultFound.get(ValueLayout.JAVA_INT, 0L) != 0,
                    index = resultIndex.get(ValueLayout.JAVA_INT, 0L).toUInt(),
                )
            }
        }
    }

    private fun invokeFloatIndexOf(
        instance: ComPtr,
        vtableIndex: Int,
        value: Float,
        operation: String,
    ): Result<IndexOfResult> {
        return runCatching {
            val function = vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val resultIndex = arena.allocate(ValueLayout.JAVA_INT)
                val resultFound = arena.allocate(ValueLayout.JAVA_INT)
                val hResult = HResult(
                    floatIndexOfHandle.bindTo(function).invokeWithArguments(
                        MemorySegment.ofAddress(instance.value.rawValue),
                        value,
                        resultIndex,
                        resultFound,
                    ) as Int,
                )
                hResult.requireSuccess(operation)
                IndexOfResult(
                    found = resultFound.get(ValueLayout.JAVA_INT, 0L) != 0,
                    index = resultIndex.get(ValueLayout.JAVA_INT, 0L).toUInt(),
                )
            }
        }
    }

    private fun invokeDoubleIndexOf(
        instance: ComPtr,
        vtableIndex: Int,
        value: Double,
        operation: String,
    ): Result<IndexOfResult> {
        return runCatching {
            val function = vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val resultIndex = arena.allocate(ValueLayout.JAVA_INT)
                val resultFound = arena.allocate(ValueLayout.JAVA_INT)
                val hResult = HResult(
                    doubleIndexOfHandle.bindTo(function).invokeWithArguments(
                        MemorySegment.ofAddress(instance.value.rawValue),
                        value,
                        resultIndex,
                        resultFound,
                    ) as Int,
                )
                hResult.requireSuccess(operation)
                IndexOfResult(
                    found = resultFound.get(ValueLayout.JAVA_INT, 0L) != 0,
                    index = resultIndex.get(ValueLayout.JAVA_INT, 0L).toUInt(),
                )
            }
        }
    }

    private fun invokeStringIndexOf(
        instance: ComPtr,
        vtableIndex: Int,
        value: String,
    ): Result<IndexOfResult> {
        return HString.fromKotlin(value).use { hString ->
            invokeAddressIndexOf(instance, vtableIndex, hString.raw, "invokeStringIndexOf($vtableIndex)")
        }
    }

    private fun invokeObjectIndexOf(
        instance: ComPtr,
        vtableIndex: Int,
        value: ComPtr,
    ): Result<IndexOfResult> {
        return invokeAddressIndexOf(instance, vtableIndex, value.value.rawValue, "invokeObjectIndexOf($vtableIndex)")
    }

    private fun invokePrimitiveIndexOf(
        instance: ComPtr,
        vtableIndex: Int,
        value: Any,
    ): Result<IndexOfResult> {
        return when (value) {
            is Boolean -> invokeIntIndexOf(instance, vtableIndex, if (value) 1 else 0, "invokePrimitiveIndexOf($vtableIndex)")
            is Int -> invokeIntIndexOf(instance, vtableIndex, value, "invokePrimitiveIndexOf($vtableIndex)")
            is UInt -> invokeIntIndexOf(instance, vtableIndex, value.toInt(), "invokePrimitiveIndexOf($vtableIndex)")
            is Long -> invokeLongIndexOf(instance, vtableIndex, value, "invokePrimitiveIndexOf($vtableIndex)")
            is ULong -> invokeLongIndexOf(instance, vtableIndex, value.toLong(), "invokePrimitiveIndexOf($vtableIndex)")
            is Float -> invokeFloatIndexOf(instance, vtableIndex, value, "invokePrimitiveIndexOf($vtableIndex)")
            is Double -> invokeDoubleIndexOf(instance, vtableIndex, value, "invokePrimitiveIndexOf($vtableIndex)")
            else -> error("Unsupported primitive indexOf value ${value::class.qualifiedName}")
        }
    }

    private fun readStringCallerAllocatedBuffer(
        buffer: MemorySegment,
        size: Int,
    ): List<String> {
        if (size == 0) {
            return emptyList()
        }
        return List(size) { index ->
            val address = buffer.getAtIndex(ValueLayout.ADDRESS, index.toLong()).address()
            if (address == 0L) {
                ""
            } else {
                HString(address).use(HString::toKotlinString)
            }
        }
    }

    private fun invokeStringKeyedMapIterator(pointer: ComPtr, valueSignature: String): ComPtr {
        val iterablePointer = PlatformComInterop.queryInterface(
            pointer,
            ParameterizedInterfaceId.createFromSignature(
                WinRtTypeSignature.parameterizedInterface(
                    "faa585ea-6214-4217-afda-7f46de5869b3",
                    WinRtTypeSignature.parameterizedInterface(
                        "02b51929-c1c4-4a7e-8940-0312b5c18500",
                        WinRtTypeSignature.string(),
                        valueSignature,
                    ),
                ),
            ),
        ).getOrThrow()
        try {
            return PlatformComInterop.invokeObjectMethod(iterablePointer, 6).getOrThrow()
        } finally {
            PlatformComInterop.release(iterablePointer)
        }
    }

    private fun readStringKeyedMapKeys(pointer: ComPtr, valueSignature: String): List<String> {
        val iterator = invokeStringKeyedMapIterator(pointer, valueSignature)
        try {
            if (!PlatformComInterop.invokeBooleanGetter(iterator, 7).getOrThrow()) {
                return emptyList()
            }
            val keys = mutableListOf<String>()
            do {
                val current = PlatformComInterop.invokeObjectMethod(iterator, 6).getOrThrow()
                try {
                    PlatformComInterop.invokeHStringMethod(current, 6).getOrThrow().use { key ->
                        keys += key.toKotlinString()
                    }
                } finally {
                    PlatformComInterop.release(current)
                }
            } while (PlatformComInterop.invokeBooleanGetter(iterator, 8).getOrThrow())
            return keys
        } finally {
            PlatformComInterop.release(iterator)
        }
    }

    private fun assertBoundsFailure(result: Result<*>) {
        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("0x${KnownHResults.E_BOUNDS.value.toUInt().toString(16)}") == true,
            result.exceptionOrNull()?.toString(),
        )
    }

    private fun assertFailureHResult(result: Result<*>, hResult: Int) {
        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("0x${hResult.toUInt().toString(16)}") == true,
            result.exceptionOrNull()?.toString(),
        )
    }

    private fun assertIndexOfResult(result: Result<IndexOfResult>, found: Boolean, index: UInt) {
        val value = result.getOrThrow()
        assertEquals(found, value.found)
        assertEquals(index, value.index)
    }

    private fun <T : Any> assertPrimitiveVectorViewCase(case: PrimitiveVectorCase<T>) {
        val pointer = projectedObjectArgumentPointer(
            value = case.initial,
            projectionTypeKey = "kotlin.collections.List<${case.projectionType}>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                case.signature,
            ),
        )

        assertFalse(pointer.isNull)
        assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
        assertEquals(case.second, case.getAt(pointer, 1u))
        assertIndexOfResult(invokePrimitiveIndexOf(pointer, 9, case.second), true, 1u)
        if (case.initial.none { case.unwrap(it) == case.appended }) {
            assertIndexOfResult(invokePrimitiveIndexOf(pointer, 9, case.appended), false, 0u)
        }

        val iterator = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
        try {
            assertEquals(case.first, case.current(iterator))
        } finally {
            PlatformComInterop.release(iterator)
        }
    }

    private fun <T : Any> assertPrimitiveVectorCase(case: PrimitiveVectorCase<T>) {
        val values = case.initial.toMutableList()
        val pointer = projectedObjectArgumentPointer(
            value = values,
            projectionTypeKey = "kotlin.collections.MutableList<${case.projectionType}>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "913337e9-11a1-4345-a3a2-4e7f956e222d",
                case.signature,
            ),
        )

        assertFalse(pointer.isNull)
        assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
        assertEquals(case.second, case.getAt(pointer, 1u))
        assertIndexOfResult(invokePrimitiveIndexOf(pointer, 10, case.second), true, 1u)
        if (case.initial.none { case.unwrap(it) == case.appended }) {
            assertIndexOfResult(invokePrimitiveIndexOf(pointer, 10, case.appended), false, 0u)
        }

        PlatformComInterop.invokeUnitMethodWithArgs(pointer, 11, 0u, case.replacement).getOrThrow()
        assertEquals(case.replacement, case.unwrap(values[0]))

        PlatformComInterop.invokeUnitMethodWithArgs(pointer, 12, 1u, case.inserted).getOrThrow()
        assertEquals(case.inserted, case.unwrap(values[1]))

        PlatformComInterop.invokeUnitMethodWithArgs(pointer, 14, case.appended).getOrThrow()
        assertEquals(case.appended, case.unwrap(values.last()))

        val view = PlatformComInterop.invokeObjectMethod(pointer, 9).getOrThrow()
        try {
            assertEquals(case.second, case.getAt(view, 2u))
        } finally {
            PlatformComInterop.release(view)
        }

        PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 13, 2u).getOrThrow()
        assertEquals(3, values.size)

        PlatformComInterop.invokeUnitMethod(pointer, 15).getOrThrow()
        assertEquals(2, values.size)

        PlatformComInterop.invokeUnitMethod(pointer, 16).getOrThrow()
        assertTrue(values.isEmpty())
    }

    private fun <T : Any> assertPrimitiveMapViewCase(case: PrimitiveMapCase<T>) {
        val pointer = projectedObjectArgumentPointer(
            value = linkedMapOf("theme" to case.initialFirst, "accent" to case.initialSecond),
            projectionTypeKey = "kotlin.collections.Map<String, ${case.projectionType}>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "e480ce40-a338-4ada-adcf-272272e48cb9",
                WinRtTypeSignature.string(),
                case.signature,
            ),
        )

        assertFalse(pointer.isNull)
        assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())
        assertTrue(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 8, "accent").getOrThrow())
        assertEquals(case.second, case.lookup(pointer, "accent"))

        val iterator = invokeStringKeyedMapIterator(pointer, case.signature)
        try {
            val current = PlatformComInterop.invokeObjectMethod(iterator, 6).getOrThrow()
            try {
                PlatformComInterop.invokeHStringMethod(current, 6).getOrThrow().use { key ->
                    assertEquals("theme", key.toKotlinString())
                }
                assertEquals(case.first, case.currentValue(current))
            } finally {
                PlatformComInterop.release(current)
            }
        } finally {
            PlatformComInterop.release(iterator)
        }
    }

    private fun <T : Any> assertPrimitiveMapCase(case: PrimitiveMapCase<T>) {
        val values = linkedMapOf("theme" to case.initialFirst, "accent" to case.initialSecond)
        val pointer = projectedObjectArgumentPointer(
            value = values,
            projectionTypeKey = "kotlin.collections.MutableMap<String, ${case.projectionType}>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "3c2925fe-8519-45c1-aa79-197b6718c1c1",
                WinRtTypeSignature.string(),
                case.signature,
            ),
        )

        assertFalse(pointer.isNull)
        assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())
        assertTrue(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 8, "accent").getOrThrow())
        assertEquals(case.second, case.lookup(pointer, "accent"))

        assertTrue(
            PlatformComInterop.invokeMethodWithResultKind(
                pointer,
                10,
                ComMethodResultKind.BOOLEAN,
                "accent",
                case.replacement,
            ).getOrThrow().requireBoolean(),
        )
        assertEquals(case.replacement, case.unwrap(values.getValue("accent")))

        assertFalse(
            PlatformComInterop.invokeMethodWithResultKind(
                pointer,
                10,
                ComMethodResultKind.BOOLEAN,
                "language",
                case.inserted,
            ).getOrThrow().requireBoolean(),
        )
        assertEquals(case.inserted, case.unwrap(values.getValue("language")))

        val view = PlatformComInterop.invokeObjectMethod(pointer, 9).getOrThrow()
        try {
            assertEquals(3u, PlatformComInterop.invokeUInt32Method(view, 7).getOrThrow())
            assertEquals(case.inserted, case.lookup(view, "language"))
        } finally {
            PlatformComInterop.release(view)
        }

        PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 11, "theme").getOrThrow()
        assertFalse(values.containsKey("theme"))

        PlatformComInterop.invokeUnitMethod(pointer, 12).getOrThrow()
        assertTrue(values.isEmpty())
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_iterable_string_values_on_jvm() {
        val pointer = projectedObjectArgumentPointer(
            value = listOf("en-US", "fr-FR"),
            projectionTypeKey = "kotlin.collections.Iterable<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "faa585ea-6214-4217-afda-7f46de5869b3",
                WinRtTypeSignature.string(),
            ),
        )

        assertFalse(pointer.isNull)

        val iterator = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
        try {
            PlatformComInterop.invokeHStringMethod(iterator, 6).getOrThrow().use { current ->
                assertEquals("en-US", current.toKotlinString())
            }
            assertTrue(PlatformComInterop.invokeBooleanGetter(iterator, 7).getOrThrow())
            assertTrue(PlatformComInterop.invokeBooleanGetter(iterator, 8).getOrThrow())
            PlatformComInterop.invokeHStringMethod(iterator, 6).getOrThrow().use { current ->
                assertEquals("fr-FR", current.toKotlinString())
            }
            assertFalse(PlatformComInterop.invokeBooleanGetter(iterator, 8).getOrThrow())
            assertFalse(PlatformComInterop.invokeBooleanGetter(iterator, 7).getOrThrow())
        } finally {
            PlatformComInterop.release(iterator)
        }
    }

    @Test
    fun projected_object_argument_pointer_iiiterator_move_next_from_fresh_state_matches_cswinrt_on_jvm() {
        val pointer = projectedObjectArgumentPointer(
            value = listOf("en-US", "fr-FR"),
            projectionTypeKey = "kotlin.collections.Iterable<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "faa585ea-6214-4217-afda-7f46de5869b3",
                WinRtTypeSignature.string(),
            ),
        )

        assertFalse(pointer.isNull)

        val iterator = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
        try {
            assertTrue(PlatformComInterop.invokeBooleanGetter(iterator, 8).getOrThrow())
            PlatformComInterop.invokeHStringMethod(iterator, 6).getOrThrow().use { current ->
                assertEquals("en-US", current.toKotlinString())
            }
            assertTrue(PlatformComInterop.invokeBooleanGetter(iterator, 8).getOrThrow())
            PlatformComInterop.invokeHStringMethod(iterator, 6).getOrThrow().use { current ->
                assertEquals("fr-FR", current.toKotlinString())
            }
            assertFalse(PlatformComInterop.invokeBooleanGetter(iterator, 8).getOrThrow())
            assertBoundsFailure(PlatformComInterop.invokeHStringMethod(iterator, 6))
        } finally {
            PlatformComInterop.release(iterator)
        }
    }

    @Test
    fun projected_object_argument_pointer_iiiterator_get_many_matches_cswinrt_on_jvm() {
        val pointer = projectedObjectArgumentPointer(
            value = listOf("en-US", "fr-FR"),
            projectionTypeKey = "kotlin.collections.Iterable<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "faa585ea-6214-4217-afda-7f46de5869b3",
                WinRtTypeSignature.string(),
            ),
        )

        assertFalse(pointer.isNull)

        val iterator = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
        try {
            val firstBatch = invokeStringGetManyNoArg(iterator, 9, 3).getOrThrow()
            assertEquals(2u, firstBatch.count)
            assertEquals(listOf("en-US", "fr-FR", ""), firstBatch.values)

            val secondBatch = invokeStringGetManyNoArg(iterator, 9, 3).getOrThrow()
            assertEquals(0u, secondBatch.count)
            assertEquals(listOf("", "", ""), secondBatch.values)
        } finally {
            PlatformComInterop.release(iterator)
        }
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_iterable_values_for_bindable_iterable_on_jvm() {
        val forwardedIid = guidOf("12345678-1111-2222-3333-444444444444")

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
        ).use { firstStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
            ).use { secondStub ->
                val values = listOf(Inspectable(firstStub.primaryPointer), Inspectable(secondStub.primaryPointer))
                val pointer = projectedObjectArgumentPointer(
                    value = values,
                    projectionTypeKey = "kotlin.collections.Iterable",
                    signature = "{036d2c08-df29-41af-8aa2-d774be62ba6f}",
                )

                assertFalse(pointer.isNull)

                val iterator = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
                try {
                    val current = PlatformComInterop.invokeObjectMethod(iterator, 6).getOrThrow()
                    try {
                        assertEquals(firstStub.primaryPointer.value.rawValue, current.value.rawValue)
                    } finally {
                        PlatformComInterop.release(current)
                    }
                    assertTrue(PlatformComInterop.invokeBooleanGetter(iterator, 7).getOrThrow())
                    assertTrue(PlatformComInterop.invokeBooleanGetter(iterator, 8).getOrThrow())
                    val next = PlatformComInterop.invokeObjectMethod(iterator, 6).getOrThrow()
                    try {
                        assertEquals(secondStub.primaryPointer.value.rawValue, next.value.rawValue)
                    } finally {
                        PlatformComInterop.release(next)
                    }
                    assertFalse(PlatformComInterop.invokeBooleanGetter(iterator, 8).getOrThrow())
                    assertFalse(PlatformComInterop.invokeBooleanGetter(iterator, 7).getOrThrow())
                } finally {
                    PlatformComInterop.release(iterator)
                }
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_reuses_plain_iterable_stub_for_same_object() {
        val languages = listOf("en-US", "fr-FR")

        val first = projectedObjectArgumentPointer(
            value = languages,
            projectionTypeKey = "kotlin.collections.Iterable<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "faa585ea-6214-4217-afda-7f46de5869b3",
                WinRtTypeSignature.string(),
            ),
        )
        val second = projectedObjectArgumentPointer(
            value = languages,
            projectionTypeKey = "kotlin.collections.Iterable<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "faa585ea-6214-4217-afda-7f46de5869b3",
                WinRtTypeSignature.string(),
            ),
        )

        assertEquals(first, second)
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_iterator_values_for_bindable_iterator_on_jvm() {
        val forwardedIid = guidOf("12345678-1111-2222-3333-444444444444")

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
        ).use { firstStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
            ).use { secondStub ->
                val pointer = projectedObjectArgumentPointer(
                    value = listOf(Inspectable(firstStub.primaryPointer), Inspectable(secondStub.primaryPointer)).iterator(),
                    projectionTypeKey = "kotlin.collections.Iterator",
                    signature = "{6a1d6c07-076d-49f2-8314-f52c9c9a8331}",
                )

                assertFalse(pointer.isNull)

                val current = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
                try {
                    assertEquals(firstStub.primaryPointer.value.rawValue, current.value.rawValue)
                } finally {
                    PlatformComInterop.release(current)
                }
                assertTrue(PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow())
                assertTrue(PlatformComInterop.invokeBooleanGetter(pointer, 8).getOrThrow())
                val next = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
                try {
                    assertEquals(secondStub.primaryPointer.value.rawValue, next.value.rawValue)
                } finally {
                    PlatformComInterop.release(next)
                }
                assertFalse(PlatformComInterop.invokeBooleanGetter(pointer, 8).getOrThrow())
                assertFalse(PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow())
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_bindable_iterator_move_next_from_fresh_state_matches_cswinrt_on_jvm() {
        val forwardedIid = guidOf("12345678-1111-2222-3333-444444444444")

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
        ).use { firstStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
            ).use { secondStub ->
                val pointer = projectedObjectArgumentPointer(
                    value = listOf(Inspectable(firstStub.primaryPointer), Inspectable(secondStub.primaryPointer)).iterator(),
                    projectionTypeKey = "kotlin.collections.Iterator",
                    signature = "{6a1d6c07-076d-49f2-8314-f52c9c9a8331}",
                )

                assertFalse(pointer.isNull)

                assertTrue(PlatformComInterop.invokeBooleanGetter(pointer, 8).getOrThrow())
                val current = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
                try {
                    assertEquals(firstStub.primaryPointer.value.rawValue, current.value.rawValue)
                } finally {
                    PlatformComInterop.release(current)
                }
                assertTrue(PlatformComInterop.invokeBooleanGetter(pointer, 8).getOrThrow())
                val next = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
                try {
                    assertEquals(secondStub.primaryPointer.value.rawValue, next.value.rawValue)
                } finally {
                    PlatformComInterop.release(next)
                }
                assertFalse(PlatformComInterop.invokeBooleanGetter(pointer, 8).getOrThrow())
                assertBoundsFailure(PlatformComInterop.invokeObjectMethod(pointer, 6))
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_bindable_iterator_get_many_returns_enotimpl_on_jvm() {
        val forwardedIid = guidOf("12345678-1111-2222-3333-444444444444")

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
        ).use { firstStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
            ).use { secondStub ->
                val pointer = projectedObjectArgumentPointer(
                    value = listOf(Inspectable(firstStub.primaryPointer), Inspectable(secondStub.primaryPointer)).iterator(),
                    projectionTypeKey = "kotlin.collections.Iterator",
                    signature = "{6a1d6c07-076d-49f2-8314-f52c9c9a8331}",
                )

                assertFalse(pointer.isNull)
                assertFailureHResult(
                    invokeObjectGetManyNoArg(pointer, 9, 2),
                    KnownHResults.E_NOTIMPL.value,
                )
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_map_values_on_jvm() {
        val forwardedIid = guidOf("12345678-1111-2222-3333-444444444444")

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
        ).use { valueStub ->
            val value = Inspectable(valueStub.primaryPointer)
            val pointer = projectedObjectArgumentPointer(
                value = linkedMapOf("theme" to value),
                projectionTypeKey = "kotlin.collections.Map<String, Object>",
                signature = WinRtTypeSignature.parameterizedInterface(
                    "e480ce40-a338-4ada-adcf-272272e48cb9",
                    WinRtTypeSignature.string(),
                    WinRtTypeSignature.object_(),
                ),
            )

            assertFalse(pointer.isNull)
            assertEquals(1u, PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())
            assertTrue(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 8, "theme").getOrThrow())

            val lookup = PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6, "theme").getOrThrow()
            try {
                assertEquals(valueStub.primaryPointer.value.rawValue, lookup.value.rawValue)
            } finally {
                PlatformComInterop.release(lookup)
            }
            assertBoundsFailure(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6, "accent"))

            val iterator = invokeStringKeyedMapIterator(pointer, WinRtTypeSignature.object_())
            try {
                val current = PlatformComInterop.invokeObjectMethod(iterator, 6).getOrThrow()
                try {
                    PlatformComInterop.invokeHStringMethod(current, 6).getOrThrow().use { key ->
                        assertEquals("theme", key.toKotlinString())
                    }
                    val currentValue = PlatformComInterop.invokeObjectMethod(current, 7).getOrThrow()
                    try {
                        assertEquals(valueStub.primaryPointer.value.rawValue, currentValue.value.rawValue)
                    } finally {
                        PlatformComInterop.release(currentValue)
                    }
                } finally {
                    PlatformComInterop.release(current)
                }
            } finally {
                PlatformComInterop.release(iterator)
            }

            val (first, second) = PlatformComInterop.invokeTwoObjectMethod(pointer, 9).getOrThrow()
            assertTrue(first.isNull)
            assertTrue(second.isNull)
        }
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_mutable_string_map_values_for_imap_on_jvm() {
        val values = linkedMapOf("theme" to "dark")
        val pointer = projectedObjectArgumentPointer(
            value = values,
            projectionTypeKey = "kotlin.collections.MutableMap<String, String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "3c2925fe-8519-45c1-aa79-197b6718c1c1",
                WinRtTypeSignature.string(),
                WinRtTypeSignature.string(),
            ),
        )

        assertFalse(pointer.isNull)
        assertEquals(1u, PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())
        assertTrue(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 8, "theme").getOrThrow())
        PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 6, "theme").getOrThrow().use { value ->
            assertEquals("dark", value.toKotlinString())
        }
        assertBoundsFailure(PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 6, "accent"))
        assertBoundsFailure(PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 11, "accent"))

        assertTrue(
            PlatformComInterop.invokeMethodWithResultKind(
                pointer,
                10,
                ComMethodResultKind.BOOLEAN,
                "theme",
                "light",
            ).getOrThrow().requireBoolean(),
        )
        assertEquals("light", values["theme"])

        assertFalse(
            PlatformComInterop.invokeMethodWithResultKind(
                pointer,
                10,
                ComMethodResultKind.BOOLEAN,
                "accent",
                "blue",
            ).getOrThrow().requireBoolean(),
        )
        assertEquals("blue", values["accent"])

        val mapViewPointer = PlatformComInterop.queryInterface(
            pointer,
            ParameterizedInterfaceId.createFromSignature(
                WinRtTypeSignature.parameterizedInterface(
                    "e480ce40-a338-4ada-adcf-272272e48cb9",
                    WinRtTypeSignature.string(),
                    WinRtTypeSignature.string(),
                ),
            ),
        ).getOrThrow()
        try {
            assertEquals(2u, PlatformComInterop.invokeUInt32Method(mapViewPointer, 7).getOrThrow())
        } finally {
            PlatformComInterop.release(mapViewPointer)
        }

        PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 11, "theme").getOrThrow()
        assertFalse(values.containsKey("theme"))

        PlatformComInterop.invokeUnitMethod(pointer, 12).getOrThrow()
        assertTrue(values.isEmpty())
    }

    @Test
    fun projected_object_argument_pointer_splits_plain_map_view_like_cswinrt_on_jvm() {
        val pointer = projectedObjectArgumentPointer(
            value = linkedMapOf(
                "theme" to "dark",
                "accent" to "blue",
                "language" to "en-US",
            ),
            projectionTypeKey = "kotlin.collections.Map<String, String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "e480ce40-a338-4ada-adcf-272272e48cb9",
                WinRtTypeSignature.string(),
                WinRtTypeSignature.string(),
            ),
        )

        assertFalse(pointer.isNull)

        val (first, second) = PlatformComInterop.invokeTwoObjectMethod(pointer, 9).getOrThrow()
        try {
            assertFalse(first.isNull)
            assertFalse(second.isNull)
            assertEquals(2u, PlatformComInterop.invokeUInt32Method(first, 7).getOrThrow())
            assertEquals(1u, PlatformComInterop.invokeUInt32Method(second, 7).getOrThrow())
            assertEquals(
                listOf("accent", "language"),
                readStringKeyedMapKeys(first, WinRtTypeSignature.string()),
            )
            assertEquals(
                listOf("theme"),
                readStringKeyedMapKeys(second, WinRtTypeSignature.string()),
            )
        } finally {
            if (!first.isNull) {
                PlatformComInterop.release(first)
            }
            if (!second.isNull) {
                PlatformComInterop.release(second)
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_mutable_object_map_values_for_imap_on_jvm() {
        val forwardedIid = guidOf("12345678-1111-2222-3333-444444444444")

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
        ).use { firstStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
            ).use { secondStub ->
                val values = linkedMapOf("theme" to Inspectable(firstStub.primaryPointer))
                val pointer = projectedObjectArgumentPointer(
                    value = values,
                    projectionTypeKey = "kotlin.collections.MutableMap<String, Object>",
                    signature = WinRtTypeSignature.parameterizedInterface(
                        "3c2925fe-8519-45c1-aa79-197b6718c1c1",
                        WinRtTypeSignature.string(),
                        WinRtTypeSignature.object_(),
                    ),
                )

                assertFalse(pointer.isNull)
                assertEquals(1u, PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())
                assertTrue(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 8, "theme").getOrThrow())

                val lookup = PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6, "theme").getOrThrow()
                try {
                    assertEquals(firstStub.primaryPointer.value.rawValue, lookup.value.rawValue)
                } finally {
                    PlatformComInterop.release(lookup)
                }

                assertTrue(
                    PlatformComInterop.invokeMethodWithResultKind(
                        pointer,
                        10,
                        ComMethodResultKind.BOOLEAN,
                        "theme",
                        secondStub.primaryPointer,
                    ).getOrThrow().requireBoolean(),
                )
                assertEquals(secondStub.primaryPointer.value.rawValue, values.getValue("theme").pointer.value.rawValue)

                val mapViewPointer = PlatformComInterop.queryInterface(
                    pointer,
                    ParameterizedInterfaceId.createFromSignature(
                        WinRtTypeSignature.parameterizedInterface(
                            "e480ce40-a338-4ada-adcf-272272e48cb9",
                            WinRtTypeSignature.string(),
                            WinRtTypeSignature.object_(),
                        ),
                    ),
                ).getOrThrow()
                try {
                    assertEquals(1u, PlatformComInterop.invokeUInt32Method(mapViewPointer, 7).getOrThrow())
                } finally {
                    PlatformComInterop.release(mapViewPointer)
                }

                PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 11, "theme").getOrThrow()
                assertFalse(values.containsKey("theme"))

                PlatformComInterop.invokeUnitMethod(pointer, 12).getOrThrow()
                assertTrue(values.isEmpty())
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_accepts_primitive_map_values_for_map_view_on_jvm() {
        assertPrimitiveMapViewCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.WinRtBoolean",
                signature = "b1",
                initialFirst = WinRtBoolean(false),
                initialSecond = WinRtBoolean(true),
                first = false,
                second = true,
                replacement = true,
                inserted = false,
                lookup = { pointer, key -> PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as WinRtBoolean).value },
            ),
        )
        assertPrimitiveMapViewCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.Int32",
                signature = "i4",
                initialFirst = Int32(1),
                initialSecond = Int32(2),
                first = 1,
                second = 2,
                replacement = 7,
                inserted = 9,
                lookup = { pointer, key -> PlatformComInterop.invokeInt32MethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeInt32Method(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as Int32).value },
            ),
        )
        assertPrimitiveMapViewCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.UInt32",
                signature = "u4",
                initialFirst = UInt32(1u),
                initialSecond = UInt32(2u),
                first = 1u,
                second = 2u,
                replacement = 7u,
                inserted = 9u,
                lookup = { pointer, key -> PlatformComInterop.invokeUInt32MethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as UInt32).value },
            ),
        )
        assertPrimitiveMapViewCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.Int64",
                signature = "i8",
                initialFirst = Int64(1L),
                initialSecond = Int64(2L),
                first = 1L,
                second = 2L,
                replacement = 7L,
                inserted = 9L,
                lookup = { pointer, key -> PlatformComInterop.invokeInt64MethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeInt64Method(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as Int64).value },
            ),
        )
        assertPrimitiveMapViewCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.UInt64",
                signature = "u8",
                initialFirst = UInt64(1uL),
                initialSecond = UInt64(2uL),
                first = 1uL,
                second = 2uL,
                replacement = 7uL,
                inserted = 9uL,
                lookup = { pointer, key -> PlatformComInterop.invokeUInt64MethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeUInt64Method(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as UInt64).value },
            ),
        )
        assertPrimitiveMapViewCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.Float32",
                signature = "f4",
                initialFirst = Float32(1.5f),
                initialSecond = Float32(2.5f),
                first = 1.5f,
                second = 2.5f,
                replacement = 7.5f,
                inserted = 9.5f,
                lookup = { pointer, key -> PlatformComInterop.invokeFloat32MethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeFloat32Method(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as Float32).value },
            ),
        )
        assertPrimitiveMapViewCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.Float64",
                signature = "f8",
                initialFirst = Float64(1.5),
                initialSecond = Float64(2.5),
                first = 1.5,
                second = 2.5,
                replacement = 7.5,
                inserted = 9.5,
                lookup = { pointer, key -> PlatformComInterop.invokeFloat64MethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeFloat64Method(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as Float64).value },
            ),
        )

        val int32MapViewPointer = projectedObjectArgumentPointer(
            value = linkedMapOf("theme" to Int32(7)),
            projectionTypeKey = "kotlin.collections.Map<String, dev.winrt.core.Int32>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "e480ce40-a338-4ada-adcf-272272e48cb9",
                WinRtTypeSignature.string(),
                "i4",
            ),
        )
        assertBoundsFailure(PlatformComInterop.invokeInt32MethodWithStringArg(int32MapViewPointer, 6, "accent"))
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_mutable_primitive_map_values_for_imap_on_jvm() {
        assertPrimitiveMapCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.WinRtBoolean",
                signature = "b1",
                initialFirst = WinRtBoolean(false),
                initialSecond = WinRtBoolean(true),
                first = false,
                second = true,
                replacement = true,
                inserted = false,
                lookup = { pointer, key -> PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as WinRtBoolean).value },
            ),
        )
        assertPrimitiveMapCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.Int32",
                signature = "i4",
                initialFirst = Int32(1),
                initialSecond = Int32(2),
                first = 1,
                second = 2,
                replacement = 7,
                inserted = 9,
                lookup = { pointer, key -> PlatformComInterop.invokeInt32MethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeInt32Method(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as Int32).value },
            ),
        )
        assertPrimitiveMapCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.UInt32",
                signature = "u4",
                initialFirst = UInt32(1u),
                initialSecond = UInt32(2u),
                first = 1u,
                second = 2u,
                replacement = 7u,
                inserted = 9u,
                lookup = { pointer, key -> PlatformComInterop.invokeUInt32MethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as UInt32).value },
            ),
        )
        assertPrimitiveMapCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.Int64",
                signature = "i8",
                initialFirst = Int64(1L),
                initialSecond = Int64(2L),
                first = 1L,
                second = 2L,
                replacement = 7L,
                inserted = 9L,
                lookup = { pointer, key -> PlatformComInterop.invokeInt64MethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeInt64Method(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as Int64).value },
            ),
        )
        assertPrimitiveMapCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.UInt64",
                signature = "u8",
                initialFirst = UInt64(1uL),
                initialSecond = UInt64(2uL),
                first = 1uL,
                second = 2uL,
                replacement = 7uL,
                inserted = 9uL,
                lookup = { pointer, key -> PlatformComInterop.invokeUInt64MethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeUInt64Method(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as UInt64).value },
            ),
        )
        assertPrimitiveMapCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.Float32",
                signature = "f4",
                initialFirst = Float32(1.5f),
                initialSecond = Float32(2.5f),
                first = 1.5f,
                second = 2.5f,
                replacement = 7.5f,
                inserted = 9.5f,
                lookup = { pointer, key -> PlatformComInterop.invokeFloat32MethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeFloat32Method(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as Float32).value },
            ),
        )
        assertPrimitiveMapCase(
            PrimitiveMapCase(
                projectionType = "dev.winrt.core.Float64",
                signature = "f8",
                initialFirst = Float64(1.5),
                initialSecond = Float64(2.5),
                first = 1.5,
                second = 2.5,
                replacement = 7.5,
                inserted = 9.5,
                lookup = { pointer, key -> PlatformComInterop.invokeFloat64MethodWithStringArg(pointer, 6, key).getOrThrow() },
                currentValue = { pointer -> PlatformComInterop.invokeFloat64Method(pointer, 7).getOrThrow() },
                unwrap = { value -> (value as Float64).value },
            ),
        )
    }

    @Test
    fun projected_object_argument_pointer_reprojects_mutable_interface_list_values_for_vector_on_jvm() {
        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = IJsonValue.iid),
        ).use { firstStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = IJsonValue.iid),
            ).use { secondStub ->
                JvmWinRtObjectStub.create(
                    JvmWinRtObjectStub.InterfaceSpec(iid = IJsonValue.iid),
                ).use { replacementStub ->
                    JvmWinRtObjectStub.create(
                        JvmWinRtObjectStub.InterfaceSpec(iid = IJsonValue.iid),
                    ).use { insertedStub ->
                        JvmWinRtObjectStub.create(
                            JvmWinRtObjectStub.InterfaceSpec(iid = IJsonValue.iid),
                        ).use { appendedStub ->
                            val values = mutableListOf(
                                IJsonValue.from(Inspectable(firstStub.primaryPointer)),
                                IJsonValue.from(Inspectable(secondStub.primaryPointer)),
                            )
                            val pointer = projectedObjectArgumentPointer(
                                value = values,
                                projectionTypeKey = "kotlin.collections.MutableList<Windows.Data.Json.IJsonValue>",
                                signature = WinRtTypeSignature.parameterizedInterface(
                                    "913337e9-11a1-4345-a3a2-4e7f956e222d",
                                    WinRtTypeSignature.guid("a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e"),
                                ),
                            )

                            assertFalse(pointer.isNull)
                            assertIndexOfResult(invokeObjectIndexOf(pointer, 10, secondStub.primaryPointer), true, 1u)
                            assertIndexOfResult(invokeObjectIndexOf(pointer, 10, ComPtr.NULL), false, 0u)

                            PlatformComInterop.invokeUnitMethodWithArgs(pointer, 11, 0u, replacementStub.primaryPointer).getOrThrow()
                            PlatformComInterop.invokeUnitMethodWithArgs(pointer, 12, 1u, insertedStub.primaryPointer).getOrThrow()
                            PlatformComInterop.invokeObjectSetter(pointer, 14, appendedStub.primaryPointer).getOrThrow()

                            val replacementValue: Any = values[0]
                            val insertedValue: Any = values[1]
                            val appendedValue: Any = values.last()

                            assertTrue(replacementValue is Inspectable)
                            assertTrue(insertedValue is Inspectable)
                            assertTrue(appendedValue is Inspectable)
                            assertEquals(
                                replacementStub.primaryPointer.value.rawValue,
                                replacementValue.pointer.value.rawValue,
                            )
                            assertEquals(
                                insertedStub.primaryPointer.value.rawValue,
                                insertedValue.pointer.value.rawValue,
                            )
                            assertEquals(
                                appendedStub.primaryPointer.value.rawValue,
                                appendedValue.pointer.value.rawValue,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_splits_imap_view_projection_from_mutable_map_like_cswinrt_on_jvm() {
        val values = linkedMapOf(
            "theme" to "dark",
            "accent" to "blue",
            "language" to "en-US",
        )
        val pointer = projectedObjectArgumentPointer(
            value = values,
            projectionTypeKey = "kotlin.collections.MutableMap<String, String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "3c2925fe-8519-45c1-aa79-197b6718c1c1",
                WinRtTypeSignature.string(),
                WinRtTypeSignature.string(),
            ),
        )

        val mapViewPointer = PlatformComInterop.queryInterface(
            pointer,
            ParameterizedInterfaceId.createFromSignature(
                WinRtTypeSignature.parameterizedInterface(
                    "e480ce40-a338-4ada-adcf-272272e48cb9",
                    WinRtTypeSignature.string(),
                    WinRtTypeSignature.string(),
                ),
            ),
        ).getOrThrow()
        try {
            val (first, second) = PlatformComInterop.invokeTwoObjectMethod(mapViewPointer, 9).getOrThrow()
            try {
                assertFalse(first.isNull)
                assertFalse(second.isNull)
                assertEquals(
                    listOf("accent", "language"),
                    readStringKeyedMapKeys(first, WinRtTypeSignature.string()),
                )
                assertEquals(
                    listOf("theme"),
                    readStringKeyedMapKeys(second, WinRtTypeSignature.string()),
                )
            } finally {
                if (!first.isNull) {
                    PlatformComInterop.release(first)
                }
                if (!second.isNull) {
                    PlatformComInterop.release(second)
                }
            }
        } finally {
            PlatformComInterop.release(mapViewPointer)
        }
    }

    @Test
    fun projected_object_argument_pointer_reprojects_mutable_runtime_class_map_values_for_imap_on_jvm() {
        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = IJsonValue.iid),
        ).use { firstStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = IJsonValue.iid),
            ).use { replacementStub ->
                val values = linkedMapOf("theme" to JsonValue(firstStub.primaryPointer))
                val pointer = projectedObjectArgumentPointer(
                    value = values,
                    projectionTypeKey = "kotlin.collections.MutableMap<String, Windows.Data.Json.JsonValue>",
                    signature = WinRtTypeSignature.parameterizedInterface(
                        "3c2925fe-8519-45c1-aa79-197b6718c1c1",
                        WinRtTypeSignature.string(),
                        WinRtTypeSignature.runtimeClass(
                            "Windows.Data.Json.JsonValue",
                            WinRtTypeSignature.guid("a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e"),
                        ),
                    ),
                )

                assertFalse(pointer.isNull)
                assertTrue(
                    PlatformComInterop.invokeMethodWithResultKind(
                        pointer,
                        10,
                        ComMethodResultKind.BOOLEAN,
                        "theme",
                        replacementStub.primaryPointer,
                    ).getOrThrow().requireBoolean(),
                )

                val replacementValue: Any = values.getValue("theme")
                assertTrue(replacementValue is JsonValue)
                assertEquals(
                    replacementStub.primaryPointer.value.rawValue,
                    replacementValue.pointer.value.rawValue,
                )
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_reprojects_mutable_parameterized_interface_map_values_for_imap_on_jvm() {
        val asyncOperationSignature = WinRtTypeSignature.parameterizedInterface(
            "9fc2b0bb-e446-44e2-aa61-9cab8f636af2",
            WinRtTypeSignature.string(),
        )
        val asyncOperationIid = ParameterizedInterfaceId.createFromSignature(asyncOperationSignature)

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = asyncOperationIid),
        ).use { firstStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = asyncOperationIid),
            ).use { replacementStub ->
                val values = linkedMapOf(
                    "job" to IAsyncOperation<String>(firstStub.primaryPointer, WinRtTypeSignature.string()),
                )
                val pointer = projectedObjectArgumentPointer(
                    value = values,
                    projectionTypeKey = "kotlin.collections.MutableMap<String, Windows.Foundation.IAsyncOperation<String>>",
                    signature = WinRtTypeSignature.parameterizedInterface(
                        "3c2925fe-8519-45c1-aa79-197b6718c1c1",
                        WinRtTypeSignature.string(),
                        asyncOperationSignature,
                    ),
                )

                assertFalse(pointer.isNull)
                assertTrue(
                    PlatformComInterop.invokeMethodWithResultKind(
                        pointer,
                        10,
                        ComMethodResultKind.BOOLEAN,
                        "job",
                        replacementStub.primaryPointer,
                    ).getOrThrow().requireBoolean(),
                )

                val replacementValue: Any = values.getValue("job")
                assertTrue(replacementValue is IAsyncOperation<*>)
                assertEquals(WinRtTypeSignature.string(), replacementValue.resultSignature)
                assertEquals(replacementStub.primaryPointer.value.rawValue, replacementValue.pointer.value.rawValue)
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_list_values_for_vector_view_on_jvm() {
        val pointer = projectedObjectArgumentPointer(
            value = listOf("en-US", "fr-FR"),
            projectionTypeKey = "kotlin.collections.List<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                WinRtTypeSignature.string(),
            ),
        )

        assertFalse(pointer.isNull)
        assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
        PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 7, 1u).getOrThrow().use { value ->
            assertEquals("fr-FR", value.toKotlinString())
        }
        assertIndexOfResult(invokeStringIndexOf(pointer, 9, "fr-FR"), true, 1u)
        assertIndexOfResult(invokeStringIndexOf(pointer, 9, "de-DE"), false, 0u)

        val iterablePointer = PlatformComInterop.queryInterface(
            pointer,
            ParameterizedInterfaceId.createFromSignature(
                WinRtTypeSignature.parameterizedInterface(
                    "faa585ea-6214-4217-afda-7f46de5869b3",
                    WinRtTypeSignature.string(),
                ),
            ),
        ).getOrThrow()
        PlatformComInterop.release(iterablePointer)

        val iterator = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
        try {
            PlatformComInterop.invokeHStringMethod(iterator, 6).getOrThrow().use { current ->
                assertEquals("en-US", current.toKotlinString())
            }
        } finally {
            PlatformComInterop.release(iterator)
        }
    }

    @Test
    fun projected_object_argument_pointer_vector_view_get_many_matches_cswinrt_on_jvm() {
        val pointer = projectedObjectArgumentPointer(
            value = listOf("en-US", "fr-FR"),
            projectionTypeKey = "kotlin.collections.List<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                WinRtTypeSignature.string(),
            ),
        )

        assertFalse(pointer.isNull)

        val firstBatch = invokeStringGetManyWithUInt32Arg(pointer, 10, 0u, 3).getOrThrow()
        assertEquals(2u, firstBatch.count)
        assertEquals(listOf("en-US", "fr-FR", ""), firstBatch.values)

        val exhaustedBatch = invokeStringGetManyWithUInt32Arg(pointer, 10, 2u, 2).getOrThrow()
        assertEquals(0u, exhaustedBatch.count)
        assertEquals(listOf("", ""), exhaustedBatch.values)

        assertBoundsFailure(invokeStringGetManyWithUInt32Arg(pointer, 10, 3u, 1))
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_mutable_string_list_values_for_vector_on_jvm() {
        val values = mutableListOf("en-US", "fr-FR")
        val pointer = projectedObjectArgumentPointer(
            value = values,
            projectionTypeKey = "kotlin.collections.MutableList<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "913337e9-11a1-4345-a3a2-4e7f956e222d",
                WinRtTypeSignature.string(),
            ),
        )

        assertFalse(pointer.isNull)
        assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
        PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 7, 1u).getOrThrow().use { value ->
            assertEquals("fr-FR", value.toKotlinString())
        }
        assertBoundsFailure(PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 7, 2u))
        assertBoundsFailure(PlatformComInterop.invokeUnitMethodWithUInt32AndStringArgs(pointer, 11, 2u, "de-DE"))
        assertBoundsFailure(PlatformComInterop.invokeUnitMethodWithUInt32AndStringArgs(pointer, 12, 3u, "de-DE"))
        assertBoundsFailure(PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 13, 2u))
        assertIndexOfResult(invokeStringIndexOf(pointer, 10, "fr-FR"), true, 1u)
        assertIndexOfResult(invokeStringIndexOf(pointer, 10, "de-DE"), false, 0u)

        PlatformComInterop.invokeUnitMethodWithUInt32AndStringArgs(pointer, 11, 0u, "de-DE").getOrThrow()
        assertEquals("de-DE", values[0])

        PlatformComInterop.invokeUnitMethodWithUInt32AndStringArgs(pointer, 12, 1u, "es-ES").getOrThrow()
        assertEquals(listOf("de-DE", "es-ES", "fr-FR"), values)

        PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 14, "ja-JP").getOrThrow()
        assertEquals(listOf("de-DE", "es-ES", "fr-FR", "ja-JP"), values)

        val vectorViewPointer = PlatformComInterop.queryInterface(
            pointer,
            ParameterizedInterfaceId.createFromSignature(
                WinRtTypeSignature.parameterizedInterface(
                    "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                    WinRtTypeSignature.string(),
                ),
            ),
        ).getOrThrow()
        try {
            assertEquals(4u, PlatformComInterop.invokeUInt32Method(vectorViewPointer, 8).getOrThrow())
        } finally {
            PlatformComInterop.release(vectorViewPointer)
        }

        val view = PlatformComInterop.invokeObjectMethod(pointer, 9).getOrThrow()
        try {
            PlatformComInterop.invokeHStringMethodWithUInt32Arg(view, 7, 2u).getOrThrow().use { value ->
                assertEquals("fr-FR", value.toKotlinString())
            }
        } finally {
            PlatformComInterop.release(view)
        }

        PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 13, 2u).getOrThrow()
        assertEquals(listOf("de-DE", "es-ES", "ja-JP"), values)

        PlatformComInterop.invokeUnitMethod(pointer, 15).getOrThrow()
        assertEquals(listOf("de-DE", "es-ES"), values)

        PlatformComInterop.invokeUnitMethod(pointer, 16).getOrThrow()
        assertTrue(values.isEmpty())
        assertBoundsFailure(PlatformComInterop.invokeUnitMethod(pointer, 15))
    }

    @Test
    fun projected_object_argument_pointer_vector_get_many_and_replace_all_match_cswinrt_on_jvm() {
        val values = mutableListOf("en-US", "fr-FR", "ja-JP")
        val pointer = projectedObjectArgumentPointer(
            value = values,
            projectionTypeKey = "kotlin.collections.MutableList<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "913337e9-11a1-4345-a3a2-4e7f956e222d",
                WinRtTypeSignature.string(),
            ),
        )

        assertFalse(pointer.isNull)

        val firstBatch = invokeStringGetManyWithUInt32Arg(pointer, 17, 1u, 3).getOrThrow()
        assertEquals(2u, firstBatch.count)
        assertEquals(listOf("fr-FR", "ja-JP", ""), firstBatch.values)

        val exhaustedBatch = invokeStringGetManyWithUInt32Arg(pointer, 17, 3u, 2).getOrThrow()
        assertEquals(0u, exhaustedBatch.count)
        assertEquals(listOf("", ""), exhaustedBatch.values)

        assertBoundsFailure(invokeStringGetManyWithUInt32Arg(pointer, 17, 4u, 1))

        invokeStringReplaceAll(pointer, 18, listOf("de-DE", "es-ES")).getOrThrow()
        assertEquals(listOf("de-DE", "es-ES"), values)
        assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
        PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 7, 1u).getOrThrow().use { value ->
            assertEquals("es-ES", value.toKotlinString())
        }
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_mutable_inspectable_list_values_for_vector_on_jvm() {
        val forwardedIid = guidOf("12345678-1111-2222-3333-444444444444")

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
        ).use { firstStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
            ).use { secondStub ->
                JvmWinRtObjectStub.create(
                    JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
                ).use { replacementStub ->
                    JvmWinRtObjectStub.create(
                        JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
                    ).use { insertedStub ->
                        JvmWinRtObjectStub.create(
                            JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
                        ).use { appendedStub ->
                            val values = mutableListOf(
                                Inspectable(firstStub.primaryPointer),
                                Inspectable(secondStub.primaryPointer),
                            )
                            val pointer = projectedObjectArgumentPointer(
                                value = values,
                                projectionTypeKey = "kotlin.collections.MutableList<Object>",
                                signature = WinRtTypeSignature.parameterizedInterface(
                                    "913337e9-11a1-4345-a3a2-4e7f956e222d",
                                    WinRtTypeSignature.object_(),
                                ),
                            )

                            assertFalse(pointer.isNull)
                            assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
                            assertIndexOfResult(invokeObjectIndexOf(pointer, 10, secondStub.primaryPointer), true, 1u)
                            assertIndexOfResult(invokeObjectIndexOf(pointer, 10, ComPtr.NULL), false, 0u)

                            PlatformComInterop.invokeUnitMethodWithArgs(pointer, 11, 0u, replacementStub.primaryPointer).getOrThrow()
                            assertEquals(replacementStub.primaryPointer.value.rawValue, values[0].pointer.value.rawValue)

                            PlatformComInterop.invokeUnitMethodWithArgs(pointer, 12, 1u, insertedStub.primaryPointer).getOrThrow()
                            assertEquals(3, values.size)
                            assertEquals(insertedStub.primaryPointer.value.rawValue, values[1].pointer.value.rawValue)

                            PlatformComInterop.invokeObjectSetter(pointer, 14, appendedStub.primaryPointer).getOrThrow()
                            assertEquals(4, values.size)
                            assertEquals(appendedStub.primaryPointer.value.rawValue, values.last().pointer.value.rawValue)

                            val vectorViewPointer = PlatformComInterop.queryInterface(
                                pointer,
                                ParameterizedInterfaceId.createFromSignature(
                                    WinRtTypeSignature.parameterizedInterface(
                                        "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                                        WinRtTypeSignature.object_(),
                                    ),
                                ),
                            ).getOrThrow()
                            try {
                                assertEquals(4u, PlatformComInterop.invokeUInt32Method(vectorViewPointer, 8).getOrThrow())
                            } finally {
                                PlatformComInterop.release(vectorViewPointer)
                            }

                            val view = PlatformComInterop.invokeObjectMethod(pointer, 9).getOrThrow()
                            try {
                                val third = PlatformComInterop.invokeObjectMethodWithUInt32Arg(view, 7, 2u).getOrThrow()
                                try {
                                    assertEquals(secondStub.primaryPointer.value.rawValue, third.value.rawValue)
                                } finally {
                                    PlatformComInterop.release(third)
                                }
                            } finally {
                                PlatformComInterop.release(view)
                            }

                            PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 13, 2u).getOrThrow()
                            assertEquals(3, values.size)

                            PlatformComInterop.invokeUnitMethod(pointer, 15).getOrThrow()
                            assertEquals(2, values.size)

                            PlatformComInterop.invokeUnitMethod(pointer, 16).getOrThrow()
                            assertTrue(values.isEmpty())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_list_values_for_bindable_vector_view_on_jvm() {
        val forwardedIid = guidOf("12345678-1111-2222-3333-444444444444")

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
        ).use { firstStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
            ).use { secondStub ->
                val values = listOf(Inspectable(firstStub.primaryPointer), Inspectable(secondStub.primaryPointer))
                val pointer = projectedObjectArgumentPointer(
                    value = values,
                    projectionTypeKey = "kotlin.collections.List",
                    signature = "{346dd6e7-976e-4bc3-815d-ece243bc0f33}",
                )

                assertFalse(pointer.isNull)
                assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
                assertIndexOfResult(invokeObjectIndexOf(pointer, 9, secondStub.primaryPointer), true, 1u)
                assertIndexOfResult(invokeObjectIndexOf(pointer, 9, ComPtr.NULL), false, 0u)

                val iterablePointer = PlatformComInterop.queryInterface(
                    pointer,
                    guidOf("036d2c08-df29-41af-8aa2-d774be62ba6f"),
                ).getOrThrow()
                PlatformComInterop.release(iterablePointer)

                val second = PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7, 1u).getOrThrow()
                try {
                    assertEquals(secondStub.primaryPointer.value.rawValue, second.value.rawValue)
                } finally {
                    PlatformComInterop.release(second)
                }

                val iterator = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
                try {
                    val current = PlatformComInterop.invokeObjectMethod(iterator, 6).getOrThrow()
                    try {
                        assertEquals(firstStub.primaryPointer.value.rawValue, current.value.rawValue)
                    } finally {
                        PlatformComInterop.release(current)
                    }
                } finally {
                    PlatformComInterop.release(iterator)
                }
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_mutable_list_values_for_bindable_vector_on_jvm() {
        val forwardedIid = guidOf("12345678-1111-2222-3333-444444444444")

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
        ).use { firstStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
            ).use { secondStub ->
                JvmWinRtObjectStub.create(
                    JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
                ).use { replacementStub ->
                    JvmWinRtObjectStub.create(
                        JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
                    ).use { insertedStub ->
                        JvmWinRtObjectStub.create(
                            JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
                        ).use { appendedStub ->
                            val values = mutableListOf(
                                Inspectable(firstStub.primaryPointer),
                                Inspectable(secondStub.primaryPointer),
                            )
                            val pointer = projectedObjectArgumentPointer(
                                value = values,
                                projectionTypeKey = "kotlin.collections.MutableList",
                                signature = "{393de7de-6fd0-4c0d-bb71-47244a113e93}",
                            )

                            assertFalse(pointer.isNull)
                            assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
                            assertIndexOfResult(invokeObjectIndexOf(pointer, 10, secondStub.primaryPointer), true, 1u)
                            assertIndexOfResult(invokeObjectIndexOf(pointer, 10, ComPtr.NULL), false, 0u)

                            PlatformComInterop.invokeUnitMethodWithArgs(pointer, 11, 0u, replacementStub.primaryPointer).getOrThrow()
                            assertEquals(replacementStub.primaryPointer.value.rawValue, values[0].pointer.value.rawValue)

                            PlatformComInterop.invokeUnitMethodWithArgs(pointer, 12, 1u, insertedStub.primaryPointer).getOrThrow()
                            assertEquals(3, values.size)
                            assertEquals(insertedStub.primaryPointer.value.rawValue, values[1].pointer.value.rawValue)

                            PlatformComInterop.invokeObjectSetter(pointer, 14, appendedStub.primaryPointer).getOrThrow()
                            assertEquals(4, values.size)
                            assertEquals(appendedStub.primaryPointer.value.rawValue, values.last().pointer.value.rawValue)

                            PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 13, 2u).getOrThrow()
                            assertEquals(3, values.size)

                            val view = PlatformComInterop.invokeObjectMethod(pointer, 9).getOrThrow()
                            try {
                                assertEquals(3u, PlatformComInterop.invokeUInt32Method(view, 8).getOrThrow())
                            } finally {
                                PlatformComInterop.release(view)
                            }

                            PlatformComInterop.invokeUnitMethod(pointer, 15).getOrThrow()
                            assertEquals(2, values.size)

                            PlatformComInterop.invokeUnitMethod(pointer, 16).getOrThrow()
                            assertTrue(values.isEmpty())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_accepts_primitive_list_values_for_vector_view_on_jvm() {
        assertPrimitiveVectorViewCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.WinRtBoolean",
                signature = "b1",
                initial = listOf(WinRtBoolean(false), WinRtBoolean(true)),
                first = false,
                second = true,
                replacement = true,
                inserted = false,
                appended = true,
                current = { pointer -> PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeBooleanMethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as WinRtBoolean).value },
            ),
        )
        assertPrimitiveVectorViewCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.Int32",
                signature = "i4",
                initial = listOf(Int32(1), Int32(2)),
                first = 1,
                second = 2,
                replacement = 7,
                inserted = 9,
                appended = 11,
                current = { pointer -> PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeInt32MethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as Int32).value },
            ),
        )
        assertPrimitiveVectorViewCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.UInt32",
                signature = "u4",
                initial = listOf(UInt32(1u), UInt32(2u)),
                first = 1u,
                second = 2u,
                replacement = 7u,
                inserted = 9u,
                appended = 11u,
                current = { pointer -> PlatformComInterop.invokeUInt32Method(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeUInt32MethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as UInt32).value },
            ),
        )
        assertPrimitiveVectorViewCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.Int64",
                signature = "i8",
                initial = listOf(Int64(1L), Int64(2L)),
                first = 1L,
                second = 2L,
                replacement = 7L,
                inserted = 9L,
                appended = 11L,
                current = { pointer -> PlatformComInterop.invokeInt64Method(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeInt64MethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as Int64).value },
            ),
        )
        assertPrimitiveVectorViewCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.UInt64",
                signature = "u8",
                initial = listOf(UInt64(1uL), UInt64(2uL)),
                first = 1uL,
                second = 2uL,
                replacement = 7uL,
                inserted = 9uL,
                appended = 11uL,
                current = { pointer -> PlatformComInterop.invokeUInt64Method(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeUInt64MethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as UInt64).value },
            ),
        )
        assertPrimitiveVectorViewCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.Float32",
                signature = "f4",
                initial = listOf(Float32(1.5f), Float32(2.5f)),
                first = 1.5f,
                second = 2.5f,
                replacement = 7.5f,
                inserted = 9.5f,
                appended = 11.5f,
                current = { pointer -> PlatformComInterop.invokeFloat32Method(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeFloat32MethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as Float32).value },
            ),
        )
        assertPrimitiveVectorViewCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.Float64",
                signature = "f8",
                initial = listOf(Float64(1.5), Float64(2.5)),
                first = 1.5,
                second = 2.5,
                replacement = 7.5,
                inserted = 9.5,
                appended = 11.5,
                current = { pointer -> PlatformComInterop.invokeFloat64Method(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeFloat64MethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as Float64).value },
            ),
        )
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_mutable_primitive_list_values_for_vector_on_jvm() {
        assertPrimitiveVectorCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.WinRtBoolean",
                signature = "b1",
                initial = listOf(WinRtBoolean(false), WinRtBoolean(true)),
                first = false,
                second = true,
                replacement = true,
                inserted = false,
                appended = true,
                current = { pointer -> PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeBooleanMethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as WinRtBoolean).value },
            ),
        )
        assertPrimitiveVectorCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.Int32",
                signature = "i4",
                initial = listOf(Int32(1), Int32(2)),
                first = 1,
                second = 2,
                replacement = 7,
                inserted = 9,
                appended = 11,
                current = { pointer -> PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeInt32MethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as Int32).value },
            ),
        )
        assertPrimitiveVectorCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.UInt32",
                signature = "u4",
                initial = listOf(UInt32(1u), UInt32(2u)),
                first = 1u,
                second = 2u,
                replacement = 7u,
                inserted = 9u,
                appended = 11u,
                current = { pointer -> PlatformComInterop.invokeUInt32Method(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeUInt32MethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as UInt32).value },
            ),
        )
        assertPrimitiveVectorCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.Int64",
                signature = "i8",
                initial = listOf(Int64(1L), Int64(2L)),
                first = 1L,
                second = 2L,
                replacement = 7L,
                inserted = 9L,
                appended = 11L,
                current = { pointer -> PlatformComInterop.invokeInt64Method(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeInt64MethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as Int64).value },
            ),
        )
        assertPrimitiveVectorCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.UInt64",
                signature = "u8",
                initial = listOf(UInt64(1uL), UInt64(2uL)),
                first = 1uL,
                second = 2uL,
                replacement = 7uL,
                inserted = 9uL,
                appended = 11uL,
                current = { pointer -> PlatformComInterop.invokeUInt64Method(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeUInt64MethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as UInt64).value },
            ),
        )
        assertPrimitiveVectorCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.Float32",
                signature = "f4",
                initial = listOf(Float32(1.5f), Float32(2.5f)),
                first = 1.5f,
                second = 2.5f,
                replacement = 7.5f,
                inserted = 9.5f,
                appended = 11.5f,
                current = { pointer -> PlatformComInterop.invokeFloat32Method(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeFloat32MethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as Float32).value },
            ),
        )
        assertPrimitiveVectorCase(
            PrimitiveVectorCase(
                projectionType = "dev.winrt.core.Float64",
                signature = "f8",
                initial = listOf(Float64(1.5), Float64(2.5)),
                first = 1.5,
                second = 2.5,
                replacement = 7.5,
                inserted = 9.5,
                appended = 11.5,
                current = { pointer -> PlatformComInterop.invokeFloat64Method(pointer, 6).getOrThrow() },
                getAt = { pointer, index -> PlatformComInterop.invokeFloat64MethodWithUInt32Arg(pointer, 7, index).getOrThrow() },
                unwrap = { value -> (value as Float64).value },
            ),
        )

        val int32VectorPointer = projectedObjectArgumentPointer(
            value = mutableListOf(Int32(1), Int32(2)),
            projectionTypeKey = "kotlin.collections.MutableList<dev.winrt.core.Int32>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "913337e9-11a1-4345-a3a2-4e7f956e222d",
                "i4",
            ),
        )
        assertBoundsFailure(PlatformComInterop.invokeInt32MethodWithUInt32Arg(int32VectorPointer, 7, 2u))
        assertBoundsFailure(PlatformComInterop.invokeUnitMethodWithArgs(int32VectorPointer, 11, 2u, 7))
        assertBoundsFailure(PlatformComInterop.invokeUnitMethodWithArgs(int32VectorPointer, 12, 3u, 7))
        assertBoundsFailure(PlatformComInterop.invokeUnitMethodWithUInt32Arg(int32VectorPointer, 13, 2u))
    }

    @Test
    fun projected_object_argument_pointer_primitive_vector_get_many_and_replace_all_match_cswinrt_on_jvm() {
        val values = mutableListOf(Int32(1), Int32(2), Int32(3))
        val pointer = projectedObjectArgumentPointer(
            value = values,
            projectionTypeKey = "kotlin.collections.MutableList<dev.winrt.core.Int32>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "913337e9-11a1-4345-a3a2-4e7f956e222d",
                "i4",
            ),
        )

        assertFalse(pointer.isNull)

        val firstBatch = invokeInt32GetManyWithUInt32Arg(pointer, 17, 1u, 2).getOrThrow()
        assertEquals(2u, firstBatch.count)
        assertEquals(listOf(2, 3), firstBatch.values)

        assertBoundsFailure(invokeInt32GetManyWithUInt32Arg(pointer, 17, 4u, 1))

        invokeInt32ReplaceAll(pointer, 18, intArrayOf(7, 9)).getOrThrow()
        assertEquals(listOf(7, 9), values.map(Int32::value))
        assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
        assertEquals(9, PlatformComInterop.invokeInt32MethodWithUInt32Arg(pointer, 7, 1u).getOrThrow())
    }

    @Test
    fun projected_object_argument_pointer_propagates_hresult_from_vector_index_of_on_jvm() {
        val values = object : MutableList<String> by mutableListOf("en-US", "fr-FR") {
            override fun indexOf(element: String): Int = throw WinRtException(KnownHResults.E_POINTER)
        }
        val pointer = projectedObjectArgumentPointer(
            value = values,
            projectionTypeKey = "kotlin.collections.MutableList<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "913337e9-11a1-4345-a3a2-4e7f956e222d",
                WinRtTypeSignature.string(),
            ),
        )

        assertFailureHResult(
            invokeStringIndexOf(pointer, 10, "en-US"),
            KnownHResults.E_POINTER.value,
        )
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_ireference_enum_values_on_jvm() {
        val pointer = projectedObjectArgumentPointer(
            value = IReference(ExampleMode.Beta),
            projectionTypeKey = "Windows.Foundation.IReference`1<Test.ExampleMode>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "61c17706-2d65-11e0-9ae8-d48564015472",
                WinRtTypeSignature.enum("Test.ExampleMode", "u4"),
            ),
        )

        assertFalse(pointer.isNull)
        assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 6).getOrThrow())
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_ireference_hresult_values_on_jvm() {
        val pointer = projectedObjectArgumentPointer(
            value = IReference(IllegalArgumentException()),
            projectionTypeKey = "Windows.Foundation.IReference`1<Exception>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "61c17706-2d65-11e0-9ae8-d48564015472",
                WinRtTypeSignature.struct("Windows.Foundation.HResult", "i4"),
            ),
        )

        assertFalse(pointer.isNull)
        assertEquals(KnownHResults.E_INVALIDARG.value, PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())
    }
}
