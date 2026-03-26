package dev.winrt.sample.jvm

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
}
