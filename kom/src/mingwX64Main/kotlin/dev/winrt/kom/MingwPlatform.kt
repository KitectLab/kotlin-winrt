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

    override fun invokeHStringMethod(instance: ComPtr, vtableIndex: Int): Result<HString> {
        return Result.failure(
            UnsupportedOperationException("Native HSTRING invocation is not wired yet"),
        )
    }

    override fun invokeStringSetter(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Native HSTRING setter invocation is not wired yet"),
        )
    }
}

actual object PlatformHStringBridge : HStringBridge {
    override fun create(value: String): HString = HString.NULL

    override fun toKotlinString(value: HString): String = ""

    override fun release(value: HString) {
    }
}
