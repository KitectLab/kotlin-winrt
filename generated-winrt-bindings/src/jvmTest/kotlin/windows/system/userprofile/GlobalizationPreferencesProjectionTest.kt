package windows.system.userprofile

import dev.winrt.core.UInt32
import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.PlatformRuntime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class GlobalizationPreferencesProjectionTest {
    @Test
    fun can_read_globalization_preferences_statics() {
        assumeTrue(PlatformRuntime.isWindows)

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val staticsPointer = JvmWinRtRuntime
                .getActivationFactory(
                    GlobalizationPreferences.qualifiedName,
                    IGlobalizationPreferencesStatics.iid,
                )
                .getOrThrow()
            try {
                val statics = IGlobalizationPreferencesStatics(staticsPointer)
                val calendars = statics.calendars
                val clocks = statics.clocks
                val currencies = statics.currencies
                val languages = statics.languages
                try {
                    assertTrue(calendars.size.value > 0u)
                    assertTrue(clocks.size.value > 0u)
                    assertTrue(currencies.size.value > 0u)
                    assertTrue(languages.size.value > 0u)
                    assertFalse(calendars.getAt(UInt32(0u)).isBlank())
                    assertFalse(clocks.getAt(UInt32(0u)).isBlank())
                    assertFalse(currencies.getAt(UInt32(0u)).isBlank())
                    assertFalse(languages.getAt(UInt32(0u)).isBlank())
                    assertFalse(statics.homeGeographicRegion.isBlank())
                    assertTrue(statics.weekStartsOn.value in 0..6)
                } finally {
                    PlatformComInterop.release(calendars.pointer)
                    PlatformComInterop.release(clocks.pointer)
                    PlatformComInterop.release(currencies.pointer)
                    PlatformComInterop.release(languages.pointer)
                }
            } finally {
                PlatformComInterop.release(staticsPointer)
            }
        } finally {
            if (shouldUninitializeRo) {
                JvmWinRtRuntime.uninitialize()
            }
            if (shouldUninitializeCom && roResult != KnownHResults.RPC_E_CHANGED_MODE) {
                JvmComRuntime.uninitialize()
            }
        }
    }
}
