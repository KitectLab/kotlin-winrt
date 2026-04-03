plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

import java.io.File
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory

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

fun normalizeNuGetVersion(value: String): String {
    val trimmed = value.trim()
    return if (
        trimmed.length > 2 &&
        ',' !in trimmed &&
        ((trimmed.startsWith("[") && trimmed.endsWith("]")) ||
            (trimmed.startsWith("(") && trimmed.endsWith(")")))
    ) {
        trimmed.substring(1, trimmed.length - 1)
    } else {
        trimmed
    }
}

val windowsAppSdkVersion = "1.8.260317003"

data class NuGetRuntimePackage(
    val packageId: String,
    val packageVersion: String,
    val packageRoot: File,
)

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
        ?.filter { it.name == windowsAppSdkVersion }
        ?.maxWithOrNull { left, right -> compareVersions(left.name, right.name) }
}

fun resolveNuGetPackageRoot(
    packageId: String,
    packageVersion: String,
    root: File = discoverNuGetGlobalPackagesRoot(),
): File {
    val packageRoot = root.resolve(packageId.lowercase())
    require(packageRoot.exists() && packageRoot.isDirectory) {
        "NuGet package directory does not exist for $packageId in $root."
    }
    return packageRoot.listFiles()
        ?.filter { it.isDirectory && it.name == packageVersion }
        ?.maxWithOrNull { left, right -> compareVersions(left.name, right.name) }
        ?: error("NuGet package directory does not exist for $packageId@$packageVersion in $root.")
}

fun resolveNuGetPackageDependencies(packageRoot: File): List<Pair<String, String>> {
    val nuspec = packageRoot.listFiles()
        ?.firstOrNull { it.isFile && it.name.endsWith(".nuspec", ignoreCase = true) }
        ?: return emptyList()
    val document = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(nuspec)
    val dependencyNodes = document.getElementsByTagName("dependency")
    return buildList {
        for (index in 0 until dependencyNodes.length) {
            val node = dependencyNodes.item(index)
            val element = node as? org.w3c.dom.Element ?: continue
            val id = element.getAttribute("id").takeIf { it.isNotBlank() } ?: continue
            val version = element.getAttribute("version").takeIf { it.isNotBlank() } ?: continue
            add(id to normalizeNuGetVersion(version))
        }
    }
}

fun collectNuGetRuntimePackageClosure(
    packageId: String,
    packageVersion: String,
): List<NuGetRuntimePackage> {
    val queue = ArrayDeque(listOf(packageId to packageVersion))
    val visited = linkedSetOf<String>()
    val result = mutableListOf<NuGetRuntimePackage>()

    while (queue.isNotEmpty()) {
        val (currentId, currentVersion) = queue.removeFirst()
        val currentRoot = resolveNuGetPackageRoot(currentId, currentVersion)
        if (!visited.add(currentRoot.absolutePath)) {
            continue
        }

        result += NuGetRuntimePackage(
            packageId = currentId,
            packageVersion = currentVersion,
            packageRoot = currentRoot,
        )

        resolveNuGetPackageDependencies(currentRoot).forEach { dependency ->
            queue += dependency
        }
    }

    return result
}

fun collectNuGetRuntimeDlls(packageRoot: File, runtimeRid: String): List<File> {
    val topLevelDlls = packageRoot.listFiles()
        ?.filter { it.isFile && it.extension.equals("dll", ignoreCase = true) }
        .orEmpty()
    val runtimeNativeDir = packageRoot.resolve("runtimes").resolve(runtimeRid).resolve("native")
    val runtimeNativeDlls = if (runtimeNativeDir.exists() && runtimeNativeDir.isDirectory) {
        Files.walk(runtimeNativeDir.toPath()).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".dll", ignoreCase = true) }
                .map { it.toFile() }
                .toList()
        }
    } else {
        emptyList()
    }
    return (topLevelDlls + runtimeNativeDlls).distinctBy { it.absolutePath }
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
    val runtimePackagesProvider = providers.provider {
        latestWindowsAppSdkPackageRoot()
            ?: error("Microsoft.WindowsAppSDK is not restored in the NuGet global packages cache.")
        collectNuGetRuntimePackageClosure("Microsoft.WindowsAppSDK", windowsAppSdkVersion)
    }
    from(runtimePackagesProvider.map { runtimePackages ->
        runtimePackages.flatMap { runtimePackage ->
            collectNuGetRuntimeDlls(runtimePackage.packageRoot, runtimeRid)
        }.distinctBy { it.absolutePath }
    }) {
        eachFile { path = name }
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
