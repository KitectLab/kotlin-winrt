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

fun currentWindowsRuntimeRid(): String {
    val arch = System.getProperty("os.arch").lowercase()
    return when {
        "aarch64" in arch || "arm64" in arch -> "win-arm64"
        "x86" in arch && "64" !in arch -> "win-x86"
        else -> "win-x64"
    }
}

val winUiRuntimeAssetsDir = layout.buildDirectory.dir("winui-runtime-assets")
val latestWindowsAppSdkPackageRootProvider = providers.provider { latestWindowsAppSdkPackageRoot() }

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

    val runtimeRid = currentWindowsRuntimeRid()
    from(latestWindowsAppSdkPackageRootProvider.map { resolvedPackageRoot ->
        resolvedPackageRoot ?: error("Microsoft.WindowsAppSDK is not restored in the NuGet global packages cache.")
    }) {
        include("*.dll")
    }
    from(latestWindowsAppSdkPackageRootProvider.map { resolvedPackageRoot ->
        val packageRoot = resolvedPackageRoot
            ?: error("Microsoft.WindowsAppSDK is not restored in the NuGet global packages cache.")
        val runtimeNativeDir = packageRoot.resolve("runtimes").resolve(runtimeRid).resolve("native")
        if (!runtimeNativeDir.exists()) {
            error("Microsoft.WindowsAppSDK does not contain a $runtimeRid runtime directory.")
        }
        runtimeNativeDir
    }) {
        include("**/*.dll")
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
