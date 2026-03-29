package dev.winrt.kom

interface ComReference {
    val pointer: ComPtr
}

interface ComInterop {
    fun queryInterface(instance: ComPtr, iid: Guid): Result<ComPtr>
    fun addRef(instance: ComPtr): UInt
    fun release(instance: ComPtr): UInt
    fun invokeUnitMethod(instance: ComPtr, vtableIndex: Int): Result<Unit>
    fun invokeUnitMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Unit>
    fun invokeUnitMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Unit>
    fun invokeUnitMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit>
    fun invokeHStringMethod(instance: ComPtr, vtableIndex: Int): Result<HString>
    fun invokeHStringMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<HString>
    fun invokeHStringMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<HString>
    fun invokeHStringMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<HString>
    fun invokeObjectMethod(instance: ComPtr, vtableIndex: Int): Result<ComPtr>
    fun invokeObjectMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<ComPtr>
    fun invokeObjectMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<ComPtr>
    fun invokeObjectSetter(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Unit>
    fun invokeStringSetter(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit>
    fun invokeInt32Setter(instance: ComPtr, vtableIndex: Int, value: Int): Result<Unit>
    fun invokeInt32Method(instance: ComPtr, vtableIndex: Int): Result<Int>
    fun invokeUInt32Method(instance: ComPtr, vtableIndex: Int): Result<UInt>
    fun invokeBooleanGetter(instance: ComPtr, vtableIndex: Int): Result<Boolean>
    fun invokeBooleanMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Boolean>
    fun invokeBooleanMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Boolean>
    fun invokeBooleanMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Boolean>
    fun invokeFloat64Method(instance: ComPtr, vtableIndex: Int): Result<Double>
    fun invokeFloat64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Double>
    fun invokeFloat64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Double>
    fun invokeGuidGetter(instance: ComPtr, vtableIndex: Int): Result<Guid>
    fun invokeInt64Getter(instance: ComPtr, vtableIndex: Int): Result<Long>
}

expect object PlatformComInterop : ComInterop
