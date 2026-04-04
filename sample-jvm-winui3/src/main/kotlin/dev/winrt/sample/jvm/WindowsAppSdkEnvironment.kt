package dev.winrt.sample.jvm

import dev.winrt.kom.PlatformRuntime

object WindowsAppSdkEnvironment {
    private const val defaultReleaseMajorMinor = "1.8"

    enum class Readiness {
        Ready,
        MissingFramework,
        MissingMain,
        MissingDdlm,
        MissingSingleton,
        Unknown,
    }

    data class PackageState(
        val frameworkInstalled: Boolean,
        val mainInstalled: Boolean,
        val ddlmInstalled: Boolean,
        val singletonInstalled: Boolean,
    ) {
        fun summary(): String {
            return "packages=framework:$frameworkInstalled,main:$mainInstalled,ddlm:$ddlmInstalled,singleton:$singletonInstalled"
        }

        fun readiness(): Readiness {
            return when {
                !frameworkInstalled -> Readiness.MissingFramework
                !mainInstalled -> Readiness.MissingMain
                !ddlmInstalled -> Readiness.MissingDdlm
                !singletonInstalled -> Readiness.MissingSingleton
                else -> Readiness.Ready
            }
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
        val versionInfo = WindowsAppSdkBootstrap.discoverConfiguredVersionInfo()
        val releaseMajorMinor = versionInfo?.releaseMajorMinorString ?: defaultReleaseMajorMinor
        val ddlmFamilyName = when {
            isArm64Architecture() -> versionInfo?.ddlmPackageFamilyNameArm64
            isX86Architecture() -> versionInfo?.ddlmPackageFamilyNameX86
            else -> versionInfo?.ddlmPackageFamilyNameX64
        }
        val frameworkSelector = packageSelector(
            packageFamilyName = versionInfo?.frameworkPackageFamilyName,
            fallbackNamePattern = "Microsoft.WindowsAppRuntime.$releaseMajorMinor*",
        )
        val mainSelector = packageSelector(
            packageFamilyName = versionInfo?.mainPackageFamilyName,
            fallbackNamePattern = "MicrosoftCorporationII.WinAppRuntime.Main.$releaseMajorMinor*",
        )
        val singletonSelector = packageSelector(
            packageFamilyName = versionInfo?.singletonPackageFamilyName,
            fallbackNamePattern = "MicrosoftCorporationII.WinAppRuntime.Singleton*",
        )
        val ddlmSelector = ddlmFamilyName
            ?.let { packageFamilySelector(it) }
            ?: "(\$_.Name -like 'Microsoft.WinAppRuntime.DDLM*' -or \$_.Name -like 'Microsoft.WindowsAppRuntime.DDLM*')"

        return """
            ${'$'}framework = @(Get-AppxPackage | Where-Object { $frameworkSelector })
            ${'$'}main = @(Get-AppxPackage | Where-Object { $mainSelector })
            ${'$'}ddlm = @(Get-AppxPackage | Where-Object { $ddlmSelector })
            ${'$'}singleton = @(Get-AppxPackage | Where-Object { $singletonSelector })
            Write-Output ('framework=' + ${'$'}framework.Count)
            Write-Output ('main=' + ${'$'}main.Count)
            Write-Output ('ddlm=' + ${'$'}ddlm.Count)
            Write-Output ('singleton=' + ${'$'}singleton.Count)
        """.trimIndent()
    }

    private fun packageSelector(packageFamilyName: String?, fallbackNamePattern: String): String {
        return packageFamilyName
            ?.let(::packageFamilySelector)
            ?: "${'$'}_.Name -like '${escapePowerShellLiteral(fallbackNamePattern)}'"
    }

    private fun packageFamilySelector(packageFamilyName: String): String {
        return "${'$'}_.PackageFamilyName -eq '${escapePowerShellLiteral(packageFamilyName)}'"
    }

    private fun escapePowerShellLiteral(value: String): String =
        value.replace("'", "''")

    private fun isArm64Architecture(): Boolean =
        System.getProperty("os.arch").lowercase().contains("arm64") ||
            System.getProperty("os.arch").lowercase().contains("aarch64")

    private fun isX86Architecture(): Boolean {
        val arch = System.getProperty("os.arch").lowercase()
        return arch.contains("x86") && !arch.contains("64")
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
            ddlmInstalled = values["ddlm"]?.toIntOrNull()?.let { it > 0 } == true,
            singletonInstalled = values["singleton"]?.toIntOrNull()?.let { it > 0 } == true,
        )
    }
}
