package dev.winrt.kom

interface ComReference {
    val pointer: ComPtr
}

interface ComInterop {
    fun queryInterface(instance: ComPtr, iid: Guid): Result<ComPtr>
    fun addRef(instance: ComPtr): UInt
    fun release(instance: ComPtr): UInt
}

expect object PlatformComInterop : ComInterop
