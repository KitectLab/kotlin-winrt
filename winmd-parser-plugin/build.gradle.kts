plugins {
    alias(libs.plugins.kotlinJvm)
    `java-gradle-plugin`
}

kotlin {
    jvmToolchain(22)
}

gradlePlugin {
    plugins {
        create("winmdParser") {
            id = "dev.winrt.winmd-parser"
            implementationClass = "dev.winrt.winmd.plugin.WinMdParserPlugin"
        }
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(gradleApi())
    testImplementation(libs.junit)
    testImplementation(gradleTestKit())
}
