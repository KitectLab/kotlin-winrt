package dev.winrt.sample.jvm

import dev.winrt.kom.PlatformRuntime
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class WindowsJsonParseSmokeTest {
    @Test
    fun windows_data_json_can_parse_json_object() {
        assumeTrue(PlatformRuntime.isWindows)

        val script = """
            ${'$'}json = '{"name":"codex","kind":"winrt"}'
            ${'$'}object = [Windows.Data.Json.JsonObject, Windows.Data.Json, ContentType=WindowsRuntime]::Parse(${'$'}json)
            ${'$'}object.GetNamedString('name')
        """.trimIndent()
        val encodedCommand = Base64.getEncoder()
            .encodeToString(script.toByteArray(StandardCharsets.UTF_16LE))

        val process = ProcessBuilder(
            "powershell.exe",
            "-NoLogo",
            "-NoProfile",
            "-NonInteractive",
            "-EncodedCommand",
            encodedCommand,
        ).start()

        val stdout = process.inputStream.bufferedReader().readText().trim()
        val stderr = process.errorStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()

        assertEquals("PowerShell WinRT JSON parse should succeed: $stderr", 0, exitCode)
        assertTrue("Expected parsed JSON value in stdout, got: '$stdout'", stdout.isNotBlank())
        assertEquals("codex", stdout)
    }
}
