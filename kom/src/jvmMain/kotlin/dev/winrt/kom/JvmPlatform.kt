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
}

actual object PlatformHStringBridge : HStringBridge {
    override fun create(value: String): HString = JvmWinRtRuntime.createHString(value)

    override fun toKotlinString(value: HString): String = JvmWinRtRuntime.toKotlinString(value)

    override fun release(value: HString) {
        JvmWinRtRuntime.releaseHString(value)
    }
}

object JvmComRuntime {
    private const val coinitMultithreaded = 0

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
    private const val roInitMultithreaded = 1
    val iidIActivationFactory = Guid(
        data1 = 0x00000035,
        data2 = 0,
        data3 = 0,
        data4 = byteArrayOf(0xC0.toByte(), 0, 0, 0, 0, 0, 0, 0x46),
    )

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
            rawBuffer.reinterpret(length.toLong() * 2).getString(0L, StandardCharsets.UTF_16LE)
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
