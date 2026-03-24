package dev.winrt.kom

actual object PlatformRuntime {
    actual val platformName: String = "jvm"
    actual val isWindows: Boolean = System.getProperty("os.name").contains("Windows", ignoreCase = true)
    actual val ffiBackend: String = "jdk22-ffm"
}

actual object PlatformComInterop : ComInterop {
    override fun queryInterface(instance: ComPtr, iid: Guid): Result<ComPtr> {
        return Result.failure(
            UnsupportedOperationException(
                "FFM COM bridge is scaffolded but QueryInterface dispatch is not implemented yet for $iid on ${instance.value.rawValue}",
            ),
        )
    }

    override fun addRef(instance: ComPtr): UInt {
        Jdk22Foreign.pointerOf(instance)
        return 1u
    }

    override fun release(instance: ComPtr): UInt {
        Jdk22Foreign.pointerOf(instance)
        return 0u
    }
}

actual object PlatformHStringBridge : HStringBridge {
    override fun create(value: String): HString = HString(value)

    override fun toKotlinString(value: HString): String = value.value
}
