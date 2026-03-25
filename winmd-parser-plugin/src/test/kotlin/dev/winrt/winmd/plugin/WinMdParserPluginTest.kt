package dev.winrt.winmd.plugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertNotNull
import org.junit.Test

class WinMdParserPluginTest {
    @Test
    fun registers_winmd_extension() {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply("dev.winrt.winmd-parser")

        assertNotNull(project.extensions.findByName("winmd"))
    }
}
