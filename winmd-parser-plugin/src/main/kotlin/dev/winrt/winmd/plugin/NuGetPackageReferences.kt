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
        val packageRoot = resolvePackageRoot(packageId, packageVersion, listOf(nugetRoot))

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

    fun resolvePackageFromRoots(
        packageId: String,
        packageVersion: String,
        nugetRoots: List<Path>,
    ): NuGetWinMdPackageReference {
        val packageRoot = resolvePackageRoot(packageId, packageVersion, nugetRoots)

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

    private fun resolvePackageRoot(
        packageId: String,
        packageVersion: String,
        nugetRoots: List<Path>,
    ): Path {
        val packageRoot = nugetRoots.asSequence()
            .filter { it.exists() && it.isDirectory() }
            .map { it.resolve(packageId.lowercase()).resolve(packageVersion) }
            .firstOrNull { it.exists() && it.isDirectory() }
            ?: error(
                "NuGet package directory does not exist for $packageId/$packageVersion in any configured source: $nugetRoots",
        )
        return packageRoot
    }
}
