package dev.winrt.winmd.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class WinMdParserInputResolverTest {
    @Test
    fun resolves_explicit_winmd_file_inputs() {
        val source = Files.createTempFile("sample", ".winmd")
        Files.write(source, byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()))

        val inputs = WinMdParserInputResolver.resolve(
            arrayOf("build/generated", source.toString()),
        )

        assertEquals(Path.of("build/generated"), inputs.outputDir)
        assertEquals(listOf(source), inputs.sources)
    }

    @Test
    fun resolves_sources_from_contract_configuration() {
        val inputs = WinMdParserInputResolver.resolve(
            arrayOf(
                "build/generated",
                "--windows-kits-root=D:/Windows Kits/10",
                "--sdk-version=10.0.22621.0",
                "--contract=Windows.Foundation.UniversalApiContract",
                "--contract=Windows.Foundation.FoundationContract",
            ),
        )

        assertEquals(Path.of("build/generated"), inputs.outputDir)
        assertEquals(2, inputs.sources.size)
        assertTrue(inputs.sources.any { it.toString().contains("Windows.Foundation.UniversalApiContract.winmd") })
        assertTrue(inputs.sources.any { it.toString().contains("Windows.Foundation.FoundationContract.winmd") })
    }
}
