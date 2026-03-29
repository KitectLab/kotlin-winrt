package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.ProjectionException
import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.PlatformRuntime
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeNoException
import org.junit.Assume.assumeTrue
import org.junit.Test

class IStringableProjectionTest {
    @Test
    fun can_project_iStringable_and_call_toString() {
        assumeTrue(PlatformRuntime.isWindows)

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val factory = JvmWinRtRuntime.getActivationFactory("Windows.Globalization.Calendar").getOrThrow()
            try {
                val instance = JvmWinRtRuntime.activateInstance(factory).getOrThrow()
                try {
                    val projected = try {
                        IStringable.from(Inspectable(instance))
                    } catch (error: ProjectionException) {
                        assumeNoException("Windows.Globalization.Calendar did not expose IStringable on this runtime", error)
                        return
                    }
                    try {
                        val value = projected.toString()
                        assertFalse(value.isBlank())
                    } finally {
                    }
                } finally {
                    PlatformComInterop.release(instance)
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
