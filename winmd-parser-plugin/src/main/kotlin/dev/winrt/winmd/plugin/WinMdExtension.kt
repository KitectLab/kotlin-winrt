package dev.winrt.winmd.plugin

open class WinMdExtension {
    var sdkVersion: String? = null
    var windowsKitsRoot: String? = null
    var referencesRoot: String? = null
    var contracts: List<String> = emptyList()
}
