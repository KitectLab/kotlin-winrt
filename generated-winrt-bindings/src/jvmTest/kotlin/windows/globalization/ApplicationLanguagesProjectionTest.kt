package windows.globalization

import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.PlatformRuntime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class ApplicationLanguagesProjectionTest {
    @Test
    fun can_read_application_languages_statics() {
        assumeTrue(PlatformRuntime.isWindows)

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val factory = JvmWinRtRuntime
                .getActivationFactory(
                    ApplicationLanguages.qualifiedName,
                    IApplicationLanguagesStatics.iid,
                )
                .getOrThrow()
            try {
                val statics = IApplicationLanguagesStatics(factory)
                val languages = statics.languages
                try {
                    assertTrue(languages.size > 0)
                    assertFalse(languages[0].isBlank())
                } finally {
                    PlatformComInterop.release(languages.pointer)
                }
            } finally {
                PlatformComInterop.release(factory)
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
