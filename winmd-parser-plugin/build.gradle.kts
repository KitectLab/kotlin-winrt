plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(22)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
