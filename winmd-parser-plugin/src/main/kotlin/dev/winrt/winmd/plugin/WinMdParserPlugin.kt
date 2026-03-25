package dev.winrt.winmd.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class WinMdParserPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("winmd", WinMdExtension::class.java)
    }
}
