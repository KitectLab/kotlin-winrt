package dev.winrt.winmd.plugin

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

data class NuGetWinMdPackageReference(
    val packageId: String,
    val packageVersion: String,
    val packageRoot: Path,
    val winmdFiles: List<Path>,
)

object NuGetPackageReferences {
    private const val nugetPackagesEnv = "NUGET_PACKAGES"

    fun resolvePackage(
        packageId: String,
        packageVersion: String,
        nugetRoot: Path = discoverPackagesRoot(),
    ): NuGetWinMdPackageReference {
        require(nugetRoot.exists() && nugetRoot.isDirectory()) {
            "NuGet packages root does not exist or is not a directory: $nugetRoot"
        }

        val packageRoot = nugetRoot
            .resolve(packageId.lowercase())
            .resolve(packageVersion)
        require(packageRoot.exists() && packageRoot.isDirectory()) {
            "NuGet package directory does not exist: $packageRoot"
        }

        val winmdFiles = Files.walk(packageRoot).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.name.endsWith(".winmd", ignoreCase = true) }
                .sorted()
                .toList()
        }
        require(winmdFiles.isNotEmpty()) {
            "No WinMD files found under NuGet package: $packageRoot"
        }

        return NuGetWinMdPackageReference(
            packageId = packageId,
            packageVersion = packageVersion,
            packageRoot = packageRoot,
            winmdFiles = winmdFiles,
        )
    }

    fun discoverPackagesRoot(): Path {
        return buildList {
            System.getenv(nugetPackagesEnv)?.let { add(Path.of(it)) }
            System.getProperty("user.home")?.let { add(Path.of(it).resolve(".nuget").resolve("packages")) }
            add(Path.of("C:/Users/${System.getProperty("user.name")}/.nuget/packages"))
        }.firstOrNull { it.exists() && it.isDirectory() }
            ?: error("Unable to locate NuGet package cache root. Checked $nugetPackagesEnv and ~/.nuget/packages.")
    }
}
