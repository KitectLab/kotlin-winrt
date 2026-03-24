plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

kotlin {
    jvmToolchain(22)
    jvm()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.junit)
        }
    }
}
