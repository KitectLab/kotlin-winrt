package dev.winrt.core

import dev.winrt.kom.KnownHResults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HResultExceptionMappingTest {
    @Test
    fun maps_success_hresult_to_null() {
        assertNull(exceptionFromHResult(0))
        assertEquals(0, hResultOfException(null))
    }

    @Test
    fun maps_illegal_argument_hresult_to_illegal_argument_exception() {
        val error = exceptionFromHResult(KnownHResults.E_INVALIDARG.value)

        assertTrue(error is IllegalArgumentException)
        assertEquals(KnownHResults.E_INVALIDARG.value, hResultOfException(error))
    }

    @Test
    fun maps_unknown_hresult_to_winrt_exception() {
        val error = exceptionFromHResult(KnownHResults.E_FAIL.value)

        assertTrue(error is WinRtException)
        assertEquals(KnownHResults.E_FAIL.value, hResultOfException(error))
    }
}
