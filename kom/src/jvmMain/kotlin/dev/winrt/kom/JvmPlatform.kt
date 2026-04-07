package dev.winrt.kom

import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.nio.charset.StandardCharsets

actual object PlatformRuntime {
    actual val platformName: String = "jvm"
    actual val isWindows: Boolean = System.getProperty("os.name").contains("Windows", ignoreCase = true)
    actual val ffiBackend: String = "jdk22-ffm"
}

private enum class AbiArrayKind {
    COM_PTR,
    STRING,
}

private fun abiArrayKind(argument: Array<*>): AbiArrayKind? {
    return when (argument.javaClass.componentType) {
        ComPtr::class.java -> AbiArrayKind.COM_PTR
        String::class.java -> AbiArrayKind.STRING
        else -> when {
            argument.all { it is ComPtr } -> AbiArrayKind.COM_PTR
            argument.all { it is String } -> AbiArrayKind.STRING
            else -> null
        }
    }
}

internal fun methodArgumentLayout(argument: Any): MemoryLayout {
    return when (argument) {
        is ComPtr,
        is String,
        is ByteArray,
        is ShortArray,
        is CharArray,
        is IntArray,
        is LongArray,
        is FloatArray,
        is DoubleArray -> ValueLayout.ADDRESS
        is Array<*> -> when (abiArrayKind(argument)) {
            AbiArrayKind.COM_PTR,
            AbiArrayKind.STRING,
            -> ValueLayout.ADDRESS
            null -> throw IllegalArgumentException("Unsupported COM argument type: ${argument::class.qualifiedName}")
        }
        is Byte,
        is UByte -> ValueLayout.JAVA_BYTE
        is Short,
        is UShort -> ValueLayout.JAVA_SHORT
        is Char -> ValueLayout.JAVA_CHAR
        is Int,
        is UInt,
        is Boolean -> ValueLayout.JAVA_INT
        is Long,
        is ULong -> ValueLayout.JAVA_LONG
        is Float -> ValueLayout.JAVA_FLOAT
        is Double -> ValueLayout.JAVA_DOUBLE
        is ComStructValue -> Jdk22Foreign.structLayout(argument.layout)
        else -> throw IllegalArgumentException("Unsupported COM argument type: ${argument::class.qualifiedName}")
    }
}

internal data class PreparedAbiArguments(
    val values: List<Any>,
    val close: () -> Unit,
)

internal fun prepareAbiArguments(arguments: Array<out Any>): PreparedAbiArguments {
    val releasers = mutableListOf<() -> Unit>()
    val values = arguments.map { argument ->
        when (argument) {
            is ComPtr -> if (argument.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(argument)
            is String -> {
                val hString = JvmWinRtRuntime.createHString(argument)
                releasers += { JvmWinRtRuntime.releaseHString(hString) }
                MemorySegment.ofAddress(hString.raw)
            }
            is ByteArray -> MemorySegment.ofArray(argument)
            is ShortArray -> MemorySegment.ofArray(argument)
            is CharArray -> MemorySegment.ofArray(argument)
            is IntArray -> MemorySegment.ofArray(argument)
            is LongArray -> MemorySegment.ofArray(argument)
            is FloatArray -> MemorySegment.ofArray(argument)
            is DoubleArray -> MemorySegment.ofArray(argument)
            is Array<*> -> {
                val arena = Arena.ofConfined()
                releasers += { arena.close() }
                val segment = arena.allocate(MemoryLayout.sequenceLayout(argument.size.toLong(), ValueLayout.ADDRESS))
                when (abiArrayKind(argument)) {
                    AbiArrayKind.COM_PTR -> argument.forEachIndexed { index, value ->
                        val pointer = value as ComPtr
                        segment.setAtIndex(
                            ValueLayout.ADDRESS,
                            index.toLong(),
                            if (pointer.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(pointer),
                        )
                    }
                    AbiArrayKind.STRING -> {
                        val hStrings = argument.map { value -> JvmWinRtRuntime.createHString(value as String) }
                        releasers += {
                            hStrings.asReversed().forEach { hString ->
                                JvmWinRtRuntime.releaseHString(hString)
                            }
                        }
                        hStrings.forEachIndexed { index, hString ->
                            segment.setAtIndex(
                                ValueLayout.ADDRESS,
                                index.toLong(),
                                MemorySegment.ofAddress(hString.raw),
                            )
                        }
                    }
                    null -> throw IllegalArgumentException("Unsupported COM argument type: ${argument::class.qualifiedName}")
                }
                segment
            }
            is UByte -> argument.toByte()
            is UShort -> argument.toShort()
            is UInt -> argument.toInt()
            is Boolean -> if (argument) 1 else 0
            is ULong -> argument.toLong()
            is ComStructValue -> {
                val arena = Arena.ofConfined()
                releasers += { arena.close() }
                releasers += { argument.close() }
                val segment = arena.allocate(Jdk22Foreign.structLayout(argument.layout))
                segment.copyFrom(MemorySegment.ofArray(argument.bytes))
                segment
            }
            is Byte,
            is Short,
            is Char,
            is Int,
            is Long,
            is Float,
            is Double -> argument
            else -> throw IllegalArgumentException("Unsupported COM argument type: ${argument::class.qualifiedName}")
        }
    }
    return PreparedAbiArguments(
        values = values,
        close = {
            releasers.asReversed().forEach { release -> release() }
        },
    )
}

private object JvmComMethodExecutor {
    private fun requireInstance(instance: ComPtr) {
        require(!instance.isNull) { "Method invocation requires a non-null COM pointer" }
    }

    class DirectUnitInvocationScope internal constructor(
        val instance: ComPtr,
        val function: MemorySegment,
        val handle: java.lang.invoke.MethodHandle,
    )

    private fun pointerArgument(value: ComPtr): MemorySegment =
        if (value.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(value)

    private inline fun <T> withCreatedHString(value: String, block: (MemorySegment) -> T): T {
        val hString = JvmWinRtRuntime.createHString(value)
        try {
            return block(MemorySegment.ofAddress(hString.raw))
        } finally {
            JvmWinRtRuntime.releaseHString(hString)
        }
    }

    private inline fun runDirectWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        invoke: DirectUnitInvocationScope.() -> Int,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(DirectUnitInvocationScope(instance, function, handle).invoke())
            hresult.requireSuccess("$operation($vtableIndex)")
        }
    }

    class DirectIndexOfInvocationScope internal constructor(
        val instance: ComPtr,
        val function: MemorySegment,
        val handle: java.lang.invoke.MethodHandle,
    )

    private inline fun runDirectIndexOf(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        invoke: DirectIndexOfInvocationScope.(MemorySegment, MemorySegment) -> Int,
    ): Result<Pair<Boolean, UInt>> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val indexSegment = arena.allocate(ValueLayout.JAVA_INT)
                val foundSegment = arena.allocate(ValueLayout.JAVA_INT)
                val hresult = HResult(DirectIndexOfInvocationScope(instance, function, handle).invoke(indexSegment, foundSegment))
                hresult.requireSuccess("$operation($vtableIndex)")
                Pair(
                    foundSegment.get(ValueLayout.JAVA_INT, 0L) != 0,
                    indexSegment.get(ValueLayout.JAVA_INT, 0L).toUInt(),
                )
            }
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: ComPtr,
        second: Int,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                pointerArgument(first),
                second,
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Int,
        second: ComPtr,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                first,
                pointerArgument(second),
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: ComPtr,
        second: Long,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                pointerArgument(first),
                second,
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Long,
        second: ComPtr,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                first,
                pointerArgument(second),
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Int,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                value,
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: UInt,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                value.toInt(),
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Long,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                value,
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: ULong,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                value.toLong(),
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Boolean,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                if (value) 1 else 0,
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Float,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                value,
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Double,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                value,
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: String,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            withCreatedHString(value) { hString ->
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    hString,
                ) as Int
            }
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Int,
        second: Int,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                first,
                second,
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Int,
        second: Long,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                first,
                second,
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Long,
        second: Int,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                first,
                second,
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Long,
        second: Long,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                first,
                second,
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: String,
        second: Int,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            withCreatedHString(first) { firstHString ->
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    firstHString,
                    second,
                ) as Int
            }
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Int,
        second: String,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            withCreatedHString(second) { secondHString ->
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    first,
                    secondHString,
                ) as Int
            }
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: String,
        second: UInt,
    ): Result<Unit> = invokeWithoutOut(instance, vtableIndex, operation, handle, first, second.toInt())

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: UInt,
        second: String,
    ): Result<Unit> = invokeWithoutOut(instance, vtableIndex, operation, handle, first.toInt(), second)

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: String,
        second: Boolean,
    ): Result<Unit> = invokeWithoutOut(instance, vtableIndex, operation, handle, first, if (second) 1 else 0)

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Boolean,
        second: String,
    ): Result<Unit> = invokeWithoutOut(instance, vtableIndex, operation, handle, if (first) 1 else 0, second)

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: String,
        second: Long,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            withCreatedHString(first) { firstHString ->
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    firstHString,
                    second,
                ) as Int
            }
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Long,
        second: String,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            withCreatedHString(second) { secondHString ->
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    first,
                    secondHString,
                ) as Int
            }
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: String,
        second: String,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            withCreatedHString(first) { firstHString ->
                withCreatedHString(second) { secondHString ->
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        firstHString,
                        secondHString,
                    ) as Int
                }
            }
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: ComPtr,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                pointerArgument(value),
            ) as Int
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: ComPtr,
        second: String,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            withCreatedHString(second) { hString ->
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    pointerArgument(first),
                    hString,
                ) as Int
            }
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: String,
        second: ComPtr,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            withCreatedHString(first) { hString ->
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    hString,
                    pointerArgument(second),
                ) as Int
            }
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: ComPtr,
        second: ComPtr,
    ): Result<Unit> {
        return runDirectWithoutOut(instance, vtableIndex, operation, handle) {
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                pointerArgument(first),
                pointerArgument(second),
            ) as Int
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
    ): Result<T> {
        return runDirectWithOut(instance, vtableIndex, operation, handle, allocator, reader) { resultSegment ->
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                resultSegment,
            ) as Int
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        value: Int,
    ): Result<T> {
        return runDirectWithOut(instance, vtableIndex, operation, handle, allocator, reader) { resultSegment ->
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                value,
                resultSegment,
            ) as Int
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        value: UInt,
    ): Result<T> {
        return runDirectWithOut(instance, vtableIndex, operation, handle, allocator, reader) { resultSegment ->
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                value.toInt(),
                resultSegment,
            ) as Int
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        value: Long,
    ): Result<T> {
        return runDirectWithOut(instance, vtableIndex, operation, handle, allocator, reader) { resultSegment ->
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                value,
                resultSegment,
            ) as Int
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        value: ULong,
    ): Result<T> {
        return runDirectWithOut(instance, vtableIndex, operation, handle, allocator, reader) { resultSegment ->
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                value.toLong(),
                resultSegment,
            ) as Int
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        value: Boolean,
    ): Result<T> {
        return runDirectWithOut(instance, vtableIndex, operation, handle, allocator, reader) { resultSegment ->
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                if (value) 1 else 0,
                resultSegment,
            ) as Int
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        value: Float,
    ): Result<T> {
        return runDirectWithOut(instance, vtableIndex, operation, handle, allocator, reader) { resultSegment ->
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                value,
                resultSegment,
            ) as Int
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        value: Double,
    ): Result<T> {
        return runDirectWithOut(instance, vtableIndex, operation, handle, allocator, reader) { resultSegment ->
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                value,
                resultSegment,
            ) as Int
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        value: String,
    ): Result<T> {
        return runDirectWithOut(instance, vtableIndex, operation, handle, allocator, reader) { resultSegment ->
            withCreatedHString(value) { hString ->
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    hString,
                    resultSegment,
                ) as Int
            }
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        first: ComPtr,
        second: String,
    ): Result<T> {
        return runDirectWithOut(instance, vtableIndex, operation, handle, allocator, reader) { resultSegment ->
            withCreatedHString(second) { hString ->
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    pointerArgument(first),
                    hString,
                    resultSegment,
                ) as Int
            }
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        first: String,
        second: ComPtr,
    ): Result<T> {
        return runDirectWithOut(instance, vtableIndex, operation, handle, allocator, reader) { resultSegment ->
            withCreatedHString(first) { hString ->
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    hString,
                    pointerArgument(second),
                    resultSegment,
                ) as Int
            }
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        value: ComPtr,
    ): Result<T> {
        return runDirectWithOut(
            instance,
            vtableIndex,
            operation,
            handle,
            allocator,
            reader,
        ) { resultSegment ->
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                if (value.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(value),
                resultSegment,
            ) as Int
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        first: ComPtr,
        second: ComPtr,
    ): Result<T> {
        return runDirectWithOut(instance, vtableIndex, operation, handle, allocator, reader) { resultSegment ->
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                pointerArgument(first),
                pointerArgument(second),
                resultSegment,
            ) as Int
        }
    }

    private fun invokeWithoutOutArguments(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        arguments: Array<out Any>,
    ): Result<Unit> {
        return runWithPreparedArguments(instance, vtableIndex, arguments) { function, _, preparedArguments ->
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    *preparedArguments.values.toTypedArray(),
                ) as Int,
            )
            hresult.requireSuccess("$operation($vtableIndex)")
        }
    }

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        vararg arguments: Any,
    ): Result<Unit> {
        return invokeWithoutOutArguments(instance, vtableIndex, operation, handle, arguments)
    }

    private inline fun <T> runWithPreparedArguments(
        instance: ComPtr,
        vtableIndex: Int,
        arguments: Array<out Any>,
        block: (MemorySegment, Arena, PreparedAbiArguments) -> T,
    ): Result<T> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val preparedArguments = prepareAbiArguments(arguments)
                try {
                    block(function, arena, preparedArguments)
                } finally {
                    preparedArguments.close()
                }
            }
        }
    }

    private fun <T> invokeWithOutSegmentArguments(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        arguments: Array<out Any>,
    ): Result<T> {
        return runWithPreparedArguments(instance, vtableIndex, arguments) { function, arena, preparedArguments ->
                val resultSegment = allocator(arena)
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        *preparedArguments.values.toTypedArray(),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
                reader(resultSegment)
        }
    }

    fun <T> invokeWithOutSegment(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        vararg arguments: Any,
    ): Result<T> {
        return invokeWithOutSegmentArguments(instance, vtableIndex, operation, handle, allocator, reader, arguments)
    }

    private fun invokeWithOutResultKindArguments(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        arguments: Array<out Any>,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            *arguments,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        vararg arguments: Any,
    ): Result<ComMethodResult> {
        return invokeWithOutResultKindArguments(instance, vtableIndex, operation, handle, resultKind, arguments)
    }

    fun invokeIndexOfMethod(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        vararg arguments: Any,
    ): Result<Pair<Boolean, UInt>> {
        return runWithPreparedArguments(instance, vtableIndex, arguments) { function, arena, preparedArguments ->
                val indexSegment = arena.allocate(ValueLayout.JAVA_INT)
                val foundSegment = arena.allocate(ValueLayout.JAVA_INT)
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        *preparedArguments.values.toTypedArray(),
                        indexSegment,
                        foundSegment,
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
                Pair(
                    foundSegment.get(ValueLayout.JAVA_INT, 0L) != 0,
                    indexSegment.get(ValueLayout.JAVA_INT, 0L).toUInt(),
                )
        }
    }

    fun invokeIndexOfMethod(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Int,
    ): Result<Pair<Boolean, UInt>> = runDirectIndexOf(instance, vtableIndex, operation, handle) { indexSegment, foundSegment ->
        handle.bindTo(function).invokeWithArguments(
            Jdk22Foreign.pointerOf(instance),
            value,
            indexSegment,
            foundSegment,
        ) as Int
    }

    fun invokeIndexOfMethod(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Long,
    ): Result<Pair<Boolean, UInt>> = runDirectIndexOf(instance, vtableIndex, operation, handle) { indexSegment, foundSegment ->
        handle.bindTo(function).invokeWithArguments(
            Jdk22Foreign.pointerOf(instance),
            value,
            indexSegment,
            foundSegment,
        ) as Int
    }

    fun invokeIndexOfMethod(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Float,
    ): Result<Pair<Boolean, UInt>> = runDirectIndexOf(instance, vtableIndex, operation, handle) { indexSegment, foundSegment ->
        handle.bindTo(function).invokeWithArguments(
            Jdk22Foreign.pointerOf(instance),
            value,
            indexSegment,
            foundSegment,
        ) as Int
    }

    fun invokeIndexOfMethod(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Double,
    ): Result<Pair<Boolean, UInt>> = runDirectIndexOf(instance, vtableIndex, operation, handle) { indexSegment, foundSegment ->
        handle.bindTo(function).invokeWithArguments(
            Jdk22Foreign.pointerOf(instance),
            value,
            indexSegment,
            foundSegment,
        ) as Int
    }

    fun invokeIndexOfMethod(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: ComPtr,
    ): Result<Pair<Boolean, UInt>> = runDirectIndexOf(instance, vtableIndex, operation, handle) { indexSegment, foundSegment ->
        handle.bindTo(function).invokeWithArguments(
            Jdk22Foreign.pointerOf(instance),
            pointerArgument(value),
            indexSegment,
            foundSegment,
        ) as Int
    }

    fun invokeIndexOfMethod(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: String,
    ): Result<Pair<Boolean, UInt>> = runDirectIndexOf(instance, vtableIndex, operation, handle) { indexSegment, foundSegment ->
        withCreatedHString(value) { hString ->
            handle.bindTo(function).invokeWithArguments(
                Jdk22Foreign.pointerOf(instance),
                hString,
                indexSegment,
                foundSegment,
            ) as Int
        }
    }

    private fun <T> runDirectWithOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        invoke: DirectOutInvocationScope.(MemorySegment) -> Int,
    ): Result<T> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val resultSegment = allocator(arena)
                val hresult = HResult(DirectOutInvocationScope(instance, function, handle).invoke(resultSegment))
                hresult.requireSuccess("$operation($vtableIndex)")
                reader(resultSegment)
            }
        }
    }

    class DirectOutInvocationScope internal constructor(
        val instance: ComPtr,
        val function: MemorySegment,
        val handle: java.lang.invoke.MethodHandle,
    )

}

private fun allocateResultSegment(arena: Arena, resultKind: ComMethodResultKind): MemorySegment {
    return when (resultKind) {
        ComMethodResultKind.HSTRING, ComMethodResultKind.OBJECT -> arena.allocate(ValueLayout.ADDRESS)
        ComMethodResultKind.UINT8 -> arena.allocate(ValueLayout.JAVA_BYTE)
        ComMethodResultKind.INT16, ComMethodResultKind.UINT16 -> arena.allocate(ValueLayout.JAVA_SHORT)
        ComMethodResultKind.CHAR16 -> arena.allocate(ValueLayout.JAVA_CHAR)
        ComMethodResultKind.INT32, ComMethodResultKind.UINT32, ComMethodResultKind.BOOLEAN -> arena.allocate(ValueLayout.JAVA_INT)
        ComMethodResultKind.INT64, ComMethodResultKind.UINT64 -> arena.allocate(ValueLayout.JAVA_LONG)
        ComMethodResultKind.FLOAT32 -> arena.allocate(ValueLayout.JAVA_FLOAT)
        ComMethodResultKind.FLOAT64 -> arena.allocate(ValueLayout.JAVA_DOUBLE)
        ComMethodResultKind.GUID -> arena.allocate(16)
    }
}

private fun readResult(segment: MemorySegment, resultKind: ComMethodResultKind): ComMethodResult {
    return when (resultKind) {
        ComMethodResultKind.HSTRING -> ComMethodResult.HStringValue(HString(segment.get(ValueLayout.ADDRESS, 0L).address()))
        ComMethodResultKind.OBJECT -> ComMethodResult.ObjectValue(Jdk22Foreign.addressResult(segment.get(ValueLayout.ADDRESS, 0L)))
        ComMethodResultKind.UINT8 -> ComMethodResult.UInt8Value(segment.get(ValueLayout.JAVA_BYTE, 0L).toUByte())
        ComMethodResultKind.INT16 -> ComMethodResult.Int16Value(segment.get(ValueLayout.JAVA_SHORT, 0L))
        ComMethodResultKind.UINT16 -> ComMethodResult.UInt16Value(segment.get(ValueLayout.JAVA_SHORT, 0L).toUShort())
        ComMethodResultKind.CHAR16 -> ComMethodResult.Char16Value(segment.get(ValueLayout.JAVA_CHAR, 0L))
        ComMethodResultKind.INT32 -> ComMethodResult.Int32Value(segment.get(ValueLayout.JAVA_INT, 0L))
        ComMethodResultKind.UINT32 -> ComMethodResult.UInt32Value(segment.get(ValueLayout.JAVA_INT, 0L).toUInt())
        ComMethodResultKind.BOOLEAN -> ComMethodResult.BooleanValue(segment.get(ValueLayout.JAVA_INT, 0L) != 0)
        ComMethodResultKind.INT64 -> ComMethodResult.Int64Value(segment.get(ValueLayout.JAVA_LONG, 0L))
        ComMethodResultKind.UINT64 -> ComMethodResult.UInt64Value(segment.get(ValueLayout.JAVA_LONG, 0L).toULong())
        ComMethodResultKind.FLOAT32 -> ComMethodResult.Float32Value(segment.get(ValueLayout.JAVA_FLOAT, 0L))
        ComMethodResultKind.FLOAT64 -> ComMethodResult.Float64Value(segment.get(ValueLayout.JAVA_DOUBLE, 0L))
        ComMethodResultKind.GUID -> ComMethodResult.GuidValue(Jdk22Foreign.guidFromSegment(segment))
    }
}

private fun resultAllocator(resultKind: ComMethodResultKind): (Arena) -> MemorySegment =
    { arena -> allocateResultSegment(arena, resultKind) }

private fun resultReader(resultKind: ComMethodResultKind): (MemorySegment) -> ComMethodResult =
    { segment -> readResult(segment, resultKind) }

private object JvmPlatformComInterop : ComInterop {
    private val twoAddressUnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    }

    private val stringInt32UnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
    }

    private val int32StringUnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
    }

    private val stringInt64UnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
    }

    private val int64StringUnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
    }

    private val twoInt32UnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
    }

    private val int32Int64UnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG)
    }

    private val int64Int32UnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT)
    }

    private val twoInt64UnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
    }

    private val addressInt32UnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
    }

    private val int32AddressUnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
    }

    private val addressInt64UnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
    }

    private val int64AddressUnitHandle by lazy {
        Jdk22Foreign.unitMethodWithTwoInputsHandle(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
    }

    private val twoAddressOutHandle by lazy {
        Jdk22Foreign.methodWithTwoInputsHandle(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    }

    private fun allocateStructSegment(arena: Arena, layout: ComStructLayout): MemorySegment =
        arena.allocate(Jdk22Foreign.structLayout(layout))

    private fun readStructValue(segment: MemorySegment, layout: ComStructLayout): ComStructValue {
        val bytes = ByteArray(layout.byteSize)
        MemorySegment.ofArray(bytes).copyFrom(segment.asSlice(0, layout.byteSize.toLong()))
        return ComStructValue(layout, bytes)
    }

    private val addressInt32OutHandle by lazy {
        Jdk22Foreign.methodWithTwoInputsHandle(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
    }

    private val int32AddressOutHandle by lazy {
        Jdk22Foreign.methodWithTwoInputsHandle(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
    }

    private val addressInt64OutHandle by lazy {
        Jdk22Foreign.methodWithTwoInputsHandle(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
    }

    private val int64AddressOutHandle by lazy {
        Jdk22Foreign.methodWithTwoInputsHandle(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
    }

    private val addressIndexOfHandle by lazy {
        Jdk22Foreign.methodWithArgumentsAndTwoOutHandles(listOf(ValueLayout.ADDRESS))
    }

    private val int32IndexOfHandle by lazy {
        Jdk22Foreign.methodWithArgumentsAndTwoOutHandles(listOf(ValueLayout.JAVA_INT))
    }

    private val int64IndexOfHandle by lazy {
        Jdk22Foreign.methodWithArgumentsAndTwoOutHandles(listOf(ValueLayout.JAVA_LONG))
    }

    private val float32IndexOfHandle by lazy {
        Jdk22Foreign.methodWithArgumentsAndTwoOutHandles(listOf(ValueLayout.JAVA_FLOAT))
    }

    private val float64IndexOfHandle by lazy {
        Jdk22Foreign.methodWithArgumentsAndTwoOutHandles(listOf(ValueLayout.JAVA_DOUBLE))
    }

    private fun invokeRawI32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
    ): Result<Int> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
        reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L) },
    )

    private fun invokeRawI32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Int,
    ): Result<Int> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
        reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L) },
        value,
    )

    private fun invokeRawI32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Long,
    ): Result<Int> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
        reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L) },
        value,
    )

    private fun invokeRawI32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: ComPtr,
    ): Result<Int> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
        reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L) },
        value,
    )

    private fun invokeRawI32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: String,
    ): Result<Int> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
        reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L) },
        value,
    )

    private fun invokeRawI64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
    ): Result<Long> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
        reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L) },
    )

    private fun invokeRawI64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Int,
    ): Result<Long> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
        reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L) },
        value,
    )

    private fun invokeRawI64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Long,
    ): Result<Long> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
        reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L) },
        value,
    )

    private fun invokeRawI64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: ComPtr,
    ): Result<Long> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
        reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L) },
        value,
    )

    private fun invokeRawI64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: String,
    ): Result<Long> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
        reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L) },
        value,
    )

    private fun invokeRawF32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
    ): Result<Float> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_FLOAT) },
        reader = { segment -> segment.get(ValueLayout.JAVA_FLOAT, 0L) },
    )

    private fun invokeRawF32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Int,
    ): Result<Float> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_FLOAT) },
        reader = { segment -> segment.get(ValueLayout.JAVA_FLOAT, 0L) },
        value,
    )

    private fun invokeRawF32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: UInt,
    ): Result<Float> = invokeRawF32Result(instance, vtableIndex, operation, handle, value.toInt())

    private fun invokeRawF32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Boolean,
    ): Result<Float> = invokeRawF32Result(instance, vtableIndex, operation, handle, if (value) 1 else 0)

    private fun invokeRawF32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Long,
    ): Result<Float> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_FLOAT) },
        reader = { segment -> segment.get(ValueLayout.JAVA_FLOAT, 0L) },
        value,
    )

    private fun invokeRawF32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: ComPtr,
    ): Result<Float> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_FLOAT) },
        reader = { segment -> segment.get(ValueLayout.JAVA_FLOAT, 0L) },
        value,
    )

    private fun invokeRawF32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: String,
    ): Result<Float> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_FLOAT) },
        reader = { segment -> segment.get(ValueLayout.JAVA_FLOAT, 0L) },
        value,
    )

    private fun invokeRawF64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
    ): Result<Double> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_DOUBLE) },
        reader = { segment -> segment.get(ValueLayout.JAVA_DOUBLE, 0L) },
    )

    private fun invokeRawF64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Int,
    ): Result<Double> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_DOUBLE) },
        reader = { segment -> segment.get(ValueLayout.JAVA_DOUBLE, 0L) },
        value,
    )

    private fun invokeRawF64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: UInt,
    ): Result<Double> = invokeRawF64Result(instance, vtableIndex, operation, handle, value.toInt())

    private fun invokeRawF64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Boolean,
    ): Result<Double> = invokeRawF64Result(instance, vtableIndex, operation, handle, if (value) 1 else 0)

    private fun invokeRawF64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Long,
    ): Result<Double> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_DOUBLE) },
        reader = { segment -> segment.get(ValueLayout.JAVA_DOUBLE, 0L) },
        value,
    )

    private fun invokeRawF64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: ComPtr,
    ): Result<Double> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_DOUBLE) },
        reader = { segment -> segment.get(ValueLayout.JAVA_DOUBLE, 0L) },
        value,
    )

    private fun invokeRawF64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: String,
    ): Result<Double> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.JAVA_DOUBLE) },
        reader = { segment -> segment.get(ValueLayout.JAVA_DOUBLE, 0L) },
        value,
    )

    private fun invokeRawAddressResult(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
    ): Result<MemorySegment> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.ADDRESS) },
        reader = { segment -> segment.get(ValueLayout.ADDRESS, 0L) },
    )

    private fun invokeRawAddressResult(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Int,
    ): Result<MemorySegment> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.ADDRESS) },
        reader = { segment -> segment.get(ValueLayout.ADDRESS, 0L) },
        value,
    )

    private fun invokeRawAddressResult(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Long,
    ): Result<MemorySegment> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.ADDRESS) },
        reader = { segment -> segment.get(ValueLayout.ADDRESS, 0L) },
        value,
    )

    private fun invokeRawAddressResult(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: ComPtr,
    ): Result<MemorySegment> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.ADDRESS) },
        reader = { segment -> segment.get(ValueLayout.ADDRESS, 0L) },
        value,
    )

    private fun invokeRawAddressResult(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: String,
    ): Result<MemorySegment> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(ValueLayout.ADDRESS) },
        reader = { segment -> segment.get(ValueLayout.ADDRESS, 0L) },
        value,
    )

    private fun invokeRawGuidResult(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
    ): Result<Guid> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(16) },
        reader = Jdk22Foreign::guidFromSegment,
    )

    private fun invokeRawGuidResult(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Int,
    ): Result<Guid> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(16) },
        reader = Jdk22Foreign::guidFromSegment,
        value,
    )

    private fun invokeRawGuidResult(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: UInt,
    ): Result<Guid> = invokeRawGuidResult(instance, vtableIndex, operation, handle, value.toInt())

    private fun invokeRawGuidResult(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Long,
    ): Result<Guid> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(16) },
        reader = Jdk22Foreign::guidFromSegment,
        value,
    )

    private fun invokeRawGuidResult(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: ComPtr,
    ): Result<Guid> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(16) },
        reader = Jdk22Foreign::guidFromSegment,
        value,
    )

    private fun invokeRawGuidResult(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: String,
    ): Result<Guid> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = { arena -> arena.allocate(16) },
        reader = Jdk22Foreign::guidFromSegment,
        value,
    )

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
    )

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Int,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        value,
    )

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Long,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        value,
    )

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: ComPtr,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        value,
    )

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: String,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        value,
    )

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Float,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        value,
    )

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Double,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        value,
    )

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Int,
        second: Int,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Int,
        second: Long,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Long,
        second: Int,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Long,
        second: Long,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: ComPtr,
        second: Int,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Int,
        second: ComPtr,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: ComPtr,
        second: Long,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Long,
        second: ComPtr,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: String,
        second: Int,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Int,
        second: String,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: String,
        second: Long,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: Long,
        second: String,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: String,
        second: String,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: ComPtr,
        second: String,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: String,
        second: ComPtr,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        first: ComPtr,
        second: ComPtr,
    ): Result<Unit> = JvmComMethodExecutor.invokeWithoutOut(instance, vtableIndex, operation, handle, first, second)

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: String,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: Int,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Int,
        second: ComPtr,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: Long,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: String,
        second: Int,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Int,
        second: String,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: String,
        second: Long,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Long,
        second: String,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Int,
        second: Int,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Int,
        second: Long,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Long,
        second: Int,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Long,
        second: Long,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Long,
        second: ComPtr,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: String,
        second: ComPtr,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    private fun invokeRawResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: ComPtr,
    ): Result<ComMethodResult> = JvmComMethodExecutor.invokeWithOutSegment(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = operation,
        handle = handle,
        allocator = resultAllocator(resultKind),
        reader = resultReader(resultKind),
        first,
        second,
    )

    override fun queryInterface(instance: ComPtr, iid: Guid): Result<ComPtr> {
        if (instance.isNull) {
            return Result.failure(KomException("QueryInterface requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val iidSegment = Jdk22Foreign.guidSegment(iid, arena)
                val resultSegment = arena.allocate(ValueLayout.ADDRESS)
                val function = Jdk22Foreign.vtableEntry(instance, 0)
                val hresult = HResult(
                    Jdk22Foreign.queryInterfaceHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        iidSegment,
                        resultSegment,
                    ) as Int,
                )

                if (!hresult.isSuccess) {
                    throw KomException("QueryInterface failed with HRESULT=0x${hresult.value.toUInt().toString(16)}")
                }

                Jdk22Foreign.addressResult(resultSegment.get(ValueLayout.ADDRESS, 0L))
            }
        }
    }

    override fun addRef(instance: ComPtr): UInt {
        if (instance.isNull) {
            return 0u
        }

        val function = Jdk22Foreign.vtableEntry(instance, 1)
        val result = Jdk22Foreign.addRefHandle.bindTo(function).invokeWithArguments(
            Jdk22Foreign.pointerOf(instance),
        ) as Int
        return Jdk22Foreign.longToUInt(result)
    }

    override fun release(instance: ComPtr): UInt {
        if (instance.isNull) {
            return 0u
        }

        val function = Jdk22Foreign.vtableEntry(instance, 2)
        val result = Jdk22Foreign.releaseHandle.bindTo(function).invokeWithArguments(
            Jdk22Foreign.pointerOf(instance),
        ) as Int
        return Jdk22Foreign.longToUInt(result)
    }

    override fun invokeUnitMethod(instance: ComPtr, vtableIndex: Int): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethod", Jdk22Foreign.unitMethodHandle)
    }

    override fun invokeUnitMethodWithArgs(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUnitMethodWithArgs",
            handle = Jdk22Foreign.unitMethodWithArgumentsHandle(arguments.map(::methodArgumentLayout)),
            *arguments,
        )
    }

    override fun invokeRawUnitMethodWithI32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeRawUnitMethodWithI32Arg", Jdk22Foreign.unitMethodWithInt32Handle, value)
    }

    override fun invokeRawUnitMethodWithI64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeRawUnitMethodWithI64Arg", Jdk22Foreign.unitMethodWithInt64Handle, value)
    }

    override fun invokeUnitMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithStringArg", Jdk22Foreign.hstringSetterHandle, value)
    }

    override fun invokeUnitMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithObjectArg", Jdk22Foreign.objectSetterHandle, value)
    }

    override fun invokeUnitMethodWithFloat32Arg(instance: ComPtr, vtableIndex: Int, value: Float): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithFloat32Arg", Jdk22Foreign.float32SetterHandle, value)
    }

    override fun invokeUnitMethodWithFloat64Arg(instance: ComPtr, vtableIndex: Int, value: Double): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithFloat64Arg", Jdk22Foreign.float64SetterHandle, value)
    }

    override fun invokeUnitMethodWithTwoInt32Args(instance: ComPtr, vtableIndex: Int, first: Int, second: Int): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithTwoInt32Args", twoInt32UnitHandle, first, second)
    }

    override fun invokeUnitMethodWithInt32AndInt64Args(instance: ComPtr, vtableIndex: Int, first: Int, second: Long): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithInt32AndInt64Args", int32Int64UnitHandle, first, second)
    }

    override fun invokeUnitMethodWithInt64AndInt32Args(instance: ComPtr, vtableIndex: Int, first: Long, second: Int): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithInt64AndInt32Args", int64Int32UnitHandle, first, second)
    }

    override fun invokeUnitMethodWithTwoInt64Args(instance: ComPtr, vtableIndex: Int, first: Long, second: Long): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithTwoInt64Args", twoInt64UnitHandle, first, second)
    }

    override fun invokeUnitMethodWithObjectAndInt32Args(instance: ComPtr, vtableIndex: Int, first: ComPtr, second: Int): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithObjectAndInt32Args", addressInt32UnitHandle, first, second)
    }

    override fun invokeUnitMethodWithInt32AndObjectArgs(instance: ComPtr, vtableIndex: Int, first: Int, second: ComPtr): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithInt32AndObjectArgs", int32AddressUnitHandle, first, second)
    }

    override fun invokeUnitMethodWithObjectAndInt64Args(instance: ComPtr, vtableIndex: Int, first: ComPtr, second: Long): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithObjectAndInt64Args", addressInt64UnitHandle, first, second)
    }

    override fun invokeUnitMethodWithInt64AndObjectArgs(instance: ComPtr, vtableIndex: Int, first: Long, second: ComPtr): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithInt64AndObjectArgs", int64AddressUnitHandle, first, second)
    }

    override fun invokeUnitMethodWithStringAndInt32Args(instance: ComPtr, vtableIndex: Int, first: String, second: Int): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithStringAndInt32Args", stringInt32UnitHandle, first, second)
    }

    override fun invokeUnitMethodWithInt32AndStringArgs(instance: ComPtr, vtableIndex: Int, first: Int, second: String): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithInt32AndStringArgs", int32StringUnitHandle, first, second)
    }

    override fun invokeUnitMethodWithStringAndInt64Args(instance: ComPtr, vtableIndex: Int, first: String, second: Long): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithStringAndInt64Args", stringInt64UnitHandle, first, second)
    }

    override fun invokeUnitMethodWithInt64AndStringArgs(instance: ComPtr, vtableIndex: Int, first: Long, second: String): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithInt64AndStringArgs", int64StringUnitHandle, first, second)
    }

    override fun invokeUnitMethodWithTwoStringArgs(
        instance: ComPtr,
        vtableIndex: Int,
        first: String,
        second: String,
    ): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithTwoStringArgs", twoAddressUnitHandle, first, second)
    }

    override fun invokeUnitMethodWithObjectAndStringArgs(
        instance: ComPtr,
        vtableIndex: Int,
        first: ComPtr,
        second: String,
    ): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithObjectAndStringArgs", twoAddressOutHandle, first, second)
    }

    override fun invokeUnitMethodWithStringAndObjectArgs(
        instance: ComPtr,
        vtableIndex: Int,
        first: String,
        second: ComPtr,
    ): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithStringAndObjectArgs", twoAddressOutHandle, first, second)
    }

    override fun invokeUnitMethodWithTwoObjectArgs(
        instance: ComPtr,
        vtableIndex: Int,
        first: ComPtr,
        second: ComPtr,
    ): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithTwoObjectArgs", twoAddressUnitHandle, first, second)
    }

    override fun invokeRawAddressMethod(instance: ComPtr, vtableIndex: Int): Result<AbiIntPtr> =
        invokeRawAddressResult(instance, vtableIndex, "invokeRawAddressMethod", Jdk22Foreign.objectMethodHandle)
            .map { segment -> AbiIntPtr(segment.address()) }

    override fun invokeRawAddressMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<AbiIntPtr> =
        invokeRawAddressResult(instance, vtableIndex, "invokeRawAddressMethodWithStringArg", Jdk22Foreign.objectMethodWithInputHandle, value)
            .map { segment -> AbiIntPtr(segment.address()) }

    override fun invokeRawAddressMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<AbiIntPtr> =
        invokeRawAddressResult(instance, vtableIndex, "invokeRawAddressMethodWithInt32Arg", Jdk22Foreign.hstringMethodWithInt32Handle, value)
            .map { segment -> AbiIntPtr(segment.address()) }

    override fun invokeRawAddressMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<AbiIntPtr> =
        invokeRawAddressResult(instance, vtableIndex, "invokeRawAddressMethodWithObjectArg", Jdk22Foreign.objectMethodWithInputHandle, value)
            .map { segment -> AbiIntPtr(segment.address()) }

    override fun invokeTwoObjectMethod(instance: ComPtr, vtableIndex: Int): Result<Pair<ComPtr, ComPtr>> {
        return runCatching {
            require(!instance.isNull) { "Method invocation requires a non-null COM pointer" }
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val firstSegment = arena.allocate(ValueLayout.ADDRESS)
                val secondSegment = arena.allocate(ValueLayout.ADDRESS)
                val hresult = HResult(
                    Jdk22Foreign.twoObjectMethodHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        firstSegment,
                        secondSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeTwoObjectMethod($vtableIndex)")
                Jdk22Foreign.addressResult(firstSegment.get(ValueLayout.ADDRESS, 0L)) to
                    Jdk22Foreign.addressResult(secondSegment.get(ValueLayout.ADDRESS, 0L))
            }
        }
    }

    override fun invokeObjectMethodWithArgs(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ComPtr> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeObjectMethodWithArgs",
            handle = Jdk22Foreign.methodWithArgumentsHandle(arguments.map(::methodArgumentLayout)),
            allocator = { arena -> arena.allocate(ValueLayout.ADDRESS) },
            reader = { segment -> Jdk22Foreign.addressResult(segment.get(ValueLayout.ADDRESS, 0L)) },
            *arguments,
        )
    }

    override fun invokeRawAddressMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<AbiIntPtr> =
        invokeRawAddressResult(instance, vtableIndex, "invokeRawAddressMethodWithInt64Arg", Jdk22Foreign.objectMethodWithInt64Handle, value)
            .map { segment -> AbiIntPtr(segment.address()) }

    override fun invokeStructMethodWithArgs(
        instance: ComPtr,
        vtableIndex: Int,
        layout: ComStructLayout,
        vararg arguments: Any,
    ): Result<ComStructValue> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeStructMethodWithArgs",
            handle = Jdk22Foreign.methodWithArgumentsHandle(arguments.map(::methodArgumentLayout)),
            allocator = { arena -> allocateStructSegment(arena, layout) },
            reader = { segment -> readStructValue(segment, layout) },
            *arguments,
        )
    }

    override fun invokeMethodWithObjectAndStringArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: String,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithObjectAndStringArgs", twoAddressOutHandle, resultKind, first, second)
    }

    override fun invokeMethodWithObjectAndInt32Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: Int,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithObjectAndInt32Args", addressInt32OutHandle, resultKind, first, second)
    }

    override fun invokeMethodWithInt32AndObjectArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Int,
        second: ComPtr,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithInt32AndObjectArgs", int32AddressOutHandle, resultKind, first, second)
    }

    override fun invokeMethodWithObjectAndInt64Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: Long,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithObjectAndInt64Args", addressInt64OutHandle, resultKind, first, second)
    }

    override fun invokeMethodWithStringAndInt32Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: String,
        second: Int,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithStringAndInt32Args", stringInt32UnitHandle, resultKind, first, second)
    }

    override fun invokeMethodWithInt32AndStringArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Int,
        second: String,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithInt32AndStringArgs", int32StringUnitHandle, resultKind, first, second)
    }

    override fun invokeMethodWithStringAndInt64Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: String,
        second: Long,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithStringAndInt64Args", stringInt64UnitHandle, resultKind, first, second)
    }

    override fun invokeMethodWithTwoInt32Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Int,
        second: Int,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithTwoInt32Args", twoInt32UnitHandle, resultKind, first, second)
    }

    override fun invokeMethodWithInt32AndInt64Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Int,
        second: Long,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithInt32AndInt64Args", int32Int64UnitHandle, resultKind, first, second)
    }

    override fun invokeMethodWithInt64AndInt32Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Long,
        second: Int,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithInt64AndInt32Args", int64Int32UnitHandle, resultKind, first, second)
    }

    override fun invokeMethodWithTwoInt64Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Long,
        second: Long,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithTwoInt64Args", twoInt64UnitHandle, resultKind, first, second)
    }

    override fun invokeMethodWithInt64AndStringArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Long,
        second: String,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithInt64AndStringArgs", int64StringUnitHandle, resultKind, first, second)
    }

    override fun invokeMethodWithInt64AndObjectArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Long,
        second: ComPtr,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithInt64AndObjectArgs", int64AddressOutHandle, resultKind, first, second)
    }

    override fun invokeMethodWithStringAndObjectArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: String,
        second: ComPtr,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithStringAndObjectArgs", twoAddressOutHandle, resultKind, first, second)
    }

    override fun invokeMethodWithTwoObjectArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: ComPtr,
    ): Result<ComMethodResult> {
        return invokeRawResultKind(instance, vtableIndex, "invokeMethodWithTwoObjectArgs", twoAddressOutHandle, resultKind, first, second)
    }

    override fun invokeMethodWithResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        vararg arguments: Any,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithResultKind",
            handle = Jdk22Foreign.methodWithArgumentsHandle(arguments.map(::methodArgumentLayout)),
            resultKind = resultKind,
            *arguments,
        )
    }

    override fun invokeIndexOfMethod(
        instance: ComPtr,
        vtableIndex: Int,
        vararg arguments: Any,
    ): Result<Pair<Boolean, UInt>> {
        val operation = "invokeIndexOfMethod"
        val singleArgument = arguments.singleOrNull()
        when (singleArgument) {
            is Boolean -> return JvmComMethodExecutor.invokeIndexOfMethod(instance, vtableIndex, operation, int32IndexOfHandle, if (singleArgument) 1 else 0)
            is Int -> return JvmComMethodExecutor.invokeIndexOfMethod(instance, vtableIndex, operation, int32IndexOfHandle, singleArgument)
            is UInt -> return JvmComMethodExecutor.invokeIndexOfMethod(instance, vtableIndex, operation, int32IndexOfHandle, singleArgument.toInt())
            is Long -> return JvmComMethodExecutor.invokeIndexOfMethod(instance, vtableIndex, operation, int64IndexOfHandle, singleArgument)
            is ULong -> return JvmComMethodExecutor.invokeIndexOfMethod(instance, vtableIndex, operation, int64IndexOfHandle, singleArgument.toLong())
            is Float -> return JvmComMethodExecutor.invokeIndexOfMethod(instance, vtableIndex, operation, float32IndexOfHandle, singleArgument)
            is Double -> return JvmComMethodExecutor.invokeIndexOfMethod(instance, vtableIndex, operation, float64IndexOfHandle, singleArgument)
            is ComPtr -> return JvmComMethodExecutor.invokeIndexOfMethod(instance, vtableIndex, operation, addressIndexOfHandle, singleArgument)
            is String -> return JvmComMethodExecutor.invokeIndexOfMethod(instance, vtableIndex, operation, addressIndexOfHandle, singleArgument)
        }
        return JvmComMethodExecutor.invokeIndexOfMethod(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = Jdk22Foreign.methodWithArgumentsAndTwoOutHandles(arguments.map(::methodArgumentLayout)),
            *arguments,
        )
    }

    override fun invokeComposableMethod(
        instance: ComPtr,
        vtableIndex: Int,
        vararg arguments: Any,
    ): Result<ComposableMethodResult> {
        return runCatching {
            require(!instance.isNull) { "Method invocation requires a non-null COM pointer" }
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val innerSegment = arena.allocate(ValueLayout.ADDRESS)
                val instanceSegment = arena.allocate(ValueLayout.ADDRESS)
                val preparedArguments = prepareAbiArguments(arguments)
                try {
                    val hresult = HResult(
                        Jdk22Foreign.composableMethodWithArgumentsHandle(arguments.map(::methodArgumentLayout))
                            .bindTo(function)
                            .invokeWithArguments(
                                Jdk22Foreign.pointerOf(instance),
                                *preparedArguments.values.toTypedArray(),
                                innerSegment,
                                instanceSegment,
                            ) as Int,
                    )
                    hresult.requireSuccess("invokeComposableMethod($vtableIndex)")
                    ComposableMethodResult(
                        instance = Jdk22Foreign.addressResult(instanceSegment.get(ValueLayout.ADDRESS, 0L)),
                        inner = Jdk22Foreign.addressResult(innerSegment.get(ValueLayout.ADDRESS, 0L)),
                    )
                } finally {
                    preparedArguments.close()
                }
            }
        }
    }

    override fun invokeRawI64MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Long> {
        return invokeRawI64Result(instance, vtableIndex, "invokeRawI64MethodWithObjectArg", Jdk22Foreign.int64MethodWithObjectHandle, value)
    }

    override fun invokeRawI64Method(instance: ComPtr, vtableIndex: Int): Result<Long> {
        return invokeRawI64Result(instance, vtableIndex, "invokeRawI64Method", Jdk22Foreign.int64MethodHandle)
    }

    override fun invokeRawI64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Long> {
        return invokeRawI64Result(instance, vtableIndex, "invokeRawI64MethodWithStringArg", Jdk22Foreign.int64MethodWithStringHandle, value)
    }

    override fun invokeRawI64MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Long> {
        return invokeRawI64Result(instance, vtableIndex, "invokeRawI64MethodWithInt32Arg", Jdk22Foreign.int64MethodWithInt32Handle, value)
    }

    override fun invokeRawI32Method(instance: ComPtr, vtableIndex: Int): Result<Int> {
        return invokeRawI32Result(instance, vtableIndex, "invokeRawI32Method", Jdk22Foreign.int32MethodHandle)
    }

    override fun invokeRawI32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Int> {
        return invokeRawI32Result(instance, vtableIndex, "invokeRawI32MethodWithStringArg", Jdk22Foreign.int32MethodWithStringHandle, value)
    }

    override fun invokeRawI32MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Int> {
        return invokeRawI32Result(instance, vtableIndex, "invokeRawI32MethodWithInt32Arg", Jdk22Foreign.int32MethodWithInt32Handle, value)
    }

    override fun invokeRawI32MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Int> {
        return invokeRawI32Result(instance, vtableIndex, "invokeRawI32MethodWithObjectArg", Jdk22Foreign.int32MethodWithObjectHandle, value)
    }

    override fun invokeRawI32MethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Int> =
        invokeRawI32Result(instance, vtableIndex, "invokeRawI32MethodWithInt64Arg", Jdk22Foreign.int32MethodWithInt64Handle, value)

    override fun invokeFloat64Method(instance: ComPtr, vtableIndex: Int): Result<Double> {
        return invokeRawF64Result(instance, vtableIndex, "invokeFloat64Method", Jdk22Foreign.float64MethodHandle)
    }

    override fun invokeFloat32Method(instance: ComPtr, vtableIndex: Int): Result<Float> {
        return invokeRawF32Result(instance, vtableIndex, "invokeFloat32Method", Jdk22Foreign.float32MethodHandle)
    }

    override fun invokeFloat32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Float> {
        return invokeRawF32Result(instance, vtableIndex, "invokeFloat32MethodWithStringArg", Jdk22Foreign.float32MethodWithInputHandle, value)
    }

    override fun invokeFloat32MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Float> {
        return invokeRawF32Result(instance, vtableIndex, "invokeFloat32MethodWithUInt32Arg", Jdk22Foreign.float32MethodWithUInt32Handle, value)
    }

    override fun invokeFloat32MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Float> =
        invokeRawF32Result(instance, vtableIndex, "invokeFloat32MethodWithObjectArg", Jdk22Foreign.float32MethodWithInputHandle, value)

    override fun invokeFloat32MethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Float> =
        invokeRawF32Result(instance, vtableIndex, "invokeFloat32MethodWithInt64Arg", Jdk22Foreign.float32MethodWithInt64Handle, value)

    override fun invokeFloat64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Double> {
        return invokeRawF64Result(instance, vtableIndex, "invokeFloat64MethodWithStringArg", Jdk22Foreign.float64MethodWithInputHandle, value)
    }

    override fun invokeFloat64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Double> {
        return invokeRawF64Result(instance, vtableIndex, "invokeFloat64MethodWithUInt32Arg", Jdk22Foreign.float64MethodWithUInt32Handle, value)
    }

    override fun invokeFloat64MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Double> =
        invokeRawF64Result(instance, vtableIndex, "invokeFloat64MethodWithObjectArg", Jdk22Foreign.float64MethodWithInputHandle, value)

    override fun invokeFloat64MethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Double> =
        invokeRawF64Result(instance, vtableIndex, "invokeFloat64MethodWithInt64Arg", Jdk22Foreign.float64MethodWithInt64Handle, value)

    override fun invokeGuidGetter(instance: ComPtr, vtableIndex: Int): Result<Guid> {
        return invokeRawGuidResult(instance, vtableIndex, "invokeGuidGetter", Jdk22Foreign.guidGetterHandle)
    }

    override fun invokeGuidMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Guid> =
        invokeRawGuidResult(instance, vtableIndex, "invokeGuidMethodWithStringArg", Jdk22Foreign.guidMethodWithInputHandle, value)

    override fun invokeGuidMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Guid> =
        invokeRawGuidResult(instance, vtableIndex, "invokeGuidMethodWithInt32Arg", Jdk22Foreign.guidMethodWithInt32Handle, value)

    override fun invokeGuidMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Guid> =
        invokeRawGuidResult(instance, vtableIndex, "invokeGuidMethodWithObjectArg", Jdk22Foreign.guidMethodWithInputHandle, value)

    override fun invokeGuidMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Guid> =
        invokeRawGuidResult(instance, vtableIndex, "invokeGuidMethodWithInt64Arg", Jdk22Foreign.guidMethodWithInt64Handle, value)

}

actual val PlatformComInteropKernel: ComInterop = JvmPlatformComInterop

private object JvmPlatformHStringBridge : HStringBridge {
    override fun create(value: String): HString = JvmWinRtRuntime.createHString(value)

    override fun toKotlinString(value: HString): String = JvmWinRtRuntime.toKotlinString(value)

    override fun release(value: HString) {
        JvmWinRtRuntime.releaseHString(value)
    }
}

actual val PlatformHStringBridge: HStringBridge = JvmPlatformHStringBridge

object JvmComRuntime {
    private const val coinitApartmentThreaded = 2
    private const val coinitMultithreaded = 0

    fun initializeSingleThreaded(): HResult {
        val result = Jdk22Foreign.coInitializeExHandle.invokeWithArguments(
            MemorySegment.NULL,
            coinitApartmentThreaded,
        ) as Int
        return HResult(result)
    }

    fun initializeMultithreaded(): HResult {
        val result = Jdk22Foreign.coInitializeExHandle.invokeWithArguments(
            MemorySegment.NULL,
            coinitMultithreaded,
        ) as Int
        return HResult(result)
    }

    fun uninitialize() {
        Jdk22Foreign.coUninitializeHandle.invokeWithArguments()
    }
}

object JvmWinRtRuntime {
    private const val roInitSingleThreaded = 0
    private const val roInitMultithreaded = 1
    val iidIActivationFactory = Guid(
        data1 = 0x00000035,
        data2 = 0,
        data3 = 0,
        data4 = byteArrayOf(0xC0.toByte(), 0, 0, 0, 0, 0, 0, 0x46),
    )

    fun initializeSingleThreaded(): HResult {
        val result = Jdk22Foreign.roInitializeHandle.invokeWithArguments(roInitSingleThreaded) as Int
        return HResult(result)
    }

    fun initializeMultithreaded(): HResult {
        val result = Jdk22Foreign.roInitializeHandle.invokeWithArguments(roInitMultithreaded) as Int
        return HResult(result)
    }

    fun uninitialize() {
        Jdk22Foreign.roUninitializeHandle.invokeWithArguments()
    }

    fun createHString(value: String): HString {
        return Arena.ofConfined().use { arena ->
            val utf16 = value.toByteArray(StandardCharsets.UTF_16LE)
            val source = arena.allocate(utf16.size.toLong() + 2)
            utf16.forEachIndexed { index, byte ->
                source.set(ValueLayout.JAVA_BYTE, index.toLong(), byte)
            }
            source.set(ValueLayout.JAVA_SHORT, utf16.size.toLong(), 0)

            val output = arena.allocate(ValueLayout.ADDRESS)
            val result = HResult(
                Jdk22Foreign.windowsCreateStringHandle.invokeWithArguments(
                    source,
                    value.length,
                    output,
                ) as Int,
            )
            result.requireSuccess("WindowsCreateString")
            HString(output.get(ValueLayout.ADDRESS, 0L).address())
        }
    }

    fun toKotlinString(value: HString): String {
        if (value.isNull) {
            return ""
        }

        return Arena.ofConfined().use { arena ->
            val lengthSegment = arena.allocate(ValueLayout.JAVA_INT)
            val rawBuffer = Jdk22Foreign.windowsGetStringRawBufferHandle.invokeWithArguments(
                MemorySegment.ofAddress(value.raw),
                lengthSegment,
            ) as MemorySegment
            val length = lengthSegment.get(ValueLayout.JAVA_INT, 0L)
            val byteLength = length * 2
            val content = rawBuffer.reinterpret(byteLength.toLong())
            val bytes = ByteArray(byteLength)
            for (index in 0 until byteLength) {
                bytes[index] = content.get(ValueLayout.JAVA_BYTE, index.toLong())
            }
            String(bytes, StandardCharsets.UTF_16LE)
        }
    }

    fun releaseHString(value: HString) {
        if (!value.isNull) {
            Jdk22Foreign.windowsDeleteStringHandle.invokeWithArguments(MemorySegment.ofAddress(value.raw))
        }
    }

    fun getActivationFactory(classId: String, iid: Guid = iidIActivationFactory): Result<ComPtr> {
        return runCatching {
            val hString = createHString(classId)
            try {
                Arena.ofConfined().use { arena ->
                    val iidSegment = Jdk22Foreign.guidSegment(iid, arena)
                    val resultSegment = arena.allocate(ValueLayout.ADDRESS)
                    val result = HResult(
                        Jdk22Foreign.roGetActivationFactoryHandle.invokeWithArguments(
                            MemorySegment.ofAddress(hString.raw),
                            iidSegment,
                            resultSegment,
                        ) as Int,
                    )
                    result.requireSuccess("RoGetActivationFactory")
                    Jdk22Foreign.addressResult(resultSegment.get(ValueLayout.ADDRESS, 0L))
                }
            } finally {
                releaseHString(hString)
            }
        }
    }

    fun activateInstance(factory: ComPtr): Result<ComPtr> {
        if (factory.isNull) {
            return Result.failure(KomException("IActivationFactory pointer must not be null"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.ADDRESS)
                val function = Jdk22Foreign.vtableEntry(factory, 6)
                val result = HResult(
                    Jdk22Foreign.activateInstanceHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(factory),
                        resultSegment,
                    ) as Int,
                )
                result.requireSuccess("IActivationFactory.ActivateInstance")
                Jdk22Foreign.addressResult(resultSegment.get(ValueLayout.ADDRESS, 0L))
            }
        }
    }
}
