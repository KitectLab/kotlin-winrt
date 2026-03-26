package dev.winrt.winmd.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class WinMdConfigurationResolverTest {
    @Test
    fun prefers_explicit_references_root_and_sdk_version() {
        val extension = WinMdExtension().apply {
            referencesRoot = "D:/Windows Kits/10/References"
            sdkVersion = "10.0.22621.0"
            contracts = listOf(
                "Windows.Foundation.UniversalApiContract",
                "Windows.Foundation.FoundationContract",
            )
        }

        val resolved = WinMdConfigurationResolver.resolve(extension)

        assertEquals("10.0.22621.0", resolved.sdkVersion)
        assertEquals(Path.of("D:/Windows Kits/10/References"), resolved.referencesRoot)
        assertEquals(2, resolved.contracts.size)
        assertEquals(2, resolved.sourceFiles.size)
        assertEquals("Windows.Foundation.UniversalApiContract", resolved.contracts[0].contractName)
        assertEquals("Windows.Foundation.FoundationContract", resolved.contracts[1].contractName)
    }

    @Test
    fun derives_references_root_from_windows_kits_root() {
        val extension = WinMdExtension().apply {
            windowsKitsRoot = "D:/Windows Kits/10"
            sdkVersion = "10.0.22621.0"
        }

        val resolved = WinMdConfigurationResolver.resolve(extension)

        assertEquals(Path.of("D:/Windows Kits/10/References"), resolved.referencesRoot)
        assertEquals("10.0.22621.0", resolved.sdkVersion)
        assertEquals(1, resolved.contracts.size)
        assertEquals(1, resolved.sourceFiles.size)
    }

    @Test
    fun falls_back_to_discovery_when_no_paths_are_configured() {
        val resolved = WinMdConfigurationResolver.resolve(WinMdExtension())

        assertTrue(resolved.referencesRoot.toString().endsWith("References"))
        assertTrue(resolved.contracts.isNotEmpty())
        assertEquals(resolved.contracts.map { it.winmdPath }, resolved.sourceFiles)
    }
}
