rootProject.name = "kotlin-winrt"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    ":kom",
    ":winrt-core",
    ":winmd-parser-plugin",
    ":winmd-parser",
    ":generated-winrt-bindings",
    ":sample-jvm-winui3",
)
