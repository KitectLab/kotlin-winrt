package dev.winrt.winmd.plugin

import java.nio.file.Path

object WinMdConfigurationResolver {
    fun resolve(extension: WinMdExtension): ResolvedWinMdConfiguration {
        if (extension.winmdFiles.isNotEmpty()) {
            return ResolvedWinMdConfiguration(
                winmdFiles = extension.winmdFiles.map(Path::of),
            )
        }

        val nugetPackageId = extension.nugetPackageId
        if (nugetPackageId != null) {
            val nugetPackage = NuGetPackageReferences.resolvePackage(
                packageId = nugetPackageId,
                packageVersion = extension.nugetPackageVersion
                    ?: error("NuGet package version is required when nugetPackageId is set."),
                nugetRoot = extension.nugetRoot?.let(Path::of) ?: NuGetPackageReferences.discoverPackagesRoot(),
            )
            return ResolvedWinMdConfiguration(
                winmdFiles = nugetPackage.winmdFiles,
                nugetPackage = nugetPackage,
            )
        }

        val referencesRoot = resolveReferencesRoot(extension)
        val sdkVersion = extension.sdkVersion ?: WindowsSdkReferences.latestSdkVersion(referencesRoot)
        val contractNames = extension.contracts.ifEmpty {
            listOf(
                "Windows.Foundation.UniversalApiContract",
            )
        }

        val contracts = contractNames.map { contractName ->
            WindowsSdkReferences.findContract(
                referencesRoot = referencesRoot,
                contractName = contractName,
                sdkVersion = sdkVersion,
            )
        }

        return ResolvedWinMdConfiguration(
            sdkVersion = sdkVersion,
            referencesRoot = referencesRoot,
            contracts = contracts,
        )
    }

    private fun resolveReferencesRoot(extension: WinMdExtension): Path {
        extension.referencesRoot?.let { return Path.of(it) }
        extension.windowsKitsRoot?.let { return Path.of(it).resolve("References") }
        return WindowsSdkReferences.discoverReferencesRoot()
    }
}
