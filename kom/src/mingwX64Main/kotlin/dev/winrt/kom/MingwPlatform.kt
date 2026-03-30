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

    override fun invokeUnitMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Native Unit method invocation with Int32 input is not wired yet"),
        )
    }

    override fun invokeUnitMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Native Unit method invocation with UInt32 input is not wired yet"),
        )
    }

    override fun invokeUnitMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Native Unit method invocation with Int64 input is not wired yet"),
        )
    }

    override fun invokeUnitMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Native Unit method invocation with String input is not wired yet"),
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

    override fun invokeHStringMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<HString> {
        return Result.failure(
            UnsupportedOperationException("Native HSTRING invocation with Int32 input is not wired yet"),
        )
    }

    override fun invokeHStringMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<HString> {
        return Result.failure(
            UnsupportedOperationException("Native HSTRING invocation with UInt32 input is not wired yet"),
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

    override fun invokeObjectMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<ComPtr> {
        return Result.failure(
            UnsupportedOperationException("Native object invocation with UInt32 input is not wired yet"),
        )
    }

    override fun invokeObjectSetter(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Native object setter invocation is not wired yet"),
        )
    }

    override fun invokeInt64MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Long> {
        return Result.failure(
            UnsupportedOperationException("Native Int64 method invocation with object input is not wired yet"),
        )
    }

    override fun invokeInt64Method(instance: ComPtr, vtableIndex: Int): Result<Long> {
        return Result.failure(
            UnsupportedOperationException("Native Int64 method invocation is not wired yet"),
        )
    }

    override fun invokeInt64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Long> {
        return Result.failure(
            UnsupportedOperationException("Native Int64 method invocation with String input is not wired yet"),
        )
    }

    override fun invokeInt64MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Long> {
        return Result.failure(
            UnsupportedOperationException("Native Int64 method invocation with Int32 input is not wired yet"),
        )
    }

    override fun invokeInt64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Long> {
        return Result.failure(
            UnsupportedOperationException("Native Int64 method invocation with UInt32 input is not wired yet"),
        )
    }

    override fun invokeInt64MethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<Long> {
        return Result.failure(
            UnsupportedOperationException("Native Int64 method invocation with Boolean input is not wired yet"),
        )
    }

    override fun invokeUInt64Method(instance: ComPtr, vtableIndex: Int): Result<ULong> {
        return Result.failure(
            UnsupportedOperationException("Native UInt64 method invocation is not wired yet"),
        )
    }

    override fun invokeUInt64MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<ULong> {
        return Result.failure(
            UnsupportedOperationException("Native UInt64 method invocation with object input is not wired yet"),
        )
    }

    override fun invokeUInt64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<ULong> {
        return Result.failure(
            UnsupportedOperationException("Native UInt64 method invocation with String input is not wired yet"),
        )
    }

    override fun invokeUInt64MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<ULong> {
        return Result.failure(
            UnsupportedOperationException("Native UInt64 method invocation with Int32 input is not wired yet"),
        )
    }

    override fun invokeUInt64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<ULong> {
        return Result.failure(
            UnsupportedOperationException("Native UInt64 method invocation with UInt32 input is not wired yet"),
        )
    }

    override fun invokeUInt64MethodWithBooleanArg(instance: ComPtr, vtableIndex: Int, value: Boolean): Result<ULong> {
        return Result.failure(
            UnsupportedOperationException("Native UInt64 method invocation with Boolean input is not wired yet"),
        )
    }

    override fun invokeStringSetter(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Native HSTRING setter invocation is not wired yet"),
        )
    }

    override fun invokeInt32Setter(instance: ComPtr, vtableIndex: Int, value: Int): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Native Int32 setter invocation is not wired yet"),
        )
    }

    override fun invokeUInt32Setter(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Native UInt32 setter invocation is not wired yet"),
        )
    }

    override fun invokeFloat32Setter(instance: ComPtr, vtableIndex: Int, value: Float): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Native Float32 setter invocation is not wired yet"),
        )
    }

    override fun invokeInt32Method(instance: ComPtr, vtableIndex: Int): Result<Int> {
        return Result.failure(
            UnsupportedOperationException("Native Int32 method invocation is not wired yet"),
        )
    }

    override fun invokeInt32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Int> {
        return Result.failure(
            UnsupportedOperationException("Native Int32 method invocation with String input is not wired yet"),
        )
    }

    override fun invokeInt32MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Int> {
        return Result.failure(
            UnsupportedOperationException("Native Int32 method invocation with Int32 input is not wired yet"),
        )
    }

    override fun invokeInt32MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Int> {
        return Result.failure(
            UnsupportedOperationException("Native Int32 method invocation with UInt32 input is not wired yet"),
        )
    }

    override fun invokeInt32MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Int> {
        return Result.failure(
            UnsupportedOperationException("Native Int32 method invocation with object input is not wired yet"),
        )
    }

    override fun invokeUInt32Method(instance: ComPtr, vtableIndex: Int): Result<UInt> {
        return Result.failure(
            UnsupportedOperationException("Native UInt32 method invocation is not wired yet"),
        )
    }

    override fun invokeUInt32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<UInt> {
        return Result.failure(
            UnsupportedOperationException("Native UInt32 method invocation with String input is not wired yet"),
        )
    }

    override fun invokeUInt32MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<UInt> {
        return Result.failure(
            UnsupportedOperationException("Native UInt32 method invocation with Int32 input is not wired yet"),
        )
    }

    override fun invokeUInt32MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<UInt> {
        return Result.failure(
            UnsupportedOperationException("Native UInt32 method invocation with UInt32 input is not wired yet"),
        )
    }

    override fun invokeUInt32MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<UInt> {
        return Result.failure(
            UnsupportedOperationException("Native UInt32 method invocation with object input is not wired yet"),
        )
    }

    override fun invokeBooleanGetter(instance: ComPtr, vtableIndex: Int): Result<Boolean> {
        return Result.failure(
            UnsupportedOperationException("Native Boolean getter invocation is not wired yet"),
        )
    }

    override fun invokeBooleanMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Boolean> {
        return Result.failure(
            UnsupportedOperationException("Native Boolean method invocation with object input is not wired yet"),
        )
    }

    override fun invokeBooleanMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Boolean> {
        return Result.failure(
            UnsupportedOperationException("Native Boolean method invocation with input is not wired yet"),
        )
    }

    override fun invokeBooleanMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Boolean> {
        return Result.failure(
            UnsupportedOperationException("Native Boolean method invocation with UInt32 input is not wired yet"),
        )
    }

    override fun invokeFloat64Method(instance: ComPtr, vtableIndex: Int): Result<Double> {
        return Result.failure(
            UnsupportedOperationException("Native Float64 method invocation is not wired yet"),
        )
    }

    override fun invokeFloat32Method(instance: ComPtr, vtableIndex: Int): Result<Float> {
        return Result.failure(
            UnsupportedOperationException("Native Float32 method invocation is not wired yet"),
        )
    }

    override fun invokeFloat32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Float> {
        return Result.failure(
            UnsupportedOperationException("Native Float32 method invocation with input is not wired yet"),
        )
    }

    override fun invokeFloat32MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Float> {
        return Result.failure(
            UnsupportedOperationException("Native Float32 method invocation with UInt32 input is not wired yet"),
        )
    }

    override fun invokeFloat64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Double> {
        return Result.failure(
            UnsupportedOperationException("Native Float64 method invocation with input is not wired yet"),
        )
    }

    override fun invokeFloat64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Double> {
        return Result.failure(
            UnsupportedOperationException("Native Float64 method invocation with UInt32 input is not wired yet"),
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
