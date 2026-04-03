package dev.winrt.winmd.plugin

import java.nio.file.Path

object WinMdConfigurationResolver {
    fun resolve(extension: WinMdExtension): ResolvedWinMdConfiguration {
        val explicitWinMdFiles = extension.winmdFiles.map(Path::of)

        val requestedNuGetPackages = buildList {
            addAll(extension.nugetComponents)
            extension.nugetPackageId?.let { packageId ->
                add(
                    NuGetComponentReference(
                        packageId = packageId,
                        packageVersion = extension.nugetPackageVersion
                            ?: error("NuGet package version is required when nugetPackageId is set."),
                        nugetRoot = extension.nugetRoot,
                    ),
                )
            }
        }

        val nugetPackages = requestedNuGetPackages.map { component ->
            NuGetPackageReferences.resolvePackage(
                packageId = component.packageId,
                packageVersion = component.packageVersion,
                nugetRoot = component.nugetRoot?.let(Path::of)
                    ?: extension.nugetRoot?.let(Path::of)
                    ?: NuGetPackageReferences.discoverPackagesRoot(),
            )
        }

        val referencesRoot = resolveReferencesRoot(extension)
        val contractNames = extension.contracts.ifEmpty {
            if (explicitWinMdFiles.isEmpty() && nugetPackages.isEmpty()) {
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
            winmdFiles = explicitWinMdFiles,
            nugetPackages = nugetPackages,
        )
    }

    private fun resolveReferencesRoot(extension: WinMdExtension): Path {
        extension.referencesRoot?.let { return Path.of(it) }
        extension.windowsKitsRoot?.let { return Path.of(it).resolve("References") }
        return WindowsSdkReferences.discoverReferencesRoot()
    }
}
