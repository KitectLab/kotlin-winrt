package dev.winrt.winmd.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class WindowsSdkReferencesTest {
    private val referencesRoot = Path.of("D:/Windows Kits/10/References")

    @Test
    fun resolves_latest_windows_foundation_universal_api_contract() {
        val reference = WindowsSdkReferences.findContract(
            referencesRoot = referencesRoot,
            contractName = "Windows.Foundation.UniversalApiContract",
        )

        assertEquals("10.0.22621.0", reference.sdkVersion)
        assertEquals("Windows.Foundation.UniversalApiContract", reference.contractName)
        assertEquals("15.0.0.0", reference.contractVersion)
        assertTrue(reference.winmdPath.exists())
        assertTrue(reference.winmdPath.fileName.toString().equals("Windows.Foundation.UniversalApiContract.winmd", ignoreCase = true))
    }

    @Test
    fun resolves_specific_foundation_contract_version() {
        val reference = WindowsSdkReferences.findContract(
            referencesRoot = referencesRoot,
            contractName = "Windows.Foundation.FoundationContract",
            sdkVersion = "10.0.22621.0",
        )

        assertEquals("10.0.22621.0", reference.sdkVersion)
        assertEquals("4.0.0.0", reference.contractVersion)
        assertTrue(reference.winmdPath.exists())
        assertTrue(reference.winmdPath.toString().contains("Windows.Foundation.FoundationContract"))
    }

    @Test
    fun uses_latest_installed_sdk_version_and_reports_missing_versions() {
        val tempRoot = Files.createTempDirectory("windows-kits")
        Files.createDirectories(tempRoot.resolve("10.0.19041.0"))
        Files.createDirectories(tempRoot.resolve("10.0.22621.0"))
        Files.createDirectories(
            tempRoot.resolve("10.0.22621.0")
                .resolve("Windows.Foundation.UniversalApiContract")
                .resolve("15.0.0.0"),
        )
        Files.write(
            tempRoot.resolve("10.0.22621.0")
                .resolve("Windows.Foundation.UniversalApiContract")
                .resolve("15.0.0.0")
                .resolve("Windows.Foundation.UniversalApiContract.winmd"),
            byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()),
        )

        val latest = WindowsSdkReferences.findContract(
            referencesRoot = tempRoot,
            contractName = "Windows.Foundation.UniversalApiContract",
        )

        assertEquals("10.0.22621.0", latest.sdkVersion)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            WindowsSdkReferences.findContract(
                referencesRoot = tempRoot,
                contractName = "Windows.Foundation.UniversalApiContract",
                sdkVersion = "10.0.20348.0",
            )
        }

        assertTrue(exception.message?.contains("Requested version: 10.0.20348.0") == true)
        assertTrue(exception.message?.contains("Installed versions: 10.0.19041.0, 10.0.22621.0") == true)
        assertTrue(exception.message?.contains("Install a Windows 10 SDK") == true)
    }

    @Test
    fun discovers_references_root_from_registry_or_fallbacks() {
        val referencesRoot = WindowsSdkReferences.discoverReferencesRoot()

        assertTrue(referencesRoot.exists())
        assertTrue(referencesRoot.toString().endsWith("References"))
    }

    @Test
    fun registry_query_returns_plain_kits_root_when_available() {
        val kitsRoot = WindowsSdkReferences.readRegistryKitsRoot()

        if (kitsRoot != null) {
            assertFalse(kitsRoot.endsWith("References"))
            assertTrue(kitsRoot.contains("Windows Kits"))
        }
    }
}
