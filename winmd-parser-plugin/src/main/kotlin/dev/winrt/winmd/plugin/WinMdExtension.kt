package dev.winrt.winmd.plugin

open class WinMdExtension {
    var sdkVersion: String? = null
    var windowsKitsRoot: String? = null
    var referencesRoot: String? = null
    var contracts: List<String> = emptyList()
    var winmdFiles: List<String> = emptyList()
    var nugetRoot: String? = null
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
}

data class NuGetComponentReference(
    val packageId: String,
    val packageVersion: String,
    val nugetRoot: String? = null,
)
