package windows.globalization.numberformatting

import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.PlatformRuntime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class NumeralSystemTranslatorProjectionTest {
    @Test
    fun can_activate_numeral_system_translator_and_translate() {
        assumeTrue(PlatformRuntime.isWindows)

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val factory = JvmWinRtRuntime.getActivationFactory(NumeralSystemTranslator.qualifiedName).getOrThrow()
            try {
                val translator = NumeralSystemTranslator(JvmWinRtRuntime.activateInstance(factory).getOrThrow())
                try {
                    val projected = translator.asINumeralSystemTranslator()
                    try {
                        val languages = projected.languages
                        try {
                            assertTrue(languages.size > 0)
                            assertFalse(languages[0].isBlank())
                        } finally {
                            PlatformComInterop.release(languages.pointer)
                        }
                        assertFalse(projected.resolvedLanguage.isBlank())
                        assertFalse(projected.numeralSystem.isBlank())
                        assertFalse(projected.translateNumerals("123").isBlank())
                    } finally {
                        PlatformComInterop.release(projected.pointer)
                    }
                } finally {
                    PlatformComInterop.release(translator.pointer)
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
