package dev.winrt.kom

object KnownHResults {
    val E_NOTIMPL = HResult(0x80004001.toInt())
    val E_NOINTERFACE = HResult(0x80004002.toInt())
    val E_POINTER = HResult(0x80004003.toInt())
    val E_FAIL = HResult(0x80004005.toInt())
    val E_BOUNDS = HResult(0x8000000B.toInt())
    val E_INVALIDARG = HResult(0x80070057.toInt())
    val REGDB_E_CLASSNOTREG = HResult(0x80040154.toInt())
    val RPC_E_CHANGED_MODE = HResult(0x80010106.toInt())
    val S_FALSE = HResult(1)
}
