package dev.winrt.kom

interface ComReference {
    val pointer: ComPtr
}

enum class ComMethodResultKind {
    HSTRING,
    OBJECT,
    UINT8,
    INT16,
    UINT16,
    CHAR16,
    INT32,
    UINT32,
    INT64,
    UINT64,
    BOOLEAN,
    FLOAT32,
    FLOAT64,
    GUID,
}

sealed interface ComMethodResult {
    data class HStringValue(val value: HString) : ComMethodResult
    data class ObjectValue(val value: ComPtr) : ComMethodResult
    data class UInt8Value(val value: UByte) : ComMethodResult
    data class Int16Value(val value: Short) : ComMethodResult
    data class UInt16Value(val value: UShort) : ComMethodResult
    data class Char16Value(val value: Char) : ComMethodResult
    data class Int32Value(val value: Int) : ComMethodResult
    data class UInt32Value(val value: UInt) : ComMethodResult
    data class Int64Value(val value: Long) : ComMethodResult
    data class UInt64Value(val value: ULong) : ComMethodResult
    data class BooleanValue(val value: Boolean) : ComMethodResult
    data class Float32Value(val value: Float) : ComMethodResult
    data class Float64Value(val value: Double) : ComMethodResult
    data class GuidValue(val value: Guid) : ComMethodResult
}

data class ComposableMethodResult(
    val instance: ComPtr,
    val inner: ComPtr,
)

fun ComMethodResult.requireHString(): HString = (this as ComMethodResult.HStringValue).value
fun ComMethodResult.requireObject(): ComPtr = (this as ComMethodResult.ObjectValue).value
fun ComMethodResult.requireUInt8(): UByte = (this as ComMethodResult.UInt8Value).value
fun ComMethodResult.requireInt16(): Short = (this as ComMethodResult.Int16Value).value
fun ComMethodResult.requireUInt16(): UShort = (this as ComMethodResult.UInt16Value).value
fun ComMethodResult.requireChar16(): Char = (this as ComMethodResult.Char16Value).value
fun ComMethodResult.requireInt32(): Int = (this as ComMethodResult.Int32Value).value
fun ComMethodResult.requireUInt32(): UInt = (this as ComMethodResult.UInt32Value).value
fun ComMethodResult.requireInt64(): Long = (this as ComMethodResult.Int64Value).value
fun ComMethodResult.requireUInt64(): ULong = (this as ComMethodResult.UInt64Value).value
fun ComMethodResult.requireBoolean(): Boolean = (this as ComMethodResult.BooleanValue).value
fun ComMethodResult.requireFloat32(): Float = (this as ComMethodResult.Float32Value).value
fun ComMethodResult.requireFloat64(): Double = (this as ComMethodResult.Float64Value).value
fun ComMethodResult.requireGuid(): Guid = (this as ComMethodResult.GuidValue).value

interface ComInterop {
    fun queryInterface(instance: ComPtr, iid: Guid): Result<ComPtr>
    fun addRef(instance: ComPtr): UInt
    fun release(instance: ComPtr): UInt

    // Core kernel
    fun invokeUnitMethod(instance: ComPtr, vtableIndex: Int): Result<Unit>
    fun invokeUnitMethodWithArgs(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Unit>
    fun invokeUnitMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Unit>
    fun invokeUnitMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Unit>
    fun invokeUnitMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Unit>
    fun invokeUnitMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit>
    fun invokeUnitMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Unit>
    fun invokeUnitMethodWithFloat32Arg(instance: ComPtr, vtableIndex: Int, value: Float): Result<Unit>
    fun invokeUnitMethodWithFloat64Arg(instance: ComPtr, vtableIndex: Int, value: Double): Result<Unit>
    fun invokeUnitMethodWithTwoInt32Args(instance: ComPtr, vtableIndex: Int, first: Int, second: Int): Result<Unit>
    fun invokeUnitMethodWithInt32AndInt64Args(instance: ComPtr, vtableIndex: Int, first: Int, second: Long): Result<Unit>
    fun invokeUnitMethodWithInt64AndInt32Args(instance: ComPtr, vtableIndex: Int, first: Long, second: Int): Result<Unit>
    fun invokeUnitMethodWithTwoInt64Args(instance: ComPtr, vtableIndex: Int, first: Long, second: Long): Result<Unit>
    fun invokeUnitMethodWithObjectAndInt32Args(instance: ComPtr, vtableIndex: Int, first: ComPtr, second: Int): Result<Unit>
    fun invokeUnitMethodWithInt32AndObjectArgs(instance: ComPtr, vtableIndex: Int, first: Int, second: ComPtr): Result<Unit>
    fun invokeUnitMethodWithObjectAndInt64Args(instance: ComPtr, vtableIndex: Int, first: ComPtr, second: Long): Result<Unit>
    fun invokeUnitMethodWithInt64AndObjectArgs(instance: ComPtr, vtableIndex: Int, first: Long, second: ComPtr): Result<Unit>
    fun invokeUnitMethodWithStringAndInt32Args(instance: ComPtr, vtableIndex: Int, first: String, second: Int): Result<Unit>
    fun invokeUnitMethodWithInt32AndStringArgs(instance: ComPtr, vtableIndex: Int, first: Int, second: String): Result<Unit>
    fun invokeUnitMethodWithStringAndInt64Args(instance: ComPtr, vtableIndex: Int, first: String, second: Long): Result<Unit>
    fun invokeUnitMethodWithInt64AndStringArgs(instance: ComPtr, vtableIndex: Int, first: Long, second: String): Result<Unit>
    fun invokeUnitMethodWithTwoStringArgs(instance: ComPtr, vtableIndex: Int, first: String, second: String): Result<Unit>
    fun invokeUnitMethodWithObjectAndStringArgs(instance: ComPtr, vtableIndex: Int, first: ComPtr, second: String): Result<Unit>
    fun invokeUnitMethodWithStringAndObjectArgs(instance: ComPtr, vtableIndex: Int, first: String, second: ComPtr): Result<Unit>
    fun invokeUnitMethodWithTwoObjectArgs(instance: ComPtr, vtableIndex: Int, first: ComPtr, second: ComPtr): Result<Unit>
    fun invokeHStringMethod(instance: ComPtr, vtableIndex: Int): Result<HString>
    fun invokeHStringMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<HString>
    fun invokeHStringMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<HString>
    fun invokeHStringMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<HString>
    fun invokeHStringMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<HString>
    fun invokeHStringMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<HString>
    fun invokeObjectMethod(instance: ComPtr, vtableIndex: Int): Result<ComPtr>
    fun invokeTwoObjectMethod(instance: ComPtr, vtableIndex: Int): Result<Pair<ComPtr, ComPtr>>
    fun invokeObjectMethodWithArgs(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<ComPtr>
    fun invokeObjectMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<ComPtr>
    fun invokeObjectMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<ComPtr>
    fun invokeObjectMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<ComPtr>
    fun invokeObjectMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<ComPtr>

    // Special paths
    fun invokeStructMethodWithArgs(
        instance: ComPtr,
        vtableIndex: Int,
        layout: ComStructLayout,
        vararg arguments: Any,
    ): Result<ComStructValue>
    fun invokeMethodWithObjectAndInt32Args(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: ComPtr, second: Int): Result<ComMethodResult>
    fun invokeMethodWithInt32AndObjectArgs(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: Int, second: ComPtr): Result<ComMethodResult>
    fun invokeMethodWithObjectAndInt64Args(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: ComPtr, second: Long): Result<ComMethodResult>
    fun invokeMethodWithInt64AndObjectArgs(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: Long, second: ComPtr): Result<ComMethodResult>
    fun invokeMethodWithStringAndInt32Args(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: String, second: Int): Result<ComMethodResult>
    fun invokeMethodWithInt32AndStringArgs(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: Int, second: String): Result<ComMethodResult>
    fun invokeMethodWithStringAndInt64Args(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: String, second: Long): Result<ComMethodResult>
    fun invokeMethodWithInt64AndStringArgs(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: Long, second: String): Result<ComMethodResult>
    fun invokeMethodWithTwoInt32Args(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: Int, second: Int): Result<ComMethodResult>
    fun invokeMethodWithInt32AndInt64Args(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: Int, second: Long): Result<ComMethodResult>
    fun invokeMethodWithInt64AndInt32Args(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: Long, second: Int): Result<ComMethodResult>
    fun invokeMethodWithTwoInt64Args(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: Long, second: Long): Result<ComMethodResult>
    fun invokeMethodWithObjectAndStringArgs(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: ComPtr, second: String): Result<ComMethodResult>
    fun invokeMethodWithStringAndObjectArgs(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: String, second: ComPtr): Result<ComMethodResult>
    fun invokeMethodWithTwoObjectArgs(instance: ComPtr, vtableIndex: Int, resultKind: ComMethodResultKind, first: ComPtr, second: ComPtr): Result<ComMethodResult>
    fun invokeMethodWithResultKind(
        instance: ComPtr,
        vtableIndex: Int,
        resultKind: ComMethodResultKind,
        vararg arguments: Any,
    ): Result<ComMethodResult>
    fun invokeIndexOfMethod(
        instance: ComPtr,
        vtableIndex: Int,
        vararg arguments: Any,
    ): Result<Pair<Boolean, UInt>>
    fun invokeComposableMethod(
        instance: ComPtr,
        vtableIndex: Int,
        vararg arguments: Any,
    ): Result<ComposableMethodResult>
    fun invokeInt64MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Long>
    fun invokeInt64Method(instance: ComPtr, vtableIndex: Int): Result<Long>
    fun invokeInt64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Long>
    fun invokeInt64MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Long>
    fun invokeInt64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Long>
    fun invokeUInt64Method(instance: ComPtr, vtableIndex: Int): Result<ULong>
    fun invokeUInt64MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<ULong>
    fun invokeUInt64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<ULong>
    fun invokeUInt64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<ULong>
    fun invokeInt32Method(instance: ComPtr, vtableIndex: Int): Result<Int>
    fun invokeInt32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Int>
    fun invokeInt32MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Int>
    fun invokeInt32MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Int>
    fun invokeInt32MethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Int>
    fun invokeInt32MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Int>
    fun invokeUInt32Method(instance: ComPtr, vtableIndex: Int): Result<UInt>
    fun invokeUInt32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<UInt>
    fun invokeUInt32MethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<UInt>
    fun invokeUInt32MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<UInt>
    fun invokeUInt32MethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<UInt>
    fun invokeUInt32MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<UInt>
    fun invokeBooleanGetter(instance: ComPtr, vtableIndex: Int): Result<Boolean>
    fun invokeBooleanMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Boolean>
    fun invokeBooleanMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Boolean>
    fun invokeBooleanMethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Boolean>
    fun invokeBooleanMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Boolean>
    fun invokeFloat32Method(instance: ComPtr, vtableIndex: Int): Result<Float>
    fun invokeFloat32MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Float>
    fun invokeFloat32MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Float>
    fun invokeFloat32MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Float>
    fun invokeFloat32MethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Float>
    fun invokeFloat64Method(instance: ComPtr, vtableIndex: Int): Result<Double>
    fun invokeFloat64MethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Double>
    fun invokeFloat64MethodWithUInt32Arg(instance: ComPtr, vtableIndex: Int, value: UInt): Result<Double>
    fun invokeFloat64MethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Double>
    fun invokeFloat64MethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Double>
    fun invokeGuidGetter(instance: ComPtr, vtableIndex: Int): Result<Guid>
    fun invokeGuidMethodWithStringArg(instance: ComPtr, vtableIndex: Int, value: String): Result<Guid>
    fun invokeGuidMethodWithInt32Arg(instance: ComPtr, vtableIndex: Int, value: Int): Result<Guid>
    fun invokeGuidMethodWithObjectArg(instance: ComPtr, vtableIndex: Int, value: ComPtr): Result<Guid>
    fun invokeGuidMethodWithInt64Arg(instance: ComPtr, vtableIndex: Int, value: Long): Result<Guid>
    fun invokeInt64Getter(instance: ComPtr, vtableIndex: Int): Result<Long>
}

expect val PlatformComInterop: ComInterop
