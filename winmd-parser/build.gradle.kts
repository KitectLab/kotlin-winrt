plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

kotlin {
    jvmToolchain(22)
}

dependencies {
    implementation(projects.kom)
    implementation(projects.winrtCore)
    implementation(projects.winmdParserPlugin)
    implementation(libs.kotlinpoet)
    testImplementation(libs.junit)
}

application {
    mainClass = "dev.winrt.winmd.parser.MainKt"
}
