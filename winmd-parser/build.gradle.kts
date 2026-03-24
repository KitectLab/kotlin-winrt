plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

kotlin {
    jvmToolchain(22)
}

dependencies {
    implementation(projects.winmdParserPlugin)
    testImplementation(libs.junit)
}

application {
    mainClass = "dev.winrt.winmd.parser.MainKt"
}
