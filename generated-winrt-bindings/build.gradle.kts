plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

import java.net.URI

fun versionKey(value: String): List<Int> =
    value.split('.').map { token -> token.toIntOrNull() ?: 0 }

fun compareVersions(left: String, right: String): Int {
    val leftParts = versionKey(left)
    val rightParts = versionKey(right)
    val maxSize = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until maxSize) {
        val leftValue = leftParts.getOrElse(index) { 0 }
        val rightValue = rightParts.getOrElse(index) { 0 }
        if (leftValue != rightValue) {
            return leftValue.compareTo(rightValue)
        }
    }
    return 0
}

fun Project.stringListProperty(name: String): List<String> =
    providers.gradleProperty(name)
        .orNull
        ?.split(',')
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?: emptyList()

fun Project.nuGetSourceEntries(): List<Pair<String, String>> =
    stringListProperty("winmd.nugetSources")
        .map { entry ->
            val separatorIndex = entry.indexOf('=')
            if (separatorIndex > 0) {
                entry.substring(0, separatorIndex).trim() to entry.substring(separatorIndex + 1).trim()
            } else {
                val sanitizedName = entry
                    .substringAfterLast('/')
                    .substringAfterLast('\\')
                    .substringAfter("://")
                    .substringBefore('?')
                    .replace(Regex("[^A-Za-z0-9._-]+"), "-")
                    .trim('-')
                    .ifBlank { "source" }
                sanitizedName to entry
            }
        }

fun Project.nuGetComponentSpecs(): List<String> =
    stringListProperty("winmd.nugetComponents").ifEmpty {
        providers.gradleProperty("winmd.nugetPackage")
            .orNull
            ?.let { packageId ->
                val packageVersion = providers.gradleProperty("winmd.nugetVersion").orNull
                    ?: error("Set -Pwinmd.nugetVersion=<version> when using -Pwinmd.nugetPackage.")
                listOf("$packageId@$packageVersion")
            }
            ?: emptyList()
    }

fun Project.winMdNuGetSourceArgs(packageId: String? = null): List<String> {
    val configuredSources = stringListProperty("winmd.nugetSources")
    if (configuredSources.isNotEmpty()) {
        return configuredSources.map { "--nuget-source=$it" }
    }
    return if (packageId == "Microsoft.WindowsAppSDK") {
        listOf("--nuget-source=https://api.nuget.org/v3/index.json")
    } else {
        emptyList()
    }
}

fun Project.winMdSourceArgs(
    contracts: List<String>,
    namespaces: List<String>,
    sdkVersion: String?,
    windowsKitsRoot: String?,
    referencesRoot: String?,
): List<String> {
    val explicitWinMdFiles = stringListProperty("winmd.files")
    val nugetComponents = nuGetComponentSpecs()
    val nugetSources = nuGetSourceEntries()
    val legacyNugetPackage = providers.gradleProperty("winmd.nugetPackage").orNull
    return buildList {
        addAll(explicitWinMdFiles)
        if (nugetComponents.isNotEmpty()) {
            val nugetRoot = providers.gradleProperty("winmd.nugetRoot").orNull
            nugetComponents.forEach { component ->
                add("--nuget-component=$component")
            }
            nugetSources.forEach { (_, source) -> add("--nuget-source=$source") }
            nugetRoot?.let { add("--nuget-root=$it") }
        } else if (legacyNugetPackage != null) {
            val nugetVersion = providers.gradleProperty("winmd.nugetVersion").orNull
                ?: error("Set -Pwinmd.nugetVersion=<version> when using -Pwinmd.nugetPackage.")
            add("--nuget-component=$legacyNugetPackage@$nugetVersion")
            nugetSources.forEach { (_, source) -> add("--nuget-source=$source") }
            providers.gradleProperty("winmd.nugetRoot").orNull?.let { add("--nuget-root=$it") }
        }
        contracts.forEach { add("--contract=$it") }
        namespaces.forEach { add("--namespace=$it") }
        sdkVersion?.let { add("--sdk-version=$it") }
        windowsKitsRoot?.let { add("--windows-kits-root=$it") }
        referencesRoot?.let { add("--references-root=$it") }
        require(isNotEmpty()) {
            "Set -Pwinmd.files=<a.winmd,b.winmd>, -Pwinmd.nugetSources=<repo1,repo2> with -Pwinmd.nugetComponents=<id@version,id2@version2>, -Pwinmd.nugetPackage=<id> with -Pwinmd.nugetVersion=<version>, or -Pwinmd.contracts=ContractA,ContractB to choose WinMD inputs."
        }
    }
}

fun Project.runNuGetInstall(
    restoreCommand: String,
    packageId: String,
    packageVersion: String,
    outputDirectory: String,
    sources: List<String>,
) {
    val process = ProcessBuilder(
        buildList {
            add(restoreCommand)
            add("install")
            add(packageId)
            add("-Version")
            add(packageVersion)
            add("-OutputDirectory")
            add(outputDirectory)
            add("-NonInteractive")
            sources.forEach { source ->
                add("-Source")
                add(source)
            }
        },
    )
        .directory(project.projectDir)
        .inheritIO()
        .start()
    val exitCode = process.waitFor()
    require(exitCode == 0) {
        "NuGet restore failed for $packageId@$packageVersion with exit code $exitCode."
    }
}

fun Project.nuGetExecutableCacheDir(): File =
    File(System.getProperty("user.home"), ".gradle/caches/kotlin-winrt/nuget")

fun Project.nuGetExecutableVersionFile(): File =
    nuGetExecutableCacheDir().resolve("nuget.version")

fun Project.nuGetToolRefreshRequested(): Boolean =
    providers.gradleProperty("winmd.nugetToolRefresh").orNull?.equals("true", ignoreCase = true) == true

fun Project.detectNuGetExecutableVersion(nugetExecutable: String): String? {
    val process = ProcessBuilder(nugetExecutable, "help")
        .directory(project.projectDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    process.waitFor()
    return output.lineSequence()
        .map(String::trim)
        .firstOrNull { it.startsWith("NuGet Version:") }
        ?.substringAfter(':')
        ?.trim()
}

fun Project.downloadNuGetExecutable(): String {
    val cacheDir = nuGetExecutableCacheDir()
    cacheDir.mkdirs()
    val exeFile = cacheDir.resolve("nuget.exe")
    if (exeFile.exists() && !nuGetToolRefreshRequested()) {
        return exeFile.absolutePath
    }
    if (exeFile.exists()) {
        exeFile.delete()
    }
    val tempFile = cacheDir.resolve("nuget.exe.download")
    URI("https://dist.nuget.org/win-x86-commandline/latest/nuget.exe").toURL().openStream().use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    check(tempFile.renameTo(exeFile)) {
        "Failed to move downloaded NuGet executable into $exeFile."
    }
    detectNuGetExecutableVersion(exeFile.absolutePath)?.let { version ->
        nuGetExecutableVersionFile().writeText(version)
    }
    return exeFile.absolutePath
}

fun Project.resolveNuGetCommand(): String {
    val configuredCommand = providers.gradleProperty("winmd.nugetCommand").orNull
    if (configuredCommand != null) {
        return configuredCommand
    }
    val cachedExe = nuGetExecutableCacheDir().resolve("nuget.exe")
    if (cachedExe.exists() && !nuGetToolRefreshRequested()) {
        val cachedVersion = nuGetExecutableVersionFile().takeIf { it.exists() }?.readText()?.trim().orEmpty()
        val detectedVersion = detectNuGetExecutableVersion(cachedExe.absolutePath)?.trim().orEmpty()
        if (cachedVersion.isNotEmpty() && detectedVersion.isNotEmpty() && cachedVersion == detectedVersion) {
            return cachedExe.absolutePath
        }
        if (detectedVersion.isNotEmpty()) {
            nuGetExecutableVersionFile().writeText(detectedVersion)
            return cachedExe.absolutePath
        }
    }
    return try {
        val command = "nuget"
        val process = ProcessBuilder(command, "locals", "global-packages", "-list")
            .directory(project.projectDir)
            .redirectErrorStream(true)
            .start()
        process.waitFor()
        command
    } catch (_: Exception) {
        downloadNuGetExecutable()
    }
}

fun Project.latestNuGetPackageVersion(packageId: String, nugetCommand: String): String? {
    val packageRoot = File(discoverNuGetGlobalPackagesRoot(nugetCommand)).resolve(packageId.lowercase())
    if (!packageRoot.exists() || !packageRoot.isDirectory) {
        return null
    }
    return packageRoot.listFiles()
        ?.filter { it.isDirectory }
        ?.maxWithOrNull { left, right -> compareVersions(left.name, right.name) }
        ?.name
}

fun Project.discoverNuGetGlobalPackagesRoot(nugetCommand: String): String {
    val process = ProcessBuilder(
        listOf(nugetCommand, "locals", "global-packages", "-list"),
    )
        .directory(project.projectDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    val exitCode = process.waitFor()
    require(exitCode == 0) {
        "Failed to discover NuGet global-packages path with exit code $exitCode."
    }
    val resolvedPath = output.lineSequence()
        .map(String::trim)
        .firstOrNull { it.startsWith("global-packages:") }
        ?.substringAfter(':')
        ?.trim()
        ?.trim('"')
    return resolvedPath
        ?: System.getenv("NUGET_PACKAGES")
        ?: File(System.getProperty("user.home"), ".nuget/packages").absolutePath
}

val restoreNuGetWinMdPackages by tasks.registering {
    group = "code generation"
    description = "Restores configured NuGet WinRT components into a local package cache."
    notCompatibleWithConfigurationCache("NuGet restore tasks build dynamic process arguments from Gradle properties.")

    val componentSpecs = nuGetComponentSpecs()
    onlyIf { componentSpecs.isNotEmpty() }

    doLast {
        val restoreCommand = resolveNuGetCommand()
        val outputDirectory = providers.gradleProperty("winmd.nugetRoot").orNull
            ?: discoverNuGetGlobalPackagesRoot(restoreCommand)
        val sources = nuGetSourceEntries().map { it.second }
        componentSpecs.forEach { spec ->
            val packageId = spec.substringBefore('@')
            val packageVersion = spec.substringAfter('@')
            project.runNuGetInstall(
                restoreCommand = restoreCommand,
                packageId = packageId,
                packageVersion = packageVersion,
                outputDirectory = outputDirectory,
                sources = sources,
            )
        }
    }
}

val collectNuGetRuntimeAssets by tasks.registering(Sync::class) {
    group = "code generation"
    description = "Collects runtime DLLs from restored NuGet WinRT components."
    dependsOn(restoreNuGetWinMdPackages)
    notCompatibleWithConfigurationCache("NuGet runtime asset collection depends on restored package cache contents.")

    val componentSpecs = nuGetComponentSpecs()
    onlyIf { componentSpecs.isNotEmpty() }

    into(layout.buildDirectory.dir("nuget/runtime-assets"))

    doFirst {
        val restoreCommand = resolveNuGetCommand()
        val resolvedPackagesDir = providers.gradleProperty("winmd.nugetRoot").orNull
            ?.let(::File)
            ?: File(discoverNuGetGlobalPackagesRoot(restoreCommand))
        val roots = componentSpecs.map { spec ->
            val packageId = spec.substringBefore('@')
            val packageVersion = spec.substringAfter('@')
            resolvedPackagesDir.resolve(packageId.lowercase()).resolve(packageVersion)
        }
        from(roots) {
            include("**/*.dll")
        }
        exclude("**/*.winmd")
    }
}

val refreshNuGetTool by tasks.registering {
    group = "code generation"
    description = "Refreshes the cached NuGet executable from the official distribution."

    doLast {
        val tool = downloadNuGetExecutable()
        logger.lifecycle("NuGet executable cached at $tool")
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
        val nugetCommand = resolveNuGetCommand()
        val packageVersion = providers.gradleProperty(versionProperty).orNull
            ?: latestNuGetPackageVersion(packageId, nugetCommand)
            ?: error("Set -P$versionProperty=<version> to choose the NuGet package version.")
        val nugetRoot = providers.gradleProperty(rootProperty).orNull
        val nugetSources = winMdNuGetSourceArgs(packageId)
        val resolvedArgs = mutableListOf(layout.buildDirectory.dir(outputDir).get().asFile.absolutePath)
        resolvedArgs += "--nuget-component=$packageId@$packageVersion"
        resolvedArgs += nugetSources
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
        val nugetCommand = resolveNuGetCommand()
        val packageVersion = providers.gradleProperty(versionProperty).orNull
            ?: latestNuGetPackageVersion(packageId, nugetCommand)
            ?: error("Set -P$versionProperty=<version> to choose the NuGet package version.")
        val nugetRoot = providers.gradleProperty(rootProperty).orNull
        val nugetSources = winMdNuGetSourceArgs(packageId)
        val resolvedArgs = mutableListOf(layout.projectDirectory.dir("src/commonMain/kotlin").asFile.absolutePath)
        resolvedArgs += "--nuget-component=$packageId@$packageVersion"
        resolvedArgs += nugetSources
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
).also {
    it.configure {
        dependsOn(restoreNuGetWinMdPackages)
    }
}

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
).also {
    it.configure {
        dependsOn(restoreNuGetWinMdPackages)
    }
}
