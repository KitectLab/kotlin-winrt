plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
}

allprojects {
    group = "dev.winrt"
    version = "0.1.0-SNAPSHOT"
}
