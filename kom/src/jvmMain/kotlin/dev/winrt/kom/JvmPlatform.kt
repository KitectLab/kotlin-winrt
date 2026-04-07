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

    fun invokeWithoutOut(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
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
        first: ComPtr,
        second: Int,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    if (first.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(first),
                    second,
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
        first: Int,
        second: ComPtr,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    first,
                    if (second.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(second),
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
        first: ComPtr,
        second: Long,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    if (first.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(first),
                    second,
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
        first: Long,
        second: ComPtr,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    first,
                    if (second.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(second),
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
        value: Int,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value,
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
        value: UInt,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value.toInt(),
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
        value: Long,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value,
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
        value: ULong,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value.toLong(),
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
        value: Boolean,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    if (value) 1 else 0,
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
        value: Float,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value,
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
        value: Double,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value,
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
        value: String,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hString = JvmWinRtRuntime.createHString(value)
            try {
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        MemorySegment.ofAddress(hString.raw),
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
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
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    first,
                    second,
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
        first: Int,
        second: Long,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    first,
                    second,
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
        first: Long,
        second: Int,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    first,
                    second,
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
        first: Long,
        second: Long,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    first,
                    second,
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
        first: String,
        second: Int,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val firstHString = JvmWinRtRuntime.createHString(first)
            try {
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        MemorySegment.ofAddress(firstHString.raw),
                        second,
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
            } finally {
                JvmWinRtRuntime.releaseHString(firstHString)
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
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val secondHString = JvmWinRtRuntime.createHString(second)
            try {
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        first,
                        MemorySegment.ofAddress(secondHString.raw),
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
            } finally {
                JvmWinRtRuntime.releaseHString(secondHString)
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
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val firstHString = JvmWinRtRuntime.createHString(first)
            try {
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        MemorySegment.ofAddress(firstHString.raw),
                        second,
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
            } finally {
                JvmWinRtRuntime.releaseHString(firstHString)
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
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val secondHString = JvmWinRtRuntime.createHString(second)
            try {
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        first,
                        MemorySegment.ofAddress(secondHString.raw),
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
            } finally {
                JvmWinRtRuntime.releaseHString(secondHString)
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
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val firstHString = JvmWinRtRuntime.createHString(first)
            try {
                val secondHString = JvmWinRtRuntime.createHString(second)
                try {
                    val hresult = HResult(
                        handle.bindTo(function).invokeWithArguments(
                            Jdk22Foreign.pointerOf(instance),
                            MemorySegment.ofAddress(firstHString.raw),
                            MemorySegment.ofAddress(secondHString.raw),
                        ) as Int,
                    )
                    hresult.requireSuccess("$operation($vtableIndex)")
                } finally {
                    JvmWinRtRuntime.releaseHString(secondHString)
                }
            } finally {
                JvmWinRtRuntime.releaseHString(firstHString)
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
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    if (value.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(value),
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
        first: ComPtr,
        second: String,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hString = JvmWinRtRuntime.createHString(second)
            try {
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        if (first.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(first),
                        MemorySegment.ofAddress(hString.raw),
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
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
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hString = JvmWinRtRuntime.createHString(first)
            try {
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        MemorySegment.ofAddress(hString.raw),
                        if (second.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(second),
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
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
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    if (first.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(first),
                    if (second.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(second),
                ) as Int,
            )
            hresult.requireSuccess("$operation($vtableIndex)")
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
        return runCatching {
            requireInstance(instance)
            Arena.ofConfined().use { arena ->
                val resultSegment = allocator(arena)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
                reader(resultSegment)
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
        return runCatching {
            requireInstance(instance)
            val hString = JvmWinRtRuntime.createHString(value)
            try {
                Arena.ofConfined().use { arena ->
                    val resultSegment = allocator(arena)
                    val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                    val hresult = HResult(
                        handle.bindTo(function).invokeWithArguments(
                            Jdk22Foreign.pointerOf(instance),
                            MemorySegment.ofAddress(hString.raw),
                            resultSegment,
                        ) as Int,
                    )
                    hresult.requireSuccess("$operation($vtableIndex)")
                    reader(resultSegment)
                }
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
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
        return runCatching {
            requireInstance(instance)
            val hString = JvmWinRtRuntime.createHString(second)
            try {
                Arena.ofConfined().use { arena ->
                    val resultSegment = allocator(arena)
                    val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                    val hresult = HResult(
                        handle.bindTo(function).invokeWithArguments(
                            Jdk22Foreign.pointerOf(instance),
                            if (first.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(first),
                            MemorySegment.ofAddress(hString.raw),
                            resultSegment,
                        ) as Int,
                    )
                    hresult.requireSuccess("$operation($vtableIndex)")
                    reader(resultSegment)
                }
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
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
        return runCatching {
            requireInstance(instance)
            val hString = JvmWinRtRuntime.createHString(first)
            try {
                Arena.ofConfined().use { arena ->
                    val resultSegment = allocator(arena)
                    val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                    val hresult = HResult(
                        handle.bindTo(function).invokeWithArguments(
                            Jdk22Foreign.pointerOf(instance),
                            MemorySegment.ofAddress(hString.raw),
                            if (second.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(second),
                            resultSegment,
                        ) as Int,
                    )
                    hresult.requireSuccess("$operation($vtableIndex)")
                    reader(resultSegment)
                }
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
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
        return runCatching {
            requireInstance(instance)
            Arena.ofConfined().use { arena ->
                val resultSegment = allocator(arena)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        if (first.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(first),
                        if (second.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(second),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
                reader(resultSegment)
            }
        }
    }

    private fun invokeWithoutOutArguments(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        arguments: Array<out Any>,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val preparedArguments = prepareAbiArguments(arguments)
            try {
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        *preparedArguments.values.toTypedArray(),
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
            } finally {
                preparedArguments.close()
            }
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

    private fun <T> invokeWithOutSegmentArguments(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        allocator: (Arena) -> MemorySegment,
        reader: (MemorySegment) -> T,
        arguments: Array<out Any>,
    ): Result<T> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val resultSegment = allocator(arena)
                val preparedArguments = prepareAbiArguments(arguments)
                try {
                    val hresult = HResult(
                        handle.bindTo(function).invokeWithArguments(
                            Jdk22Foreign.pointerOf(instance),
                            *preparedArguments.values.toTypedArray(),
                            resultSegment,
                        ) as Int,
                    )
                    hresult.requireSuccess("$operation($vtableIndex)")
                    reader(resultSegment)
                } finally {
                    preparedArguments.close()
                }
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
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            Arena.ofConfined().use { arena ->
                val indexSegment = arena.allocate(ValueLayout.JAVA_INT)
                val foundSegment = arena.allocate(ValueLayout.JAVA_INT)
                val preparedArguments = prepareAbiArguments(arguments)
                try {
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
                } finally {
                    preparedArguments.close()
                }
            }
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

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: String,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Int,
        second: Int,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Int,
        second: Long,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Long,
        second: Int,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Long,
        second: Long,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: String,
        second: Int,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Int,
        second: String,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: String,
        second: Long,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Long,
        second: String,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: String,
        second: ComPtr,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: ComPtr,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: Int,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Int,
        second: ComPtr,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: Long,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }

    fun invokeWithOutResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        resultKind: ComMethodResultKind,
        first: Long,
        second: ComPtr,
    ): Result<ComMethodResult> {
        return invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = operation,
            handle = handle,
            allocator = { arena -> allocateResultSegment(arena, resultKind) },
            reader = { segment -> readResult(segment, resultKind) },
            first,
            second,
        )
    }
}

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
        value: UInt,
    ): Result<Int> = invokeRawI32Result(instance, vtableIndex, operation, handle, value.toInt())

    private fun invokeRawI32Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Boolean,
    ): Result<Int> = invokeRawI32Result(instance, vtableIndex, operation, handle, if (value) 1 else 0)

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
        value: UInt,
    ): Result<Long> = invokeRawI64Result(instance, vtableIndex, operation, handle, value.toInt())

    private fun invokeRawI64Result(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Boolean,
    ): Result<Long> = invokeRawI64Result(instance, vtableIndex, operation, handle, if (value) 1 else 0)

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
        value: UInt,
    ): Result<MemorySegment> = invokeRawAddressResult(instance, vtableIndex, operation, handle, value.toInt())

    private fun invokeRawAddressResult(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Boolean,
    ): Result<MemorySegment> = invokeRawAddressResult(instance, vtableIndex, operation, handle, if (value) 1 else 0)

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

    private fun Result<Int>.asUIntResult(): Result<UInt> = map(Int::toUInt)

    private fun Result<Int>.asBooleanResult(): Result<Boolean> = map { it != 0 }

    private fun Result<Long>.asULongResult(): Result<ULong> = map(Long::toULong)

    private fun Result<MemorySegment>.asHStringResult(): Result<HString> = map { segment -> HString(segment.address()) }

    private fun Result<MemorySegment>.asComPtrResult(): Result<ComPtr> = map(Jdk22Foreign::addressResult)

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
        value: UInt,
    ): Result<Unit> = invokeRawUnit(instance, vtableIndex, operation, handle, value.toInt())

    private fun invokeRawUnit(
        instance: ComPtr,
        vtableIndex: Int,
        operation: String,
        handle: java.lang.invoke.MethodHandle,
        value: Boolean,
    ): Result<Unit> = invokeRawUnit(instance, vtableIndex, operation, handle, if (value) 1 else 0)

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

    override fun invokeUnitMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithInt32Arg", Jdk22Foreign.unitMethodWithInt32Handle, value)
    }

    override fun invokeUnitMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithUInt32Arg", Jdk22Foreign.unitMethodWithUInt32Handle, value)
    }

    override fun invokeUnitMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithInt64Arg", Jdk22Foreign.unitMethodWithInt64Handle, value)
    }

    override fun invokeUnitMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithStringArg", Jdk22Foreign.hstringSetterHandle, value)
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

    override fun invokeUnitMethodWithObjectAndBooleanArgs(instance: ComPtr, vtableIndex: Int, first: ComPtr, second: Boolean): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithObjectAndBooleanArgs", addressInt32UnitHandle, first, if (second) 1 else 0)
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

    override fun invokeUnitMethodWithStringAndUInt32Args(instance: ComPtr, vtableIndex: Int, first: String, second: UInt): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithStringAndUInt32Args", stringInt32UnitHandle, first, second.toInt())
    }

    override fun invokeUnitMethodWithUInt32AndStringArgs(instance: ComPtr, vtableIndex: Int, first: UInt, second: String): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithUInt32AndStringArgs", int32StringUnitHandle, first.toInt(), second)
    }

    override fun invokeUnitMethodWithStringAndBooleanArgs(instance: ComPtr, vtableIndex: Int, first: String, second: Boolean): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithStringAndBooleanArgs", stringInt32UnitHandle, first, if (second) 1 else 0)
    }

    override fun invokeUnitMethodWithBooleanAndStringArgs(instance: ComPtr, vtableIndex: Int, first: Boolean, second: String): Result<Unit> {
        return invokeRawUnit(instance, vtableIndex, "invokeUnitMethodWithBooleanAndStringArgs", int32StringUnitHandle, if (first) 1 else 0, second)
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

    override fun invokeHStringMethod(instance: ComPtr, vtableIndex: Int): Result<HString> {
        return invokeRawAddressResult(instance, vtableIndex, "invokeHStringMethod", Jdk22Foreign.hstringMethodHandle).asHStringResult()
    }

    override fun invokeHStringMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<HString> {
        return invokeRawAddressResult(instance, vtableIndex, "invokeHStringMethodWithStringArg", Jdk22Foreign.hstringMethodWithInputHandle, value).asHStringResult()
    }

    override fun invokeHStringMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<HString> {
        return invokeRawAddressResult(instance, vtableIndex, "invokeHStringMethodWithInt32Arg", Jdk22Foreign.hstringMethodWithInt32Handle, value).asHStringResult()
    }

    override fun invokeObjectMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<ComPtr> {
        return invokeRawAddressResult(instance, vtableIndex, "invokeObjectMethodWithStringArg", Jdk22Foreign.objectMethodWithInputHandle, value).asComPtrResult()
    }

    override fun invokeObjectMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<ComPtr> {
        return invokeRawAddressResult(instance, vtableIndex, "invokeObjectMethodWithObjectArg", Jdk22Foreign.objectMethodWithInputHandle, value).asComPtrResult()
    }

    override fun invokeObjectMethod(instance: ComPtr, vtableIndex: Int): Result<ComPtr> {
        return invokeRawAddressResult(instance, vtableIndex, "invokeObjectMethod", Jdk22Foreign.objectMethodHandle).asComPtrResult()
    }

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

    override fun invokeObjectMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<ComPtr> {
        return invokeRawAddressResult(instance, vtableIndex, "invokeObjectMethodWithUInt32Arg", Jdk22Foreign.objectMethodWithUInt32Handle, value).asComPtrResult()
    }

    override fun invokeObjectMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<ComPtr> =
        invokeRawAddressResult(instance, vtableIndex, "invokeObjectMethodWithInt32Arg", Jdk22Foreign.objectMethodWithUInt32Handle, value).asComPtrResult()

    override fun invokeObjectMethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<ComPtr> =
        invokeRawAddressResult(instance, vtableIndex, "invokeObjectMethodWithBooleanArg", Jdk22Foreign.objectMethodWithUInt32Handle, value).asComPtrResult()

    override fun invokeObjectMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<ComPtr> =
        invokeRawAddressResult(instance, vtableIndex, "invokeObjectMethodWithInt64Arg", Jdk22Foreign.objectMethodWithInt64Handle, value).asComPtrResult()

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
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithObjectAndStringArgs",
            handle = twoAddressOutHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithObjectAndInt32Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: Int,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithObjectAndInt32Args",
            handle = addressInt32OutHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithInt32AndObjectArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Int,
        second: ComPtr,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithInt32AndObjectArgs",
            handle = int32AddressOutHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithObjectAndInt64Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: Long,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithObjectAndInt64Args",
            handle = addressInt64OutHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithStringAndInt32Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: String,
        second: Int,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithStringAndInt32Args",
            handle = stringInt32UnitHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithInt32AndStringArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Int,
        second: String,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithInt32AndStringArgs",
            handle = int32StringUnitHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithStringAndInt64Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: String,
        second: Long,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithStringAndInt64Args",
            handle = stringInt64UnitHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithTwoInt32Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Int,
        second: Int,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithTwoInt32Args",
            handle = twoInt32UnitHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithInt32AndInt64Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Int,
        second: Long,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithInt32AndInt64Args",
            handle = int32Int64UnitHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithInt64AndInt32Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Long,
        second: Int,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithInt64AndInt32Args",
            handle = int64Int32UnitHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithTwoInt64Args(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Long,
        second: Long,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithTwoInt64Args",
            handle = twoInt64UnitHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithInt64AndStringArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Long,
        second: String,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithInt64AndStringArgs",
            handle = int64StringUnitHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithInt64AndObjectArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: Long,
        second: ComPtr,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithInt64AndObjectArgs",
            handle = int64AddressOutHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithStringAndObjectArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: String,
        second: ComPtr,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithStringAndObjectArgs",
            handle = twoAddressOutHandle,
            resultKind = resultKind,
            first,
            second,
        )
    }

    override fun invokeMethodWithTwoObjectArgs(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        first: ComPtr,
        second: ComPtr,
    ): Result<ComMethodResult> {
        return JvmComMethodExecutor.invokeWithOutResultKind(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeMethodWithTwoObjectArgs",
            handle = twoAddressOutHandle,
            resultKind = resultKind,
            first,
            second,
        )
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
        return JvmComMethodExecutor.invokeIndexOfMethod(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeIndexOfMethod",
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

    override fun invokeHStringMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<HString> {
        return invokeRawAddressResult(instance, vtableIndex, "invokeHStringMethodWithUInt32Arg", Jdk22Foreign.hstringMethodWithUInt32Handle, value).asHStringResult()
    }

    override fun invokeHStringMethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<HString> =
        invokeRawAddressResult(instance, vtableIndex, "invokeHStringMethodWithBooleanArg", Jdk22Foreign.hstringMethodWithUInt32Handle, value).asHStringResult()

    override fun invokeHStringMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<HString> =
        invokeRawAddressResult(instance, vtableIndex, "invokeHStringMethodWithObjectArg", Jdk22Foreign.hstringMethodWithInputHandle, value).asHStringResult()

    override fun invokeHStringMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<HString> =
        invokeRawAddressResult(instance, vtableIndex, "invokeHStringMethodWithInt64Arg", Jdk22Foreign.hstringMethodWithInt64Handle, value).asHStringResult()

    override fun invokeStringSetter(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeStringSetter",
            handle = Jdk22Foreign.hstringSetterHandle,
            value,
        )
    }

    override fun invokeObjectSetter(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeObjectSetter",
            handle = Jdk22Foreign.objectSetterHandle,
            value,
        )
    }

    override fun invokeInt64MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Long> {
        return invokeRawI64Result(instance, vtableIndex, "invokeInt64MethodWithObjectArg", Jdk22Foreign.int64MethodWithObjectHandle, value)
    }

    override fun invokeInt64Method(instance: ComPtr, vtableIndex: Int): Result<Long> {
        return invokeRawI64Result(instance, vtableIndex, "invokeInt64Method", Jdk22Foreign.int64MethodHandle)
    }

    override fun invokeInt64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Long> {
        return invokeRawI64Result(instance, vtableIndex, "invokeInt64MethodWithStringArg", Jdk22Foreign.int64MethodWithStringHandle, value)
    }

    override fun invokeInt64MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Long> {
        return invokeRawI64Result(instance, vtableIndex, "invokeInt64MethodWithInt32Arg", Jdk22Foreign.int64MethodWithInt32Handle, value)
    }

    override fun invokeInt64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Long> {
        return invokeRawI64Result(instance, vtableIndex, "invokeInt64MethodWithUInt32Arg", Jdk22Foreign.int64MethodWithUInt32Handle, value)
    }

    override fun invokeInt64MethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<Long> {
        return invokeRawI64Result(instance, vtableIndex, "invokeInt64MethodWithBooleanArg", Jdk22Foreign.int64MethodWithBooleanHandle, value)
    }

    override fun invokeUInt64Method(instance: ComPtr, vtableIndex: Int): Result<ULong> {
        return invokeRawI64Result(instance, vtableIndex, "invokeUInt64Method", Jdk22Foreign.uint64MethodHandle).asULongResult()
    }

    override fun invokeUInt64MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<ULong> {
        return invokeRawI64Result(instance, vtableIndex, "invokeUInt64MethodWithObjectArg", Jdk22Foreign.uint64MethodWithObjectHandle, value).asULongResult()
    }

    override fun invokeUInt64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<ULong> {
        return invokeRawI64Result(instance, vtableIndex, "invokeUInt64MethodWithStringArg", Jdk22Foreign.uint64MethodWithStringHandle, value).asULongResult()
    }

    override fun invokeUInt64MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<ULong> {
        return invokeRawI64Result(instance, vtableIndex, "invokeUInt64MethodWithInt32Arg", Jdk22Foreign.uint64MethodWithInt32Handle, value).asULongResult()
    }

    override fun invokeUInt64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<ULong> {
        return invokeRawI64Result(instance, vtableIndex, "invokeUInt64MethodWithUInt32Arg", Jdk22Foreign.uint64MethodWithUInt32Handle, value).asULongResult()
    }

    override fun invokeUInt64MethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<ULong> {
        return invokeRawI64Result(instance, vtableIndex, "invokeUInt64MethodWithBooleanArg", Jdk22Foreign.uint64MethodWithBooleanHandle, value).asULongResult()
    }

    override fun invokeInt32Setter(instance: ComPtr, vtableIndex: Int, value: Int): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeInt32Setter",
            handle = Jdk22Foreign.int32SetterHandle,
            value,
        )
    }

    override fun invokeUInt32Setter(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUInt32Setter",
            handle = Jdk22Foreign.uint32SetterHandle,
            value,
        )
    }

    override fun invokeFloat32Setter(instance: ComPtr, vtableIndex: Int, value: Float): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeFloat32Setter",
            handle = Jdk22Foreign.float32SetterHandle,
            value,
        )
    }

    override fun invokeBooleanSetter(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeBooleanSetter",
            handle = Jdk22Foreign.booleanSetterHandle,
            value,
        )
    }

    override fun invokeFloat64Setter(instance: ComPtr, vtableIndex: Int, value: Double): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeFloat64Setter",
            handle = Jdk22Foreign.float64SetterHandle,
            value,
        )
    }

    override fun invokeInt64Setter(instance: ComPtr, vtableIndex: Int, value: Long): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeInt64Setter",
            handle = Jdk22Foreign.int64SetterHandle,
            value,
        )
    }

    override fun invokeUInt64Setter(instance: ComPtr, vtableIndex: Int, value: ULong): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUInt64Setter",
            handle = Jdk22Foreign.uint64SetterHandle,
            value,
        )
    }

    override fun invokeInt32Method(instance: ComPtr, vtableIndex: Int): Result<Int> {
        return invokeRawI32Result(instance, vtableIndex, "invokeInt32Method", Jdk22Foreign.int32MethodHandle)
    }

    override fun invokeInt32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Int> {
        return invokeRawI32Result(instance, vtableIndex, "invokeInt32MethodWithStringArg", Jdk22Foreign.int32MethodWithStringHandle, value)
    }

    override fun invokeInt32MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Int> {
        return invokeRawI32Result(instance, vtableIndex, "invokeInt32MethodWithInt32Arg", Jdk22Foreign.int32MethodWithInt32Handle, value)
    }

    override fun invokeInt32MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Int> {
        return invokeRawI32Result(instance, vtableIndex, "invokeInt32MethodWithUInt32Arg", Jdk22Foreign.int32MethodWithUInt32Handle, value)
    }

    override fun invokeInt32MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Int> {
        return invokeRawI32Result(instance, vtableIndex, "invokeInt32MethodWithObjectArg", Jdk22Foreign.int32MethodWithObjectHandle, value)
    }

    override fun invokeInt32MethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<Int> =
        invokeRawI32Result(instance, vtableIndex, "invokeInt32MethodWithBooleanArg", Jdk22Foreign.int32MethodWithUInt32Handle, value)

    override fun invokeInt32MethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Int> =
        invokeRawI32Result(instance, vtableIndex, "invokeInt32MethodWithInt64Arg", Jdk22Foreign.int32MethodWithInt64Handle, value)

    override fun invokeUInt32Method(instance: ComPtr, vtableIndex: Int): Result<UInt> {
        return invokeRawI32Result(instance, vtableIndex, "invokeUInt32Method", Jdk22Foreign.uint32MethodHandle).asUIntResult()
    }

    override fun invokeUInt32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<UInt> {
        return invokeRawI32Result(instance, vtableIndex, "invokeUInt32MethodWithStringArg", Jdk22Foreign.uint32MethodWithStringHandle, value).asUIntResult()
    }

    override fun invokeUInt32MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<UInt> {
        return invokeRawI32Result(instance, vtableIndex, "invokeUInt32MethodWithInt32Arg", Jdk22Foreign.uint32MethodWithInt32Handle, value).asUIntResult()
    }

    override fun invokeUInt32MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<UInt> {
        return invokeRawI32Result(instance, vtableIndex, "invokeUInt32MethodWithUInt32Arg", Jdk22Foreign.uint32MethodWithUInt32Handle, value).asUIntResult()
    }

    override fun invokeUInt32MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<UInt> {
        return invokeRawI32Result(instance, vtableIndex, "invokeUInt32MethodWithObjectArg", Jdk22Foreign.uint32MethodWithObjectHandle, value).asUIntResult()
    }

    override fun invokeUInt32MethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<UInt> =
        invokeRawI32Result(instance, vtableIndex, "invokeUInt32MethodWithBooleanArg", Jdk22Foreign.uint32MethodWithUInt32Handle, value).asUIntResult()

    override fun invokeUInt32MethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<UInt> =
        invokeRawI32Result(instance, vtableIndex, "invokeUInt32MethodWithInt64Arg", Jdk22Foreign.uint32MethodWithInt64Handle, value).asUIntResult()

    override fun invokeBooleanGetter(instance: ComPtr, vtableIndex: Int): Result<Boolean> {
        return invokeRawI32Result(instance, vtableIndex, "invokeBooleanGetter", Jdk22Foreign.booleanGetterHandle).asBooleanResult()
    }

    override fun invokeBooleanMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Boolean> {
        return invokeRawI32Result(instance, vtableIndex, "invokeBooleanMethodWithObjectArg", Jdk22Foreign.booleanMethodWithInputHandle, value).asBooleanResult()
    }

    override fun invokeBooleanMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Boolean> {
        return invokeRawI32Result(instance, vtableIndex, "invokeBooleanMethodWithStringArg", Jdk22Foreign.booleanMethodWithInputHandle, value).asBooleanResult()
    }

    override fun invokeBooleanMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Boolean> {
        return invokeRawI32Result(instance, vtableIndex, "invokeBooleanMethodWithUInt32Arg", Jdk22Foreign.booleanMethodWithUInt32Handle, value).asBooleanResult()
    }

    override fun invokeBooleanMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Boolean> =
        invokeRawI32Result(instance, vtableIndex, "invokeBooleanMethodWithInt32Arg", Jdk22Foreign.booleanMethodWithUInt32Handle, value).asBooleanResult()

    override fun invokeBooleanMethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<Boolean> =
        invokeRawI32Result(instance, vtableIndex, "invokeBooleanMethodWithBooleanArg", Jdk22Foreign.booleanMethodWithUInt32Handle, value).asBooleanResult()

    override fun invokeBooleanMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Boolean> =
        invokeRawI32Result(instance, vtableIndex, "invokeBooleanMethodWithInt64Arg", Jdk22Foreign.booleanMethodWithInt64Handle, value).asBooleanResult()

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

    override fun invokeFloat32MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Float> =
        invokeRawF32Result(instance, vtableIndex, "invokeFloat32MethodWithInt32Arg", Jdk22Foreign.float32MethodWithUInt32Handle, value)

    override fun invokeFloat32MethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<Float> =
        invokeRawF32Result(instance, vtableIndex, "invokeFloat32MethodWithBooleanArg", Jdk22Foreign.float32MethodWithUInt32Handle, value)

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

    override fun invokeFloat64MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Double> =
        invokeRawF64Result(instance, vtableIndex, "invokeFloat64MethodWithInt32Arg", Jdk22Foreign.float64MethodWithUInt32Handle, value)

    override fun invokeFloat64MethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<Double> =
        invokeRawF64Result(instance, vtableIndex, "invokeFloat64MethodWithBooleanArg", Jdk22Foreign.float64MethodWithUInt32Handle, value)

    override fun invokeFloat64MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Double> =
        invokeRawF64Result(instance, vtableIndex, "invokeFloat64MethodWithObjectArg", Jdk22Foreign.float64MethodWithInputHandle, value)

    override fun invokeFloat64MethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Double> =
        invokeRawF64Result(instance, vtableIndex, "invokeFloat64MethodWithInt64Arg", Jdk22Foreign.float64MethodWithInt64Handle, value)

    override fun invokeGuidGetter(instance: ComPtr, vtableIndex: Int): Result<Guid> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeGuidGetter",
            handle = Jdk22Foreign.guidGetterHandle,
            allocator = { arena -> arena.allocate(16) },
            reader = Jdk22Foreign::guidFromSegment,
        )
    }

    override fun invokeGuidMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Guid> =
        JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeGuidMethodWithStringArg",
            handle = Jdk22Foreign.guidMethodWithInputHandle,
            allocator = { arena -> arena.allocate(16) },
            reader = Jdk22Foreign::guidFromSegment,
            value,
        )

    override fun invokeGuidMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Guid> =
        JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeGuidMethodWithInt32Arg",
            handle = Jdk22Foreign.guidMethodWithInt32Handle,
            allocator = { arena -> arena.allocate(16) },
            reader = Jdk22Foreign::guidFromSegment,
            value,
        )

    override fun invokeGuidMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Guid> =
        JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeGuidMethodWithUInt32Arg",
            handle = Jdk22Foreign.guidMethodWithInt32Handle,
            allocator = { arena -> arena.allocate(16) },
            reader = Jdk22Foreign::guidFromSegment,
            value.toInt(),
        )

    override fun invokeGuidMethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<Guid> =
        JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeGuidMethodWithBooleanArg",
            handle = Jdk22Foreign.guidMethodWithInt32Handle,
            allocator = { arena -> arena.allocate(16) },
            reader = Jdk22Foreign::guidFromSegment,
            if (value) 1 else 0,
        )

    override fun invokeGuidMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Guid> =
        JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeGuidMethodWithObjectArg",
            handle = Jdk22Foreign.guidMethodWithInputHandle,
            allocator = { arena -> arena.allocate(16) },
            reader = Jdk22Foreign::guidFromSegment,
            value,
        )

    override fun invokeGuidMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Guid> =
        JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeGuidMethodWithInt64Arg",
            handle = Jdk22Foreign.guidMethodWithInt64Handle,
            allocator = { arena -> arena.allocate(16) },
            reader = Jdk22Foreign::guidFromSegment,
            value,
        )

    override fun invokeInt64Getter(instance: ComPtr, vtableIndex: Int): Result<Long> {
        return invokeRawI64Result(instance, vtableIndex, "invokeInt64Getter", Jdk22Foreign.int64GetterHandle)
    }
}

actual val PlatformComInterop: ComInterop = JvmPlatformComInterop

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
