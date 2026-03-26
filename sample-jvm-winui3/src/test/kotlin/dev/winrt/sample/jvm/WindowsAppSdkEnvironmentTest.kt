package dev.winrt.sample.jvm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowsAppSdkEnvironmentTest {
    @Test
    fun parses_package_counts() {
        val state = WindowsAppSdkEnvironment.parse(
            """
            framework=2
            main=0
            singleton=1
            """.trimIndent(),
        )

        assertTrue(state.frameworkInstalled)
        assertFalse(state.mainInstalled)
        assertTrue(state.singletonInstalled)
    }

    @Test
    fun classifies_missing_main_package() {
        val state = WindowsAppSdkEnvironment.parse(
            """
            framework=1
            main=0
            singleton=1
            """.trimIndent(),
        )

        assertEquals(WindowsAppSdkEnvironment.Readiness.MissingMain, state.readiness())
    }

    @Test
    fun classifies_ready_environment() {
        val state = WindowsAppSdkEnvironment.parse(
            """
            framework=1
            main=1
            singleton=1
            """.trimIndent(),
        )

        assertEquals(WindowsAppSdkEnvironment.Readiness.Ready, state.readiness())
    }
}
