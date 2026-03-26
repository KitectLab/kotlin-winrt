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
}
