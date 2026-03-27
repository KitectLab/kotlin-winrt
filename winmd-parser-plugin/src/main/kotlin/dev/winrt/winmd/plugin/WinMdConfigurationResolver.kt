package dev.winrt.winmd.plugin

import java.nio.file.Path

object WinMdConfigurationResolver {
    fun resolve(extension: WinMdExtension): ResolvedWinMdConfiguration {
        val explicitWinMdFiles = extension.winmdFiles.map(Path::of)

        val nugetPackageId = extension.nugetPackageId
        val nugetPackage = if (nugetPackageId != null) {
            NuGetPackageReferences.resolvePackage(
                packageId = nugetPackageId,
                packageVersion = extension.nugetPackageVersion
                    ?: error("NuGet package version is required when nugetPackageId is set."),
                nugetRoot = extension.nugetRoot?.let(Path::of) ?: NuGetPackageReferences.discoverPackagesRoot(),
            )
        } else {
            null
        }

        val resolvedWinMdFiles = buildList {
            addAll(explicitWinMdFiles)
            addAll(nugetPackage?.winmdFiles.orEmpty())
        }

        val referencesRoot = resolveReferencesRoot(extension)
        val contractNames = extension.contracts.ifEmpty {
            if (resolvedWinMdFiles.isEmpty()) {
                listOf("Windows.Foundation.UniversalApiContract")
            } else {
                emptyList()
            }
        }
        val sdkVersion = if (contractNames.isNotEmpty()) {
            extension.sdkVersion ?: WindowsSdkReferences.latestSdkVersion(referencesRoot)
        } else {
            extension.sdkVersion
        }

        val contracts = contractNames.map { contractName ->
            WindowsSdkReferences.findContract(
                referencesRoot = referencesRoot,
                contractName = contractName,
                sdkVersion = sdkVersion ?: error("SDK version is required when resolving contracts."),
            )
        }

        return ResolvedWinMdConfiguration(
            sdkVersion = sdkVersion,
            referencesRoot = referencesRoot,
            contracts = contracts,
            winmdFiles = resolvedWinMdFiles,
            nugetPackage = nugetPackage,
        )
    }

    private fun resolveReferencesRoot(extension: WinMdExtension): Path {
        extension.referencesRoot?.let { return Path.of(it) }
        extension.windowsKitsRoot?.let { return Path.of(it).resolve("References") }
        return WindowsSdkReferences.discoverReferencesRoot()
    }
}
