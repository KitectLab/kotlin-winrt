package dev.winrt.winmd.plugin

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

    fun latestSdkVersion(referencesRoot: Path): String {
        return referencesRoot.listDirectoryEntries()
            .filter { it.isDirectory() }
            .map { it.name }
            .maxWithOrNull(compareByVersion { it })
            ?: error("No Windows SDK versions found under $referencesRoot")
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
