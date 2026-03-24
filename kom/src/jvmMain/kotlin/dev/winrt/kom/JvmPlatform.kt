package dev.winrt.kom

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

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
}

actual object PlatformHStringBridge : HStringBridge {
    override fun create(value: String): HString = HString(value)

    override fun toKotlinString(value: HString): String = value.value
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
