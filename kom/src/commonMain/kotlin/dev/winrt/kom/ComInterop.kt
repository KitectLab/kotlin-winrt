package dev.winrt.kom

interface ComReference {
    val pointer: ComPtr
}

interface ComInterop {
    fun queryInterface(instance: ComPtr, iid: Guid): Result<ComPtr>
    fun addRef(instance: ComPtr): UInt
    fun release(instance: ComPtr): UInt
    fun invokeUnitMethod(instance: ComPtr, vtableIndex: Int): Result<Unit>
    fun invokeHStringMethod(instance: ComPtr, vtableIndex: Int): Result<HString>
    fun invokeHStringMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<HString>
    fun invokeObjectMethod(instance: ComPtr, vtableIndex: Int): Result<ComPtr>
    fun invokeObjectMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<ComPtr>
    fun invokeObjectMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<ComPtr>
    fun invokeStringSetter(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit>
    fun invokeUInt32Method(instance: ComPtr, vtableIndex: Int): Result<UInt>
    fun invokeBooleanGetter(instance: ComPtr, vtableIndex: Int): Result<Boolean>
    fun invokeBooleanMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Boolean>
    fun invokeFloat64Method(instance: ComPtr, vtableIndex: Int): Result<Double>
    fun invokeFloat64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Double>
    fun invokeGuidGetter(instance: ComPtr, vtableIndex: Int): Result<Guid>
    fun invokeInt64Getter(instance: ComPtr, vtableIndex: Int): Result<Long>
}

expect object PlatformComInterop : ComInterop
