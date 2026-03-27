package dev.winrt.winmd.plugin

import java.nio.file.Path

data class ResolvedWinMdConfiguration(
    val sdkVersion: String? = null,
    val referencesRoot: Path? = null,
    val contracts: List<WindowsSdkContractReference> = emptyList(),
    val winmdFiles: List<Path> = emptyList(),
    val nugetPackage: NuGetWinMdPackageReference? = null,
) {
    val sourceFiles: List<Path>
        get() = buildList {
            addAll(winmdFiles)
            addAll(contracts.map { it.winmdPath })
        }.distinct()
}
