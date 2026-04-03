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

fun Project.winMdSourceArgs(
    contracts: List<String>,
    namespaces: List<String>,
    sdkVersion: String?,
    windowsKitsRoot: String?,
    referencesRoot: String?,
): List<String> {
    val explicitWinMdFiles = stringListProperty("winmd.files")
    val nugetComponents = stringListProperty("winmd.nugetComponents")
    val legacyNugetPackage = providers.gradleProperty("winmd.nugetPackage").orNull
    return buildList {
        addAll(explicitWinMdFiles)
        if (nugetComponents.isNotEmpty()) {
            val nugetRoot = providers.gradleProperty("winmd.nugetRoot").orNull
            nugetComponents.forEach { component ->
                add("--nuget-component=$component")
            }
            nugetRoot?.let { add("--nuget-root=$it") }
        } else if (legacyNugetPackage != null) {
            val nugetVersion = providers.gradleProperty("winmd.nugetVersion").orNull
                ?: error("Set -Pwinmd.nugetVersion=<version> when using -Pwinmd.nugetPackage.")
            add("--nuget-component=$legacyNugetPackage@$nugetVersion")
            providers.gradleProperty("winmd.nugetRoot").orNull?.let { add("--nuget-root=$it") }
        }
        contracts.forEach { add("--contract=$it") }
        namespaces.forEach { add("--namespace=$it") }
        sdkVersion?.let { add("--sdk-version=$it") }
        windowsKitsRoot?.let { add("--windows-kits-root=$it") }
        referencesRoot?.let { add("--references-root=$it") }
        require(isNotEmpty()) {
            "Set -Pwinmd.files=<a.winmd,b.winmd>, -Pwinmd.nugetComponents=<id@version,id2@version2>, -Pwinmd.nugetPackage=<id> with -Pwinmd.nugetVersion=<version>, or -Pwinmd.contracts=ContractA,ContractB to choose WinMD inputs."
        }
    }
}

fun JavaExec.configureWinMdParserClasspath() {
    classpath(
        winmdParserMainOutput,
        project(":winmd-parser").configurations.getByName("runtimeClasspath"),
    )
    mainClass = "dev.winrt.winmd.parser.MainKt"
}

fun registerPresetSdkGenerationTask(
    name: String,
    descriptionText: String,
    outputDir: String,
    contracts: List<String>,
    namespaces: List<String>,
) = tasks.register(name, JavaExec::class) {
    group = "code generation"
    description = descriptionText
    notCompatibleWithConfigurationCache("WinMD generation tasks build dynamic JavaExec args from Gradle properties.")
    dependsOn(project(":winmd-parser").tasks.named("classes"))

    args(
        layout.buildDirectory.dir(outputDir).get().asFile.absolutePath,
        *contracts.map { "--contract=$it" }.toTypedArray(),
        *namespaces.map { "--namespace=$it" }.toTypedArray(),
    )
    configureWinMdParserClasspath()
}

fun registerPresetSdkRegenerationTask(
    name: String,
    descriptionText: String,
    contracts: List<String>,
    namespaces: List<String>,
) = tasks.register(name, JavaExec::class) {
    group = "code generation"
    description = descriptionText
    dependsOn(project(":winmd-parser").tasks.named("classes"))

    args(
        layout.projectDirectory.dir("src/commonMain/kotlin").asFile.absolutePath,
        *contracts.map { "--contract=$it" }.toTypedArray(),
        *namespaces.map { "--namespace=$it" }.toTypedArray(),
    )
    configureWinMdParserClasspath()
}

fun registerPresetNuGetGenerationTask(
    name: String,
    descriptionText: String,
    outputDir: String,
    packageId: String,
    namespaces: List<String>,
    contracts: List<String> = emptyList(),
    versionProperty: String = "winmd.nugetVersion",
    rootProperty: String = "winmd.nugetRoot",
) = tasks.register(name, JavaExec::class) {
    group = "code generation"
    description = descriptionText
    notCompatibleWithConfigurationCache("WinMD generation tasks build dynamic JavaExec args from Gradle properties.")
    dependsOn(project(":winmd-parser").tasks.named("classes"))

    doFirst {
        val packageVersion = providers.gradleProperty(versionProperty).orNull
            ?: error("Set -P$versionProperty=<version> to choose the NuGet package version.")
        val nugetRoot = providers.gradleProperty(rootProperty).orNull
        val resolvedArgs = mutableListOf(layout.buildDirectory.dir(outputDir).get().asFile.absolutePath)
        resolvedArgs += "--nuget-component=$packageId@$packageVersion"
        nugetRoot?.let { resolvedArgs += "--nuget-root=$it" }
        contracts.forEach { resolvedArgs += "--contract=$it" }
        namespaces.forEach { resolvedArgs += "--namespace=$it" }
        args = resolvedArgs
    }

    configureWinMdParserClasspath()
}

fun registerPresetNuGetRegenerationTask(
    name: String,
    descriptionText: String,
    packageId: String,
    namespaces: List<String>,
    contracts: List<String> = emptyList(),
    versionProperty: String = "winmd.nugetVersion",
    rootProperty: String = "winmd.nugetRoot",
) = tasks.register(name, JavaExec::class) {
    group = "code generation"
    description = descriptionText
    notCompatibleWithConfigurationCache("WinMD generation tasks build dynamic JavaExec args from Gradle properties.")
    dependsOn(project(":winmd-parser").tasks.named("classes"))

    doFirst {
        val packageVersion = providers.gradleProperty(versionProperty).orNull
            ?: error("Set -P$versionProperty=<version> to choose the NuGet package version.")
        val nugetRoot = providers.gradleProperty(rootProperty).orNull
        val resolvedArgs = mutableListOf(layout.projectDirectory.dir("src/commonMain/kotlin").asFile.absolutePath)
        resolvedArgs += "--nuget-component=$packageId@$packageVersion"
        nugetRoot?.let { resolvedArgs += "--nuget-root=$it" }
        contracts.forEach { resolvedArgs += "--contract=$it" }
        namespaces.forEach { resolvedArgs += "--namespace=$it" }
        args = resolvedArgs
    }

    configureWinMdParserClasspath()
}

kotlin {
    jvmToolchain(22)
    jvm()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.kom)
            implementation(projects.winrtCore)
            implementation(libs.kotlinx.coroutines.core)
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
    notCompatibleWithConfigurationCache("WinMD generation tasks build dynamic JavaExec args from Gradle properties.")
    dependsOn(project(":winmd-parser").tasks.named("classes"))

    val outputDir = layout.projectDirectory.dir("src/commonMain/kotlin")
    val fixtureDir = rootProject.layout.projectDirectory.file("metadata/Windows.WinRT.winmd")

    configureWinMdParserClasspath()
    args(outputDir.asFile.absolutePath, fixtureDir.asFile.absolutePath)
}

val generateBindingsFromSdk by tasks.registering(JavaExec::class) {
    group = "code generation"
    description = "Generates WinRT bindings from installed Windows SDK contracts with optional namespace filters."
    notCompatibleWithConfigurationCache("WinMD generation tasks build dynamic JavaExec args from Gradle properties.")
    dependsOn(project(":winmd-parser").tasks.named("classes"))

    val outputDir = providers.gradleProperty("winmd.outputDir")
        .orElse(layout.buildDirectory.dir("generated/sdk-bindings").map { it.asFile.absolutePath })
    val contracts = stringListProperty("winmd.contracts")
    val namespaces = stringListProperty("winmd.namespaces")
    val sdkVersion = providers.gradleProperty("winmd.sdkVersion").orNull
    val windowsKitsRoot = providers.gradleProperty("winmd.windowsKitsRoot").orNull
    val referencesRoot = providers.gradleProperty("winmd.referencesRoot").orNull

    doFirst {
        val resolvedArgs = mutableListOf(outputDir.get())
        resolvedArgs += winMdSourceArgs(
            contracts = contracts,
            namespaces = namespaces,
            sdkVersion = sdkVersion,
            windowsKitsRoot = windowsKitsRoot,
            referencesRoot = referencesRoot,
        )

        args = resolvedArgs
    }

    configureWinMdParserClasspath()
}

val regenerateCheckedInBindingsFromSdk by tasks.registering(JavaExec::class) {
    group = "code generation"
    description = "Regenerates checked-in bindings from installed Windows SDK contracts with optional namespace filters."
    notCompatibleWithConfigurationCache("WinMD generation tasks build dynamic JavaExec args from Gradle properties.")
    dependsOn(project(":winmd-parser").tasks.named("classes"))

    val outputDir = layout.projectDirectory.dir("src/commonMain/kotlin")
    val contracts = stringListProperty("winmd.contracts")
    val namespaces = stringListProperty("winmd.namespaces")
    val sdkVersion = providers.gradleProperty("winmd.sdkVersion").orNull
    val windowsKitsRoot = providers.gradleProperty("winmd.windowsKitsRoot").orNull
    val referencesRoot = providers.gradleProperty("winmd.referencesRoot").orNull

    doFirst {
        val resolvedArgs = mutableListOf(outputDir.asFile.absolutePath)
        resolvedArgs += winMdSourceArgs(
            contracts = contracts,
            namespaces = namespaces,
            sdkVersion = sdkVersion,
            windowsKitsRoot = windowsKitsRoot,
            referencesRoot = referencesRoot,
        )

        args = resolvedArgs
    }

    configureWinMdParserClasspath()
}

val generateGlobalizationBindingsFromSdk by registerPresetSdkGenerationTask(
    name = "generateGlobalizationBindingsFromSdk",
    descriptionText = "Generates Windows.Globalization bindings from installed Windows SDK contracts.",
    outputDir = "generated/presets/windows-globalization",
    contracts = listOf(
        "Windows.Foundation.UniversalApiContract",
        "Windows.Foundation.FoundationContract",
    ),
    namespaces = listOf("Windows.Globalization"),
)

val generateFoundationBindingsFromSdk by registerPresetSdkGenerationTask(
    name = "generateFoundationBindingsFromSdk",
    descriptionText = "Generates Windows.Foundation bindings from installed Windows SDK contracts.",
    outputDir = "generated/presets/windows-foundation",
    contracts = listOf(
        "Windows.Foundation.UniversalApiContract",
        "Windows.Foundation.FoundationContract",
    ),
    namespaces = listOf("Windows.Foundation"),
)

val generateJsonBindingsFromSdk by registerPresetSdkGenerationTask(
    name = "generateJsonBindingsFromSdk",
    descriptionText = "Generates Windows.Data.Json bindings from installed Windows SDK contracts.",
    outputDir = "generated/presets/windows-data-json",
    contracts = listOf(
        "Windows.Foundation.UniversalApiContract",
        "Windows.Foundation.FoundationContract",
    ),
    namespaces = listOf("Windows.Data.Json"),
)

val generateWinUiBindingsFromNuGet by registerPresetNuGetGenerationTask(
    name = "generateWinUiBindingsFromNuGet",
    descriptionText = "Generates Microsoft.UI.Xaml bindings from the Microsoft.WindowsAppSDK NuGet package.",
    outputDir = "generated/presets/microsoft-ui-xaml",
    packageId = "Microsoft.WindowsAppSDK",
    contracts = listOf(
        "Windows.Foundation.UniversalApiContract",
        "Windows.Foundation.FoundationContract",
    ),
    namespaces = listOf("Microsoft.UI.Xaml"),
)

val regenerateGlobalizationBindingsFromSdk by registerPresetSdkRegenerationTask(
    name = "regenerateGlobalizationBindingsFromSdk",
    descriptionText = "Regenerates checked-in Windows.Globalization bindings from installed Windows SDK contracts.",
    contracts = listOf(
        "Windows.Foundation.UniversalApiContract",
        "Windows.Foundation.FoundationContract",
    ),
    namespaces = listOf("Windows.Globalization"),
)

val regenerateFoundationBindingsFromSdk by registerPresetSdkRegenerationTask(
    name = "regenerateFoundationBindingsFromSdk",
    descriptionText = "Regenerates checked-in Windows.Foundation bindings from installed Windows SDK contracts.",
    contracts = listOf(
        "Windows.Foundation.UniversalApiContract",
        "Windows.Foundation.FoundationContract",
    ),
    namespaces = listOf("Windows.Foundation"),
)

val regenerateJsonBindingsFromSdk by registerPresetSdkRegenerationTask(
    name = "regenerateJsonBindingsFromSdk",
    descriptionText = "Regenerates checked-in Windows.Data.Json bindings from installed Windows SDK contracts.",
    contracts = listOf(
        "Windows.Foundation.UniversalApiContract",
        "Windows.Foundation.FoundationContract",
    ),
    namespaces = listOf("Windows.Data.Json"),
)

val regenerateWinUiBindingsFromNuGet by registerPresetNuGetRegenerationTask(
    name = "regenerateWinUiBindingsFromNuGet",
    descriptionText = "Regenerates checked-in Microsoft.UI.Xaml bindings from the Microsoft.WindowsAppSDK NuGet package.",
    packageId = "Microsoft.WindowsAppSDK",
    contracts = listOf(
        "Windows.Foundation.UniversalApiContract",
        "Windows.Foundation.FoundationContract",
    ),
    namespaces = listOf("Microsoft.UI.Xaml"),
)
