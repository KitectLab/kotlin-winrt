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
        minHeapSize = "64m"
        maxHeapSize = "128m"
        jvmArgs("-XX:+UseSerialGC")
        systemProperty(
            "dev.winrt.enableProbe",
            providers.gradleProperty("dev.winrt.enableProbe").orNull ?: "false",
        )
        systemProperty(
            "dev.winrt.probeTarget",
            providers.gradleProperty("dev.winrt.probeTarget").orNull ?: "",
        )
        systemProperty(
            "dev.winrt.probeMode",
            providers.gradleProperty("dev.winrt.probeMode").orNull ?: "",
        )
        systemProperty(
            "dev.winrt.bootstrapDll",
            providers.systemProperty("dev.winrt.bootstrapDll").orNull ?: "",
        )
        systemProperty(
            "dev.winrt.windowsAppSdkRoot",
            providers.systemProperty("dev.winrt.windowsAppSdkRoot").orNull ?: "",
        )
    }
}
