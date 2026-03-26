plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

fun Project.stringListProperty(name: String): List<String> =
    providers.gradleProperty(name)
        .orNull
        ?.split(',')
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?: emptyList()

kotlin {
    jvmToolchain(22)
    jvm()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.kom)
            implementation(projects.winrtCore)
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

val winmdParserMainOutput = files(
    project(":winmd-parser").layout.buildDirectory.dir("classes/kotlin/main"),
    project(":winmd-parser").layout.buildDirectory.dir("resources/main"),
)

val generateBindings by tasks.registering(JavaExec::class) {
    group = "code generation"
    description = "Generates checked-in WinRT bindings from local WinMD fixtures."
    dependsOn(project(":winmd-parser").tasks.named("classes"))

    val outputDir = layout.projectDirectory.dir("src/commonMain/kotlin")
    val fixtureDir = rootProject.layout.projectDirectory.file("metadata/Windows.WinRT.winmd")

    classpath(
        winmdParserMainOutput,
        project(":winmd-parser").configurations.getByName("runtimeClasspath"),
    )
    mainClass = "dev.winrt.winmd.parser.MainKt"
    args(outputDir.asFile.absolutePath, fixtureDir.asFile.absolutePath)
}

val generateBindingsFromSdk by tasks.registering(JavaExec::class) {
    group = "code generation"
    description = "Generates WinRT bindings from installed Windows SDK contracts with optional namespace filters."
    dependsOn(project(":winmd-parser").tasks.named("classes"))

    val outputDir = providers.gradleProperty("winmd.outputDir")
        .orElse(layout.buildDirectory.dir("generated/sdk-bindings").map { it.asFile.absolutePath })
    val contracts = stringListProperty("winmd.contracts")
    val namespaces = stringListProperty("winmd.namespaces")
    val sdkVersion = providers.gradleProperty("winmd.sdkVersion").orNull
    val windowsKitsRoot = providers.gradleProperty("winmd.windowsKitsRoot").orNull
    val referencesRoot = providers.gradleProperty("winmd.referencesRoot").orNull

    doFirst {
        require(contracts.isNotEmpty()) {
            "Set -Pwinmd.contracts=ContractA,ContractB to choose Windows SDK contracts."
        }

        val resolvedArgs = mutableListOf(outputDir.get())
        contracts.forEach { resolvedArgs += "--contract=$it" }
        namespaces.forEach { resolvedArgs += "--namespace=$it" }
        sdkVersion?.let { resolvedArgs += "--sdk-version=$it" }
        windowsKitsRoot?.let { resolvedArgs += "--windows-kits-root=$it" }
        referencesRoot?.let { resolvedArgs += "--references-root=$it" }

        args = resolvedArgs
    }

    classpath(
        winmdParserMainOutput,
        project(":winmd-parser").configurations.getByName("runtimeClasspath"),
    )
    mainClass = "dev.winrt.winmd.parser.MainKt"
}

val regenerateCheckedInBindingsFromSdk by tasks.registering(JavaExec::class) {
    group = "code generation"
    description = "Regenerates checked-in bindings from installed Windows SDK contracts with optional namespace filters."
    dependsOn(project(":winmd-parser").tasks.named("classes"))

    val outputDir = layout.projectDirectory.dir("src/commonMain/kotlin")
    val contracts = stringListProperty("winmd.contracts")
    val namespaces = stringListProperty("winmd.namespaces")
    val sdkVersion = providers.gradleProperty("winmd.sdkVersion").orNull
    val windowsKitsRoot = providers.gradleProperty("winmd.windowsKitsRoot").orNull
    val referencesRoot = providers.gradleProperty("winmd.referencesRoot").orNull

    doFirst {
        require(contracts.isNotEmpty()) {
            "Set -Pwinmd.contracts=ContractA,ContractB to choose Windows SDK contracts."
        }

        val resolvedArgs = mutableListOf(outputDir.asFile.absolutePath)
        contracts.forEach { resolvedArgs += "--contract=$it" }
        namespaces.forEach { resolvedArgs += "--namespace=$it" }
        sdkVersion?.let { resolvedArgs += "--sdk-version=$it" }
        windowsKitsRoot?.let { resolvedArgs += "--windows-kits-root=$it" }
        referencesRoot?.let { resolvedArgs += "--references-root=$it" }

        args = resolvedArgs
    }

    classpath(
        winmdParserMainOutput,
        project(":winmd-parser").configurations.getByName("runtimeClasspath"),
    )
    mainClass = "dev.winrt.winmd.parser.MainKt"
}
