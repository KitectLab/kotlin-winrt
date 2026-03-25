import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
}

allprojects {
    group = "dev.winrt"
    version = "0.1.0-SNAPSHOT"

    tasks.withType<Test>().configureEach {
        maxParallelForks = 1
        maxHeapSize = "256m"
        systemProperty(
            "dev.winrt.enableProbe",
            providers.gradleProperty("dev.winrt.enableProbe").orNull ?: "false",
        )
    }
}
