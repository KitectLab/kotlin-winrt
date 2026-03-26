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

actual object PlatformComInterop : ComInterop {
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
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                Jdk22Foreign.unitMethodHandle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                ) as Int,
            )
            hresult.requireSuccess("invokeUnitMethod($vtableIndex)")
        }
    }

    override fun invokeUnitMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Unit> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                Jdk22Foreign.unitMethodWithInt32Handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value,
                ) as Int,
            )
            hresult.requireSuccess("invokeUnitMethodWithInt32Arg($vtableIndex)")
        }
    }

    override fun invokeUnitMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Unit> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
            val hresult = HResult(
                Jdk22Foreign.unitMethodWithInt64Handle.bindTo(function).invokeWithArguments(
                    Jdk22Foreign.pointerOf(instance),
                    value,
                ) as Int,
            )
            hresult.requireSuccess("invokeUnitMethodWithInt64Arg($vtableIndex)")
        }
    }

    override fun invokeHStringMethod(instance: ComPtr, vtableIndex: Int): Result<HString> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.ADDRESS)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.hstringMethodHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeHStringMethod($vtableIndex)")
                HString(resultSegment.get(ValueLayout.ADDRESS, 0L).address())
            }
        }
    }

    override fun invokeHStringMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<HString> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val hString = JvmWinRtRuntime.createHString(value)
            try {
                Arena.ofConfined().use { arena ->
                    val resultSegment = arena.allocate(ValueLayout.ADDRESS)
                    val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                    val hresult = HResult(
                        Jdk22Foreign.hstringMethodWithInputHandle.bindTo(function).invokeWithArguments(
                            Jdk22Foreign.pointerOf(instance),
                            MemorySegment.ofAddress(hString.raw),
                            resultSegment,
                        ) as Int,
                    )
                    hresult.requireSuccess("invokeHStringMethodWithStringArg($vtableIndex)")
                    HString(resultSegment.get(ValueLayout.ADDRESS, 0L).address())
                }
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
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
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val hString = JvmWinRtRuntime.createHString(value)
            try {
                Arena.ofConfined().use { arena ->
                    val resultSegment = arena.allocate(ValueLayout.ADDRESS)
                    val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                    val hresult = HResult(
                        Jdk22Foreign.objectMethodWithInputHandle.bindTo(function).invokeWithArguments(
                            Jdk22Foreign.pointerOf(instance),
                            MemorySegment.ofAddress(hString.raw),
                            resultSegment,
                        ) as Int,
                    )
                    hresult.requireSuccess("invokeObjectMethodWithStringArg($vtableIndex)")
                    Jdk22Foreign.addressResult(resultSegment.get(ValueLayout.ADDRESS, 0L))
                }
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
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
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            val hString = JvmWinRtRuntime.createHString(value)
            try {
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.hstringSetterHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        MemorySegment.ofAddress(hString.raw),
                    ) as Int,
                )
                hresult.requireSuccess("invokeStringSetter($vtableIndex)")
            } finally {
                JvmWinRtRuntime.releaseHString(hString)
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

    override fun invokeInt32Method(instance: ComPtr, vtableIndex: Int): Result<Int> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.JAVA_INT)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.int32MethodHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeInt32Method($vtableIndex)")
                resultSegment.get(ValueLayout.JAVA_INT, 0L)
            }
        }
    }

    override fun invokeUInt32Method(instance: ComPtr, vtableIndex: Int): Result<UInt> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.JAVA_INT)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.uint32MethodHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeUInt32Method($vtableIndex)")
                resultSegment.get(ValueLayout.JAVA_INT, 0L).toUInt()
            }
        }
    }

    override fun invokeBooleanGetter(instance: ComPtr, vtableIndex: Int): Result<Boolean> {
        if (instance.isNull) {
            return Result.failure(KomException("Method invocation requires a non-null COM pointer"))
        }

        return runCatching {
            Arena.ofConfined().use { arena ->
                val resultSegment = arena.allocate(ValueLayout.JAVA_INT)
                val function = Jdk22Foreign.vtableEntry(instance, vtableIndex)
                val hresult = HResult(
                    Jdk22Foreign.booleanGetterHandle.bindTo(function).invokeWithArguments(
                        Jdk22Foreign.pointerOf(instance),
                        resultSegment,
                    ) as Int,
                )
                hresult.requireSuccess("invokeBooleanGetter($vtableIndex)")
                resultSegment.get(ValueLayout.JAVA_INT, 0L) != 0
            }
        }
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

actual object PlatformHStringBridge : HStringBridge {
    override fun create(value: String): HString = JvmWinRtRuntime.createHString(value)

    override fun toKotlinString(value: HString): String = JvmWinRtRuntime.toKotlinString(value)

    override fun release(value: HString) {
        JvmWinRtRuntime.releaseHString(value)
    }
}

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
