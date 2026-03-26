package dev.winrt.sample.jvm

import dev.winrt.kom.PlatformRuntime

object WindowsAppSdkEnvironment {
    data class PackageState(
        val frameworkInstalled: Boolean,
        val mainInstalled: Boolean,
        val singletonInstalled: Boolean,
    ) {
        fun summary(): String {
            return "packages=framework:$frameworkInstalled,main:$mainInstalled,singleton:$singletonInstalled"
        }
    }

    fun detect(): PackageState? {
        if (!PlatformRuntime.isWindows) {
            return null
        }

        return runCatching {
            val command = listOf(
                "powershell.exe",
                "-NoLogo",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                buildScript(),
            )
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                return null
            }
            parse(output)
        }.getOrNull()
    }

    private fun buildScript(): String {
        return """
            ${'$'}framework = @(Get-AppxPackage -Name 'Microsoft.WindowsAppRuntime.1.6')
            ${'$'}main = @(Get-AppxPackage -Name 'MicrosoftCorporationII.WinAppRuntime.Main.1.6*')
            ${'$'}singleton = @(Get-AppxPackage -Name 'MicrosoftCorporationII.WinAppRuntime.Singleton')
            Write-Output ('framework=' + ${'$'}framework.Count)
            Write-Output ('main=' + ${'$'}main.Count)
            Write-Output ('singleton=' + ${'$'}singleton.Count)
        """.trimIndent()
    }

    internal fun parse(output: String): PackageState {
        val values = output.lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) {
                    null
                } else {
                    line.substring(0, separator).trim() to line.substring(separator + 1).trim()
                }
            }
            .toMap()

        return PackageState(
            frameworkInstalled = values["framework"]?.toIntOrNull()?.let { it > 0 } == true,
            mainInstalled = values["main"]?.toIntOrNull()?.let { it > 0 } == true,
            singletonInstalled = values["singleton"]?.toIntOrNull()?.let { it > 0 } == true,
        )
    }
}
