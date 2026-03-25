package dev.winrt.winmd.plugin

import java.nio.file.Path

object WinMdConfigurationResolver {
    fun resolve(extension: WinMdExtension): ResolvedWinMdConfiguration {
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
