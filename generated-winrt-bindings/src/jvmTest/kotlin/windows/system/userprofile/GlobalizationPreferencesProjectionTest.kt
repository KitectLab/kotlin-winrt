package windows.system.userprofile

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
                    assertTrue(calendars.size > 0)
                    assertTrue(clocks.size > 0)
                    assertTrue(currencies.size > 0)
                    assertTrue(languages.size > 0)
                    assertFalse(calendars[0].isBlank())
                    assertFalse(clocks[0].isBlank())
                    assertFalse(currencies[0].isBlank())
                    assertFalse(languages[0].isBlank())
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
