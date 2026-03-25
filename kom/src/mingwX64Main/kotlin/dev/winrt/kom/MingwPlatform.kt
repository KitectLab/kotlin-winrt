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

    override fun invokeUnitMethod(instance: ComPtr, vtableIndex: Int): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Native Unit method invocation is not wired yet"),
        )
    }

    override fun addRef(instance: ComPtr): UInt = 1u

    override fun release(instance: ComPtr): UInt = 0u

    override fun invokeHStringMethod(instance: ComPtr, vtableIndex: Int): Result<HString> {
        return Result.failure(
            UnsupportedOperationException("Native HSTRING invocation is not wired yet"),
        )
    }

    override fun invokeHStringMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<HString> {
        return Result.failure(
            UnsupportedOperationException("Native HSTRING invocation with input is not wired yet"),
        )
    }

    override fun invokeObjectMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<ComPtr> {
        return Result.failure(
            UnsupportedOperationException("Native object invocation with HSTRING input is not wired yet"),
        )
    }

    override fun invokeObjectMethod(instance: ComPtr, vtableIndex: Int): Result<ComPtr> {
        return Result.failure(
            UnsupportedOperationException("Native object invocation is not wired yet"),
        )
    }

    override fun invokeStringSetter(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Native HSTRING setter invocation is not wired yet"),
        )
    }

    override fun invokeUInt32Method(instance: ComPtr, vtableIndex: Int): Result<UInt> {
        return Result.failure(
            UnsupportedOperationException("Native UInt32 method invocation is not wired yet"),
        )
    }

    override fun invokeBooleanGetter(instance: ComPtr, vtableIndex: Int): Result<Boolean> {
        return Result.failure(
            UnsupportedOperationException("Native Boolean getter invocation is not wired yet"),
        )
    }

    override fun invokeBooleanMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Boolean> {
        return Result.failure(
            UnsupportedOperationException("Native Boolean method invocation with input is not wired yet"),
        )
    }

    override fun invokeFloat64Method(instance: ComPtr, vtableIndex: Int): Result<Double> {
        return Result.failure(
            UnsupportedOperationException("Native Float64 method invocation is not wired yet"),
        )
    }

    override fun invokeFloat64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Double> {
        return Result.failure(
            UnsupportedOperationException("Native Float64 method invocation with input is not wired yet"),
        )
    }

    override fun invokeGuidGetter(instance: ComPtr, vtableIndex: Int): Result<Guid> {
        return Result.failure(
            UnsupportedOperationException("Native GUID getter invocation is not wired yet"),
        )
    }

    override fun invokeInt64Getter(instance: ComPtr, vtableIndex: Int): Result<Long> {
        return Result.failure(
            UnsupportedOperationException("Native Int64 getter invocation is not wired yet"),
        )
    }
}

actual object PlatformHStringBridge : HStringBridge {
    override fun create(value: String): HString = HString.NULL

    override fun toKotlinString(value: HString): String = ""

    override fun release(value: HString) {
    }
}
