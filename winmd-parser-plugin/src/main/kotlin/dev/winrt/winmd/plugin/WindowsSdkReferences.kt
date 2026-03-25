package dev.winrt.winmd.plugin

import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

data class WindowsSdkContractReference(
    val sdkVersion: String,
    val contractName: String,
    val contractVersion: String,
    val winmdPath: Path,
)

object WindowsSdkReferences {
    private const val windowsKitsEnv = "WINDOWS_KITS_10_DIR"
    private const val registryKey = """HKLM\SOFTWARE\Microsoft\Windows Kits\Installed Roots"""
    private const val registryValue = "KitsRoot10"

    fun findContract(
        referencesRoot: Path,
        contractName: String,
        sdkVersion: String? = null,
    ): WindowsSdkContractReference {
        require(referencesRoot.exists() && referencesRoot.isDirectory()) {
            "Windows SDK references root does not exist or is not a directory: $referencesRoot"
        }

        val resolvedSdkVersion = sdkVersion ?: latestSdkVersion(referencesRoot)
        val sdkRoot = referencesRoot.resolve(resolvedSdkVersion)
        require(sdkRoot.exists() && sdkRoot.isDirectory()) {
            "Windows SDK version directory does not exist: $sdkRoot"
        }

        val contractRoot = sdkRoot.resolve(contractName)
        require(contractRoot.exists() && contractRoot.isDirectory()) {
            "Windows SDK contract directory does not exist: $contractRoot"
        }

        val contractVersion = contractRoot.listDirectoryEntries()
            .filter { it.isDirectory() }
            .maxWithOrNull(compareByVersion { it.name })
            ?.name
            ?: error("No contract versions found under $contractRoot")

        val contractVersionRoot = contractRoot.resolve(contractVersion)
        val winmdPath = contractVersionRoot.listDirectoryEntries()
            .firstOrNull { entry ->
                Files.isRegularFile(entry) && entry.name.equals("$contractName.winmd", ignoreCase = true)
            }
            ?: error("No WinMD file found for $contractName under $contractVersionRoot")

        return WindowsSdkContractReference(
            sdkVersion = resolvedSdkVersion,
            contractName = contractName,
            contractVersion = contractVersion,
            winmdPath = winmdPath,
        )
    }

    fun findContract(
        contractName: String,
        sdkVersion: String? = null,
    ): WindowsSdkContractReference {
        val referencesRoot = discoverReferencesRoot()
        return findContract(
            referencesRoot = referencesRoot,
            contractName = contractName,
            sdkVersion = sdkVersion,
        )
    }

    fun latestSdkVersion(referencesRoot: Path): String {
        return referencesRoot.listDirectoryEntries()
            .filter { it.isDirectory() }
            .map { it.name }
            .maxWithOrNull(compareByVersion { it })
            ?: error("No Windows SDK versions found under $referencesRoot")
    }

    fun discoverReferencesRoot(): Path {
        return discoverReferencesRootCandidates().firstOrNull()
            ?: error(
                "Unable to locate Windows SDK references root. " +
                    "Checked $windowsKitsEnv, registry $registryKey/$registryValue, and common install paths."
            )
    }

    fun discoverReferencesRootCandidates(): List<Path> {
        return buildList {
            addIfReferencesRoot(System.getenv(windowsKitsEnv)?.let(Path::of))
            addIfReferencesRoot(readRegistryKitsRoot()?.let(Path::of))
            addIfReferencesRoot(Path.of("D:/Windows Kits/10"))
            addIfReferencesRoot(Path.of("C:/Program Files (x86)/Windows Kits/10"))
        }.distinct()
    }

    internal fun readRegistryKitsRoot(): String? {
        return runCatching {
            val process = ProcessBuilder(
                "reg",
                "query",
                registryKey,
                "/v",
                registryValue,
            ).redirectErrorStream(true).start()
            process.inputStream.bufferedReader().use(BufferedReader::readText)
                .lineSequence()
                .map(String::trim)
                .firstNotNullOfOrNull(::parseRegistryValueLine)
                ?.also { process.waitFor() }
        }.getOrNull()
    }

    private fun MutableList<Path>.addIfReferencesRoot(kitsRoot: Path?) {
        if (kitsRoot == null) {
            return
        }

        val referencesRoot = if (kitsRoot.fileName?.toString()?.equals("References", ignoreCase = true) == true) {
            kitsRoot
        } else {
            kitsRoot.resolve("References")
        }

        if (referencesRoot.exists() && referencesRoot.isDirectory()) {
            add(referencesRoot)
        }
    }

    private fun parseRegistryValueLine(line: String): String? {
        if (!line.contains(registryValue)) {
            return null
        }

        val tokens = line.split(Regex("\\s+"))
        if (tokens.size < 3 || tokens[0] != registryValue) {
            return null
        }

        return tokens.drop(2).joinToString(" ").trim().takeIf(String::isNotEmpty)
    }

    private fun versionKey(value: String): List<Int> {
        return value.split('.').map { token -> token.toIntOrNull() ?: 0 }
    }

    private fun <T> compareByVersion(selector: (T) -> String): Comparator<T> {
        return Comparator { left, right ->
            val leftParts = versionKey(selector(left))
            val rightParts = versionKey(selector(right))
            val maxSize = maxOf(leftParts.size, rightParts.size)
            for (index in 0 until maxSize) {
                val leftValue = leftParts.getOrElse(index) { 0 }
                val rightValue = rightParts.getOrElse(index) { 0 }
                if (leftValue != rightValue) {
                    return@Comparator leftValue.compareTo(rightValue)
                }
            }
            0
        }
    }
}
