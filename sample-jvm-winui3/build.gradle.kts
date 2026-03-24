plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

kotlin {
    jvmToolchain(22)
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

dependencies {
    implementation(projects.generatedWinrtBindings)
    implementation(projects.winrtCore)
    implementation(projects.kom)
    testImplementation(libs.junit)
}

application {
    mainClass = "dev.winrt.sample.jvm.MainKt"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}
