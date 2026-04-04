package dev.winrt.winmd.plugin

import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import org.w3c.dom.Element

data class NuGetWinMdPackageReference(
    val packageId: String,
    val packageVersion: String,
    val packageRoot: Path,
    val winmdFiles: List<Path>,
    val runtimeDllFiles: List<Path>,
)

object NuGetPackageReferences {
    private const val nugetPackagesEnv = "NUGET_PACKAGES"

    fun resolvePackage(
        packageId: String,
        packageVersion: String,
        nugetRoot: Path = discoverPackagesRoot(),
    ): NuGetWinMdPackageReference = resolvePackageFromRoots(packageId, packageVersion, listOf(nugetRoot))

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
        val packageClosure = collectPackageClosure(packageId, packageVersion, nugetRoots)

        val winmdFiles = collectFiles(packageClosure, ".winmd")
        val runtimeDllFiles = collectFiles(packageClosure, ".dll")
        require(winmdFiles.isNotEmpty()) {
            "No WinMD files found under NuGet package or its dependencies: $packageRoot"
        }

        return NuGetWinMdPackageReference(
            packageId = packageId,
            packageVersion = packageVersion,
            packageRoot = packageRoot,
            winmdFiles = winmdFiles,
            runtimeDllFiles = runtimeDllFiles,
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

    private fun collectPackageClosure(
        packageId: String,
        packageVersion: String,
        nugetRoots: List<Path>,
    ): List<Path> {
        val queue = ArrayDeque(listOf(packageId to packageVersion))
        val visited = linkedSetOf<Path>()
        val resolvedRoots = mutableListOf<Path>()

        while (queue.isNotEmpty()) {
            val (currentId, currentVersion) = queue.removeFirst()
            val currentRoot = resolvePackageRoot(currentId, currentVersion, nugetRoots)
            if (!visited.add(currentRoot.normalize())) {
                continue
            }

            resolvedRoots.add(currentRoot)
            resolvePackageDependencies(currentRoot).forEach { dependency ->
                queue.addLast(dependency)
            }
        }

        return resolvedRoots
    }

    private fun collectFiles(packageRoots: List<Path>, extension: String): List<Path> {
        return packageRoots
            .flatMap { packageRoot ->
                Files.walk(packageRoot).use { paths ->
                    paths
                        .filter { Files.isRegularFile(it) && it.name.endsWith(extension, ignoreCase = true) }
                        .sorted()
                        .toList()
                }
            }
            .distinct()
    }

    private fun resolvePackageDependencies(packageRoot: Path): List<Pair<String, String>> {
        val nuspec = Files.list(packageRoot).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.name.endsWith(".nuspec", ignoreCase = true) }
                .findFirst()
                .orElse(null)
        } ?: return emptyList()

        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(nuspec.toFile())
        val dependenciesNodes = document.getElementsByTagName("dependencies")
        if (dependenciesNodes.length == 0) {
            return emptyList()
        }
        val dependenciesElement = dependenciesNodes.item(0) as? Element ?: return emptyList()
        return buildList {
            for (index in 0 until dependenciesElement.childNodes.length) {
                val node = dependenciesElement.childNodes.item(index) as? Element ?: continue
                when (node.tagName) {
                    "dependency" -> parseDependency(node)?.let(::add)
                    "group" -> {
                        val targetFramework = node.getAttribute("targetFramework").trim()
                        if (targetFramework.isNotEmpty() && !targetFramework.startsWith("native", ignoreCase = true)) {
                            continue
                        }
                        for (dependencyIndex in 0 until node.childNodes.length) {
                            val dependency = node.childNodes.item(dependencyIndex) as? Element ?: continue
                            if (dependency.tagName != "dependency") continue
                            parseDependency(dependency)?.let(::add)
                        }
                    }
                }
            }
        }
    }

    private fun parseDependency(node: Element): Pair<String, String>? {
        val id = node.getAttribute("id").takeIf { it.isNotBlank() } ?: return null
        val version = node.getAttribute("version").takeIf { it.isNotBlank() } ?: return null
        return id to normalizeVersion(version)
    }

    private fun normalizeVersion(value: String): String {
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
}
