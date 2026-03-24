package dev.winrt.kom

actual object PlatformRuntime {
    actual val platformName: String = "mingwX64"
    actual val isWindows: Boolean = true
    actual val ffiBackend: String = "kotlin-native-cinterop"
}

actual object PlatformComInterop : ComInterop {
    override fun queryInterface(instance: ComPtr, iid: Guid): Result<ComPtr> {
        return Result.failure(
            UnsupportedOperationException("Native COM interop is not wired yet for $iid"),
        )
    }

    override fun addRef(instance: ComPtr): UInt = 1u

    override fun release(instance: ComPtr): UInt = 0u
}

actual object PlatformHStringBridge : HStringBridge {
    override fun create(value: String): HString = HString(value)

    override fun toKotlinString(value: HString): String = value.value
}
