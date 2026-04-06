package dev.winrt.core

import dev.winrt.kom.HResult
import dev.winrt.kom.KnownHResults

class WinRtException(
    val hResult: HResult,
) : Exception("WinRT HRESULT 0x${hResult.value.toUInt().toString(16).padStart(8, '0')}")

fun exceptionFromHResult(value: Int): Exception? {
    if (value >= 0) {
        return null
    }
    val hResult = HResult(value)
    return when (hResult) {
        KnownHResults.E_INVALIDARG -> IllegalArgumentException()
        KnownHResults.E_BOUNDS -> IndexOutOfBoundsException()
        KnownHResults.E_POINTER -> NullPointerException()
        KnownHResults.E_NOTIMPL -> UnsupportedOperationException()
        else -> WinRtException(hResult)
    }
}

fun hResultOfException(error: Exception?): Int {
    return when (error) {
        null -> HResult.OK.value
        is WinRtException -> error.hResult.value
        is IllegalArgumentException -> KnownHResults.E_INVALIDARG.value
        is IndexOutOfBoundsException -> KnownHResults.E_BOUNDS.value
        is NullPointerException -> KnownHResults.E_POINTER.value
        is UnsupportedOperationException -> KnownHResults.E_NOTIMPL.value
        else -> KnownHResults.E_FAIL.value
    }
}
