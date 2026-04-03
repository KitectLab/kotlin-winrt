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

val winUiRuntimeAssetsDir = layout.buildDirectory.dir("winui-runtime-assets")

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
        "dev.winrt.bootstrapDll",
        providers.systemProperty("dev.winrt.bootstrapDll").orNull ?: "",
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

    into(winUiRuntimeAssetsDir)

    val packageRoot = latestWindowsAppSdkPackageRoot()
    onlyIf { packageRoot != null }

    doFirst {
        val resolvedPackageRoot = packageRoot
            ?: error("Microsoft.WindowsAppSDK is not restored in the NuGet global packages cache.")
        from(resolvedPackageRoot) {
            include("**/*")
        }
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
