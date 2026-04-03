plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

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

fun discoverNuGetGlobalPackagesRoot(): File {
    return System.getenv("NUGET_PACKAGES")
        ?.takeIf { it.isNotBlank() }
        ?.let(::File)
        ?: File(System.getProperty("user.home"), ".nuget/packages")
}

fun latestWindowsAppSdkPackageRoot(): File? {
    val packageRoot = discoverNuGetGlobalPackagesRoot().resolve("microsoft.windowsappsdk")
    if (!packageRoot.exists() || !packageRoot.isDirectory) {
        return null
    }
    return packageRoot.listFiles()
        ?.filter { it.isDirectory }
        ?.maxWithOrNull { left, right -> compareVersions(left.name, right.name) }
}

fun findBootstrapDll(packageRoot: File): File? {
    val archPreferences = buildList {
        when {
            System.getProperty("os.arch").contains("aarch64", ignoreCase = true) ||
                System.getProperty("os.arch").contains("arm64", ignoreCase = true) -> {
                add("win-arm64")
                add("win-x64")
                add("win-x86")
            }
            else -> {
                add("win-x64")
                add("win-x86")
                add("win-arm64")
            }
        }
    }
    val candidates = buildList {
        add(packageRoot.resolve("Microsoft.WindowsAppRuntime.Bootstrap.dll"))
        archPreferences.forEach { arch ->
            add(packageRoot.resolve("runtimes").resolve(arch).resolve("native").resolve("Microsoft.WindowsAppRuntime.Bootstrap.dll"))
        }
        packageRoot.walkTopDown().forEach { file ->
            if (file.isFile && file.name.equals("Microsoft.WindowsAppRuntime.Bootstrap.dll", ignoreCase = true)) {
                add(file)
            }
        }
    }
    return candidates.firstOrNull { it.isFile }
}

val winUiRuntimeAssetsDir = layout.buildDirectory.dir("winui-runtime-assets")
val latestWindowsAppSdkPackageRootProvider = providers.provider { latestWindowsAppSdkPackageRoot() }
val latestWindowsAppSdkBootstrapDllProvider = latestWindowsAppSdkPackageRootProvider.map { packageRoot ->
    packageRoot?.let(::findBootstrapDll)
}

kotlin {
    jvmToolchain(22)
}

tasks.withType<Test>().configureEach {
    dependsOn(collectWinUiRuntimeAssets)
    jvmArgs("-Xms128m", "-Xmx768m")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    providers.systemProperty("dev.winrt.errorFile").orNull?.let { errorFile ->
        jvmArgs("-XX:ErrorFile=$errorFile")
    }
    systemProperty(
        "dev.winrt.bootstrapDll",
        providers.systemProperty("dev.winrt.bootstrapDll").orNull
            ?: winUiRuntimeAssetsDir.get().asFile.resolve("Microsoft.WindowsAppRuntime.Bootstrap.dll").absolutePath,
    )
    systemProperty(
        "dev.winrt.windowsAppSdkRoot",
        winUiRuntimeAssetsDir.get().asFile.absolutePath,
    )
}

tasks.withType<JavaExec>().configureEach {
    dependsOn(collectWinUiRuntimeAssets)
    jvmArgs("-Xms128m", "-Xmx768m")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    providers.systemProperty("dev.winrt.errorFile").orNull?.let { errorFile ->
        jvmArgs("-XX:ErrorFile=$errorFile")
    }
    systemProperty(
        "dev.winrt.bootstrapDll",
        providers.systemProperty("dev.winrt.bootstrapDll").orNull
            ?: winUiRuntimeAssetsDir.get().asFile.resolve("Microsoft.WindowsAppRuntime.Bootstrap.dll").absolutePath,
    )
    systemProperty(
        "dev.winrt.windowsAppSdkRoot",
        providers.systemProperty("dev.winrt.windowsAppSdkRoot").orNull
            ?: winUiRuntimeAssetsDir.get().asFile.absolutePath,
    )
    systemProperty(
        "dev.winrt.autoQuitVisible",
        providers.systemProperty("dev.winrt.autoQuitVisible").orNull ?: "",
    )
}

val collectWinUiRuntimeAssets by tasks.registering(Sync::class) {
    group = "code generation"
    description = "Collects the latest restored Microsoft.WindowsAppSDK runtime package into a local staging directory."
    notCompatibleWithConfigurationCache("WinUI runtime asset staging depends on resolved package cache contents.")
    dependsOn(project(":generated-winrt-bindings").tasks.named("restoreNuGetWinMdPackages"))

    into(winUiRuntimeAssetsDir)

    from(latestWindowsAppSdkPackageRootProvider.map { resolvedPackageRoot ->
        resolvedPackageRoot
            ?: error("Microsoft.WindowsAppSDK is not restored in the NuGet global packages cache.")
    })
    from(latestWindowsAppSdkBootstrapDllProvider.map { resolvedBootstrapDll ->
        resolvedBootstrapDll
            ?: error("Microsoft.WindowsAppRuntime.Bootstrap.dll is not restored in the NuGet global packages cache.")
    }) {
        rename { "Microsoft.WindowsAppRuntime.Bootstrap.dll" }
    }
}

dependencies {
    implementation(projects.generatedWinrtBindings)
    implementation(projects.winrtCore)
    implementation(projects.kom)
    testImplementation(libs.junit)
}

application {
    mainClass = "dev.winrt.sample.jvm.MainKt"
    applicationDefaultJvmArgs = listOf(
        "-Ddev.winrt.bootstrapDll=%APP_HOME%/winui-runtime-assets/Microsoft.WindowsAppRuntime.Bootstrap.dll",
        "-Ddev.winrt.windowsAppSdkRoot=%APP_HOME%/winui-runtime-assets",
    )
}

distributions {
    main {
        contents {
            into("winui-runtime-assets") {
                from(winUiRuntimeAssetsDir)
            }
        }
    }
}

tasks.named("installDist") {
    dependsOn(collectWinUiRuntimeAssets)
}

tasks.named("distZip") {
    dependsOn(collectWinUiRuntimeAssets)
}

tasks.named("distTar") {
    dependsOn(collectWinUiRuntimeAssets)
}
