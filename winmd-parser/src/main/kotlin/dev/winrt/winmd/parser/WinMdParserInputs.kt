package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdConfigurationResolver
import dev.winrt.winmd.plugin.WinMdExtension
import java.nio.file.Path

data class WinMdParserInputs(
    val outputDir: Path,
    val sources: List<Path>,
    val namespaceFilters: List<String> = emptyList(),
)

object WinMdParserInputResolver {
    fun resolve(args: Array<String>): WinMdParserInputs {
        require(args.size >= 2) {
            "Usage: winmd-parser <outputDir> <winmdFile> [<winmdFile> ...] | " +
                "winmd-parser <outputDir> --contract=<name> [--contract=<name> ...] " +
                "[--sdk-version=<version>] [--windows-kits-root=<path>] [--references-root=<path>] " +
                "[--namespace=<prefix> ...]"
        }

        val outputDir = Path.of(args.first())
        val remainingArgs = args.drop(1)
        val optionArgs = remainingArgs.filter { it.startsWith("--") }
        val positionalArgs = remainingArgs.filterNot { it.startsWith("--") }

        val nonNamespaceOptions = optionArgs.filterNot { it.startsWith("--namespace=") }
        if (nonNamespaceOptions.isNotEmpty() && positionalArgs.isNotEmpty()) {
            error("Do not mix WinMD file paths with --contract/--sdk-version style options.")
        }

        val sources = if (nonNamespaceOptions.isNotEmpty()) {
            resolveConfiguredSources(nonNamespaceOptions)
        } else {
            positionalArgs.map(Path::of)
        }

        require(sources.isNotEmpty()) {
            "No WinMD sources were resolved."
        }

        val namespaceFilters = optionArgs
            .filter { it.startsWith("--namespace=") }
            .map { it.substringAfter('=') }

        return WinMdParserInputs(
            outputDir = outputDir,
            sources = sources,
            namespaceFilters = namespaceFilters,
        )
    }

    private fun resolveConfiguredSources(args: List<String>): List<Path> {
        val extension = WinMdExtension()

        args.forEach { arg ->
            when {
                arg.startsWith("--contract=") -> {
                    extension.contracts = extension.contracts + arg.substringAfter('=')
                }
                arg.startsWith("--sdk-version=") -> {
                    extension.sdkVersion = arg.substringAfter('=')
                }
                arg.startsWith("--windows-kits-root=") -> {
                    extension.windowsKitsRoot = arg.substringAfter('=')
                }
                arg.startsWith("--references-root=") -> {
                    extension.referencesRoot = arg.substringAfter('=')
                }
                arg.startsWith("--namespace=") -> Unit
                else -> error("Unknown option: $arg")
            }
        }

        val resolved = WinMdConfigurationResolver.resolve(extension)
        return resolved.contracts.map { it.winmdPath }
    }
}
