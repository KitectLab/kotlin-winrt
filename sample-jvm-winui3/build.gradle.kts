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

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty(
        "dev.winrt.bootstrapDll",
        providers.systemProperty("dev.winrt.bootstrapDll").orNull ?: "",
    )
    systemProperty(
        "dev.winrt.windowsAppSdkRoot",
        providers.systemProperty("dev.winrt.windowsAppSdkRoot").orNull ?: "",
    )
    systemProperty(
        "dev.winrt.autoQuitVisible",
        providers.systemProperty("dev.winrt.autoQuitVisible").orNull ?: "",
    )
}

dependencies {
    implementation(projects.generatedWinrtBindings)
    implementation(projects.winrtCore)
    implementation(projects.kom)
    testImplementation(libs.junit)
}

application {
    mainClass = "dev.winrt.sample.jvm.MainKt"
}
