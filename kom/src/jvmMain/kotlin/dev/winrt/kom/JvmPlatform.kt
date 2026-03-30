package dev.winrt.kom

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.nio.charset.StandardCharsets

actual object PlatformRuntime {
    actual val platformName: String = "jvm"
    actual val isWindows: Boolean = System.getProperty("os.name").contains("Windows", ignoreCase = true)
    actual val ffiBackend: String = "jdk22-ffm"
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
        vararg arguments: Any?,
    ): Result<Unit> {
        return runCatching {
            requireInstance(instance)
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    *arguments,
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
        vararg arguments: Any?,
    ): Result<T> {
        return runCatching {
            requireInstance(instance)
            Arena.ofConfined().use { arena ->
                val resultSegment = allocator(arena)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        *arguments,
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("$operation($vtableIndex)")
                reader(resultSegment)
            }
        }
    }

    fun <T> withHStringArg(value: String, block: (MemorySegment) -> T): T {
        val hString = JvmWinRtRuntime.createHString(value)
        try {
            return block(MemorySegment.ofAddress(hString.raw))
        } finally {
            JvmWinRtRuntime.releaseHString(hString)
        }
    }
}

private object JvmPlatformComInterop : ComInterop {
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
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUnitMethod",
            handle = Jdk22Foreign.unitMethodHandle,
        )
    }

    override fun invokeUnitMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUnitMethodWithInt32Arg",
            handle = Jdk22Foreign.unitMethodWithInt32Handle,
            value,
        )
    }

    override fun invokeUnitMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUnitMethodWithUInt32Arg",
            handle = Jdk22Foreign.unitMethodWithUInt32Handle,
            value.toInt(),
        )
    }

    override fun invokeUnitMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Unit> {
        return JvmComMethodExecutor.invokeWithoutOut(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUnitMethodWithInt64Arg",
            handle = Jdk22Foreign.unitMethodWithInt64Handle,
            value,
        )
    }

    override fun invokeUnitMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit> {
        return runCatching {
            JvmComMethodExecutor.withHStringArg(value) { input ->
                JvmComMethodExecutor.invokeWithoutOut(
                    instance = instance,
                    vtableIndex = vtableIndex,
                    operation = "invokeUnitMethodWithStringArg",
                    handle = Jdk22Foreign.hstringSetterHandle,
                    input,
                ).getOrThrow()
            }
        }
    }

    override fun invokeHStringMethod(instance: ComPtr, vtableIndex: Int): Result<HString> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeHStringMethod",
            handle = Jdk22Foreign.hstringMethodHandle,
            allocator = { arena -> arena.allocate(ValueLayout.ADDRESS) },
            reader = { segment -> HString(segment.get(ValueLayout.ADDRESS, 0L).address()) },
        )
    }

    override fun invokeHStringMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<HString> {
        return runCatching {
            JvmComMethodExecutor.withHStringArg(value) { input ->
                JvmComMethodExecutor.invokeWithOutSegment(
                    instance = instance,
                    vtableIndex = vtableIndex,
                    operation = "invokeHStringMethodWithStringArg",
                    handle = Jdk22Foreign.hstringMethodWithInputHandle,
                    allocator = { arena -> arena.allocate(ValueLayout.ADDRESS) },
                    reader = { segment -> HString(segment.get(ValueLayout.ADDRESS, 0L).address()) },
                    input,
                ).getOrThrow()
            }
        }
    }

    override fun invokeHStringMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<HString> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.ADDRESS)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.hstringMethodWithInt32Handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        value,
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeHStringMethodWithInt32Arg($vtableIndex)")
                HString(resultSegment.get(ValueLayout.ADDRESS, 0L).address())
            }
        }
    }

    override fun invokeObjectMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<ComPtr> {
        return runCatching {
            JvmComMethodExecutor.withHStringArg(value) { input ->
                JvmComMethodExecutor.invokeWithOutSegment(
                    instance = instance,
                    vtableIndex = vtableIndex,
                    operation = "invokeObjectMethodWithStringArg",
                    handle = Jdk22Foreign.objectMethodWithInputHandle,
                    allocator = { arena -> arena.allocate(ValueLayout.ADDRESS) },
                    reader = { segment -> Jdk22Foreign.addressResult(segment.get(ValueLayout.ADDRESS, 0L)) },
                    input,
                ).getOrThrow()
            }
        }
    }

    override fun invokeObjectMethod(instance: ComPtr, vtableIndex: Int): Result<ComPtr> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.ADDRESS)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.objectMethodHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeObjectMethod($vtableIndex)")
                Jdk22Foreign.addressResult(resultSegment.get(ValueLayout.ADDRESS, 0L))
            }
        }
    }

    override fun invokeObjectMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<ComPtr> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.ADDRESS)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.objectMethodWithUInt32Handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        value.toInt(),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeObjectMethodWithUInt32Arg($vtableIndex)")
                Jdk22Foreign.addressResult(resultSegment.get(ValueLayout.ADDRESS, 0L))
            }
        }
    }

    override fun invokeHStringMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<HString> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.ADDRESS)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.hstringMethodWithUInt32Handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        value.toInt(),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeHStringMethodWithUInt32Arg($vtableIndex)")
                HString(resultSegment.get(ValueLayout.ADDRESS, 0L).address())
            }
        }
    }

    override fun invokeStringSetter(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit> {
        return runCatching {
            JvmComMethodExecutor.withHStringArg(value) { input ->
                JvmComMethodExecutor.invokeWithoutOut(
                    instance = instance,
                    vtableIndex = vtableIndex,
                    operation = "invokeStringSetter",
                    handle = Jdk22Foreign.hstringSetterHandle,
                    input,
                ).getOrThrow()
            }
        }
    }

    override fun invokeObjectSetter(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Unit> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                Jdk22Foreign.objectSetterHandle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    if (value.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(value),
                ) as Int,
            )
            hresult.requireSuccess("invokeObjectSetter($vtableIndex)")
        }
    }

    override fun invokeInt64MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Long> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeInt64MethodWithObjectArg",
            handle = Jdk22Foreign.int64MethodWithObjectHandle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
            reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L) },
            if (value.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(value),
        )
    }

    override fun invokeInt64Method(instance: ComPtr, vtableIndex: Int): Result<Long> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeInt64Method",
            handle = Jdk22Foreign.int64MethodHandle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
            reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L) },
        )
    }

    override fun invokeInt64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Long> {
        return runCatching {
            JvmComMethodExecutor.withHStringArg(value) { input ->
                JvmComMethodExecutor.invokeWithOutSegment(
                    instance = instance,
                    vtableIndex = vtableIndex,
                    operation = "invokeInt64MethodWithStringArg",
                    handle = Jdk22Foreign.int64MethodWithStringHandle,
                    allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
                    reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L) },
                    input,
                ).getOrThrow()
            }
        }
    }

    override fun invokeInt64MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Long> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeInt64MethodWithInt32Arg",
            handle = Jdk22Foreign.int64MethodWithInt32Handle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
            reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L) },
            value,
        )
    }

    override fun invokeInt64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Long> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeInt64MethodWithUInt32Arg",
            handle = Jdk22Foreign.int64MethodWithUInt32Handle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
            reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L) },
            value.toLong(),
        )
    }

    override fun invokeInt64MethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<Long> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeInt64MethodWithBooleanArg",
            handle = Jdk22Foreign.int64MethodWithBooleanHandle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
            reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L) },
            if (value) 1L else 0L,
        )
    }

    override fun invokeUInt64Method(instance: ComPtr, vtableIndex: Int): Result<ULong> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUInt64Method",
            handle = Jdk22Foreign.uint64MethodHandle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
            reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L).toULong() },
        )
    }

    override fun invokeUInt64MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<ULong> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUInt64MethodWithObjectArg",
            handle = Jdk22Foreign.uint64MethodWithObjectHandle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
            reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L).toULong() },
            if (value.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(value),
        )
    }

    override fun invokeUInt64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<ULong> {
        return runCatching {
            JvmComMethodExecutor.withHStringArg(value) { input ->
                JvmComMethodExecutor.invokeWithOutSegment(
                    instance = instance,
                    vtableIndex = vtableIndex,
                    operation = "invokeUInt64MethodWithStringArg",
                    handle = Jdk22Foreign.uint64MethodWithStringHandle,
                    allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
                    reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L).toULong() },
                    input,
                ).getOrThrow()
            }
        }
    }

    override fun invokeUInt64MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<ULong> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUInt64MethodWithInt32Arg",
            handle = Jdk22Foreign.uint64MethodWithInt32Handle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
            reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L).toULong() },
            value,
        )
    }

    override fun invokeUInt64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<ULong> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUInt64MethodWithUInt32Arg",
            handle = Jdk22Foreign.uint64MethodWithUInt32Handle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
            reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L).toULong() },
            value.toLong(),
        )
    }

    override fun invokeUInt64MethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<ULong> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUInt64MethodWithBooleanArg",
            handle = Jdk22Foreign.uint64MethodWithBooleanHandle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_LONG) },
            reader = { segment -> segment.get(ValueLayout.JAVA_LONG, 0L).toULong() },
            if (value) 1L else 0L,
        )
    }

    override fun invokeInt32Setter(instance: ComPtr, vtableIndex: Int, value: Int): Result<Unit> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                Jdk22Foreign.int32SetterHandle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value,
                ) as Int,
            )
            hresult.requireSuccess("invokeInt32Setter($vtableIndex)")
        }
    }

    override fun invokeUInt32Setter(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Unit> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                Jdk22Foreign.uint32SetterHandle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value.toInt(),
                ) as Int,
            )
            hresult.requireSuccess("invokeUInt32Setter($vtableIndex)")
        }
    }

    override fun invokeFloat32Setter(instance: ComPtr, vtableIndex: Int, value: Float): Result<Unit> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                Jdk22Foreign.float32SetterHandle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value,
                ) as Int,
            )
            hresult.requireSuccess("invokeFloat32Setter($vtableIndex)")
        }
    }

    override fun invokeBooleanSetter(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<Unit> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                Jdk22Foreign.booleanSetterHandle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    if (value) 1 else 0,
                ) as Int,
            )
            hresult.requireSuccess("invokeBooleanSetter($vtableIndex)")
        }
    }

    override fun invokeFloat64Setter(instance: ComPtr, vtableIndex: Int, value: Double): Result<Unit> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                Jdk22Foreign.float64SetterHandle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value,
                ) as Int,
            )
            hresult.requireSuccess("invokeFloat64Setter($vtableIndex)")
        }
    }

    override fun invokeInt64Setter(instance: ComPtr, vtableIndex: Int, value: Long): Result<Unit> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                Jdk22Foreign.int64SetterHandle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value,
                ) as Int,
            )
            hresult.requireSuccess("invokeInt64Setter($vtableIndex)")
        }
    }

    override fun invokeUInt64Setter(instance: ComPtr, vtableIndex: Int, value: ULong): Result<Unit> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                Jdk22Foreign.uint64SetterHandle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value.toLong(),
                ) as Int,
            )
            hresult.requireSuccess("invokeUInt64Setter($vtableIndex)")
        }
    }

    override fun invokeInt32Method(instance: ComPtr, vtableIndex: Int): Result<Int> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeInt32Method",
            handle = Jdk22Foreign.int32MethodHandle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
            reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L) },
        )
    }

    override fun invokeInt32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Int> {
        return runCatching {
            JvmComMethodExecutor.withHStringArg(value) { input ->
                JvmComMethodExecutor.invokeWithOutSegment(
                    instance = instance,
                    vtableIndex = vtableIndex,
                    operation = "invokeInt32MethodWithStringArg",
                    handle = Jdk22Foreign.int32MethodWithStringHandle,
                    allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
                    reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L) },
                    input,
                ).getOrThrow()
            }
        }
    }

    override fun invokeInt32MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Int> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeInt32MethodWithInt32Arg",
            handle = Jdk22Foreign.int32MethodWithInt32Handle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
            reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L) },
            value,
        )
    }

    override fun invokeInt32MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Int> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeInt32MethodWithUInt32Arg",
            handle = Jdk22Foreign.int32MethodWithUInt32Handle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
            reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L) },
            value.toInt(),
        )
    }

    override fun invokeInt32MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Int> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeInt32MethodWithObjectArg",
            handle = Jdk22Foreign.int32MethodWithObjectHandle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
            reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L) },
            if (value.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(value),
        )
    }

    override fun invokeUInt32Method(instance: ComPtr, vtableIndex: Int): Result<UInt> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUInt32Method",
            handle = Jdk22Foreign.uint32MethodHandle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
            reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L).toUInt() },
        )
    }

    override fun invokeUInt32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<UInt> {
        return runCatching {
            JvmComMethodExecutor.withHStringArg(value) { input ->
                JvmComMethodExecutor.invokeWithOutSegment(
                    instance = instance,
                    vtableIndex = vtableIndex,
                    operation = "invokeUInt32MethodWithStringArg",
                    handle = Jdk22Foreign.uint32MethodWithStringHandle,
                    allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
                    reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L).toUInt() },
                    input,
                ).getOrThrow()
            }
        }
    }

    override fun invokeUInt32MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<UInt> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUInt32MethodWithInt32Arg",
            handle = Jdk22Foreign.uint32MethodWithInt32Handle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
            reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L).toUInt() },
            value,
        )
    }

    override fun invokeUInt32MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<UInt> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUInt32MethodWithUInt32Arg",
            handle = Jdk22Foreign.uint32MethodWithUInt32Handle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
            reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L).toUInt() },
            value.toInt(),
        )
    }

    override fun invokeUInt32MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<UInt> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeUInt32MethodWithObjectArg",
            handle = Jdk22Foreign.uint32MethodWithObjectHandle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
            reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L).toUInt() },
            if (value.isNull) MemorySegment.NULL else Jdk22Foreign.pointerOf(value),
        )
    }

    override fun invokeBooleanGetter(instance: ComPtr, vtableIndex: Int): Result<Boolean> {
        return JvmComMethodExecutor.invokeWithOutSegment(
            instance = instance,
            vtableIndex = vtableIndex,
            operation = "invokeBooleanGetter",
            handle = Jdk22Foreign.booleanGetterHandle,
            allocator = { arena -> arena.allocate(ValueLayout.JAVA_INT) },
            reader = { segment -> segment.get(ValueLayout.JAVA_INT, 0L) != 0 },
        )
    }

    override fun invokeBooleanMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Boolean> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.JAVA_INT)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.booleanMethodWithInputHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        Jdk22Foreign.pointerOf(value),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeBooleanMethodWithObjectArg($vtableIndex)")
                resultSegment.get(ValueLayout.JAVA_INT, 0L) != 0
            }
        }
    }

    override fun invokeBooleanMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Boolean> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val hString = JvmWinRtRuntime.createHString(value)
            try {
                Arena.ofConfined().use { arena ->
                    val resultSegment = arena.allocate(ValueLayout.JAVA_INT)
                    val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                    val hresult = HResult(
                        Jdk22Foreign.booleanMethodWithInputHandle.bindTo(function).invokeWithArguments(
                            Jdk22Foreign.pointerOf(instance),
                            MemorySegment.ofAddress(hString.raw),
                            resultSegment,
                        ) as Int,
                    )
                    hresult.requireSuccess("invokeBooleanMethodWithStringArg($vtableIndex)")
                    resultSegment.get(ValueLayout.JAVA_INT, 0L) != 0
                }
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
            }
        }
    }

    override fun invokeBooleanMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Boolean> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.JAVA_INT)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.booleanMethodWithUInt32Handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        value.toInt(),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeBooleanMethodWithUInt32Arg($vtableIndex)")
                resultSegment.get(ValueLayout.JAVA_INT, 0L) != 0
            }
        }
    }

    override fun invokeFloat64Method(instance: ComPtr, vtableIndex: Int): Result<Double> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.JAVA_DOUBLE)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.float64MethodHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeFloat64Method($vtableIndex)")
                resultSegment.get(ValueLayout.JAVA_DOUBLE, 0L)
            }
        }
    }

    override fun invokeFloat32Method(instance: ComPtr, vtableIndex: Int): Result<Float> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.JAVA_FLOAT)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.float32MethodHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeFloat32Method($vtableIndex)")
                resultSegment.get(ValueLayout.JAVA_FLOAT, 0L)
            }
        }
    }

    override fun invokeFloat32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Float> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val hString = JvmWinRtRuntime.createHString(value)
            try {
                Arena.ofConfined().use { arena ->
                    val resultSegment = arena.allocate(ValueLayout.JAVA_FLOAT)
                    val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                    val hresult = HResult(
                        Jdk22Foreign.float32MethodWithInputHandle.bindTo(function).invokeWithArguments(
                            Jdk22Foreign.pointerOf(instance),
                            MemorySegment.ofAddress(hString.raw),
                            resultSegment,
                        ) as Int,
                    )
                    hresult.requireSuccess("invokeFloat32MethodWithStringArg($vtableIndex)")
                    resultSegment.get(ValueLayout.JAVA_FLOAT, 0L)
                }
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
            }
        }
    }

    override fun invokeFloat32MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Float> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.JAVA_FLOAT)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.float32MethodWithUInt32Handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        value.toInt(),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeFloat32MethodWithUInt32Arg($vtableIndex)")
                resultSegment.get(ValueLayout.JAVA_FLOAT, 0L)
            }
        }
    }

    override fun invokeFloat64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Double> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val hString = JvmWinRtRuntime.createHString(value)
            try {
                Arena.ofConfined().use { arena ->
                    val resultSegment = arena.allocate(ValueLayout.JAVA_DOUBLE)
                    val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                    val hresult = HResult(
                        Jdk22Foreign.float64MethodWithInputHandle.bindTo(function).invokeWithArguments(
                            Jdk22Foreign.pointerOf(instance),
                            MemorySegment.ofAddress(hString.raw),
                            resultSegment,
                        ) as Int,
                    )
                    hresult.requireSuccess("invokeFloat64MethodWithStringArg($vtableIndex)")
                    resultSegment.get(ValueLayout.JAVA_DOUBLE, 0L)
                }
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
            }
        }
    }

    override fun invokeFloat64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Double> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.JAVA_DOUBLE)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.float64MethodWithUInt32Handle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        value.toInt(),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeFloat64MethodWithUInt32Arg($vtableIndex)")
                resultSegment.get(ValueLayout.JAVA_DOUBLE, 0L)
            }
        }
    }

    override fun invokeGuidGetter(instance: ComPtr, vtableIndex: Int): Result<Guid> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(16)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.guidGetterHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeGuidGetter($vtableIndex)")
                Jdk22Foreign.guidFromSegment(resultSegment)
            }
        }
    }

    override fun invokeInt64Getter(instance: ComPtr, vtableIndex: Int): Result<Long> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.JAVA_LONG)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.int64GetterHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeInt64Getter($vtableIndex)")
                resultSegment.get(ValueLayout.JAVA_LONG, 0L)
            }
        }
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
