package dev.winrt.kom

interface ComReference {
    val pointer: ComPtr
}

interface ComInterop {
    fun queryInterface(instance: ComPtr, iid: Guid): Result<ComPtr>
    fun addRef(instance: ComPtr): UInt
    fun release(instance: ComPtr): UInt
    fun invokeHStringMethod(instance: ComPtr, vtableIndex: Int): Result<HString>
    fun invokeStringSetter(instance: ComPtr, vtableIndex: Int, value: String): Result<Unit>
}

expect object PlatformComInterop : ComInterop
