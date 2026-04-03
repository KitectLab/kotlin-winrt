package dev.winrt.winmd.plugin

open class WinMdExtension {
    companion object {
        const val OFFICIAL_NUGET_SOURCE = "https://api.nuget.org/v3/index.json"
    }

    var sdkVersion: String? = null
    var windowsKitsRoot: String? = null
    var referencesRoot: String? = null
    var contracts: List<String> = emptyList()
    var winmdFiles: List<String> = emptyList()
    var nugetRoot: String? = null
    var nugetSources: List<String> = emptyList()
    var nugetPackageId: String? = null
    var nugetPackageVersion: String? = null
    private val _nugetComponents = mutableListOf<NuGetComponentReference>()

    val nugetComponents: List<NuGetComponentReference>
        get() = _nugetComponents

    fun nugetComponent(
        packageId: String,
        packageVersion: String,
        nugetRoot: String? = this.nugetRoot,
    ) {
        _nugetComponents += NuGetComponentReference(
            packageId = packageId,
            packageVersion = packageVersion,
            nugetRoot = nugetRoot,
        )
    }

    fun official() {
        nugetSources = (nugetSources + OFFICIAL_NUGET_SOURCE).distinct()
    }
}

data class NuGetComponentReference(
    val packageId: String,
    val packageVersion: String,
    val nugetRoot: String? = null,
    val nugetSource: String? = null,
)
