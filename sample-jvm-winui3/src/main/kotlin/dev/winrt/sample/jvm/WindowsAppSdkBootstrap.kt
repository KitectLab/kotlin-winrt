package dev.winrt.sample.jvm

import dev.winrt.kom.HResult
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.nio.file.Files
import java.nio.file.Path
import java.nio.charset.StandardCharsets

object WindowsAppSdkBootstrap {
    private const val defaultMajorMinorVersion = 0x00010008
    private const val tag = ""
    private const val defaultMinVersion = 0x1F40032608CC0000L
    private const val versionInfoHeaderRelativePath = "include/WindowsAppSDK-VersionInfo.h"
    private val releaseMajorRegex = Regex("""#define\s+WINDOWSAPPSDK_RELEASE_MAJOR\s+(\d+)""")
    private val releaseMinorRegex = Regex("""#define\s+WINDOWSAPPSDK_RELEASE_MINOR\s+(\d+)""")
    private val releaseMajorMinorRegex = Regex("""#define\s+WINDOWSAPPSDK_RELEASE_MAJORMINOR\s+(0x[0-9A-Fa-f]+)""")
    private val releaseVersionTagRegex = Regex("""#define\s+WINDOWSAPPSDK_RELEASE_VERSION_TAG_W\s+L"([^"]*)"""")
    private val runtimeVersionRegex = Regex("""#define\s+WINDOWSAPPSDK_RUNTIME_VERSION_UINT64\s+(0x[0-9A-Fa-f]+)u""")
    private val frameworkPackageFamilyNameRegex = Regex("""#define\s+WINDOWSAPPSDK_RUNTIME_PACKAGE_FRAMEWORK_PACKAGEFAMILYNAME\s+"([^"]+)"""")
    private val mainPackageFamilyNameRegex = Regex("""#define\s+WINDOWSAPPSDK_RUNTIME_PACKAGE_MAIN_PACKAGEFAMILYNAME\s+"([^"]+)"""")
    private val singletonPackageFamilyNameRegex = Regex("""#define\s+WINDOWSAPPSDK_RUNTIME_PACKAGE_SINGLETON_PACKAGEFAMILYNAME\s+"([^"]+)"""")
    private val ddlmPackageFamilyNameX86Regex = Regex("""#define\s+WINDOWSAPPSDK_RUNTIME_PACKAGE_DDLM_X86_PACKAGEFAMILYNAME\s+"([^"]+)"""")
    private val ddlmPackageFamilyNameX64Regex = Regex("""#define\s+WINDOWSAPPSDK_RUNTIME_PACKAGE_DDLM_X64_PACKAGEFAMILYNAME\s+"([^"]+)"""")
    private val ddlmPackageFamilyNameArm64Regex = Regex("""#define\s+WINDOWSAPPSDK_RUNTIME_PACKAGE_DDLM_ARM64_PACKAGEFAMILYNAME\s+"([^"]+)"""")

    private val arena: Arena = Arena.ofAuto()
    private val linker: Linker = Linker.nativeLinker()

    data class BootstrapLibrary(
        val path: Path,
        val lookup: SymbolLookup,
    )

    data class BootstrapVersionInfo(
        val releaseMajor: Int?,
        val releaseMinor: Int?,
        val majorMinorVersion: Int,
        val versionTag: String,
        val minVersion: Long,
        val frameworkPackageFamilyName: String?,
        val mainPackageFamilyName: String?,
        val singletonPackageFamilyName: String?,
        val ddlmPackageFamilyNameX86: String?,
        val ddlmPackageFamilyNameX64: String?,
        val ddlmPackageFamilyNameArm64: String?,
    ) {
        val releaseMajorMinorString: String?
            get() = if (releaseMajor != null && releaseMinor != null) {
                "$releaseMajor.$releaseMinor"
            } else {
                null
            }
    }

    fun parseNuGetGlobalPackagesOutput(output: String): List<Path> {
        return output.lineSequence()
            .map(String::trim)
            .filter { it.startsWith("global-packages:", ignoreCase = true) }
            .map { it.substringAfter(':').trim().trim('"') }
            .filter(String::isNotEmpty)
            .map(Path::of)
            .toList()
    }

    fun discoverBootstrapLibrary(): BootstrapLibrary? {
        val explicitCandidates = buildList {
            System.getenv("WINAPPSDK_BOOTSTRAP_DLL")?.let { add(Path.of(it)) }
            System.getProperty("dev.winrt.bootstrapDll")?.takeIf { it.isNotBlank() }?.let { add(Path.of(it)) }
            System.getProperty("dev.winrt.windowsAppSdkRoot")?.takeIf { it.isNotBlank() }?.let {
                addAll(bootstrapDllCandidates(Path.of(it)))
            }
        }

        return explicitCandidates
            .firstOrNull(Files::isRegularFile)
            ?.let { path ->
                BootstrapLibrary(
                    path = path,
                    lookup = SymbolLookup.libraryLookup(path, arena),
                )
            }
    }

    private fun bootstrapDllCandidates(root: Path): List<Path> {
        return if (Files.isDirectory(root)) {
            Files.walk(root).use { stream ->
                stream.filter { file ->
                    Files.isRegularFile(file) && file.fileName.toString()
                        .equals("Microsoft.WindowsAppRuntime.Bootstrap.dll", ignoreCase = true)
                }.toList()
            }
        } else {
            emptyList()
        }
    }

    fun discoverConfiguredVersionInfo(): BootstrapVersionInfo? {
        val candidates = buildList {
            System.getProperty("dev.winrt.windowsAppSdkRoot")
                ?.takeIf { it.isNotBlank() }
                ?.let { addAll(versionInfoHeaderCandidates(Path.of(it))) }
            System.getenv("WINAPPSDK_BOOTSTRAP_DLL")
                ?.takeIf { it.isNotBlank() }
                ?.let { addAll(versionInfoHeaderCandidates(Path.of(it))) }
            System.getProperty("dev.winrt.bootstrapDll")
                ?.takeIf { it.isNotBlank() }
                ?.let { addAll(versionInfoHeaderCandidates(Path.of(it))) }
        }

        val versionInfoHeader = candidates
            .distinct()
            .firstOrNull(Files::isRegularFile)
            ?: return null
        return parseVersionInfoHeader(Files.readString(versionInfoHeader))
    }

    private fun downcall(library: BootstrapLibrary, name: String, descriptor: FunctionDescriptor): MethodHandle {
        val symbol = library.lookup.find(name).orElse(null)
        requireNotNull(symbol) {
            "Bootstrap symbol not found: $name in ${library.path}"
        }
        return linker.downcallHandle(symbol, descriptor)
    }

    fun initialize(majorMinorVersion: Int = defaultMajorMinorVersion): Result<BootstrapLibrary> {
        return runCatching {
            val library = discoverBootstrapLibrary()
                ?: error("Microsoft.WindowsAppRuntime.Bootstrap.dll was not found")
            val versionInfo = discoverVersionInfo(library.path) ?: discoverConfiguredVersionInfo() ?: BootstrapVersionInfo(
                releaseMajor = 1,
                releaseMinor = 8,
                majorMinorVersion = majorMinorVersion,
                versionTag = tag,
                minVersion = defaultMinVersion,
                frameworkPackageFamilyName = null,
                mainPackageFamilyName = null,
                singletonPackageFamilyName = null,
                ddlmPackageFamilyNameX86 = null,
                ddlmPackageFamilyNameX64 = null,
                ddlmPackageFamilyNameArm64 = null,
            )
            val initialize2 = downcall(
                library,
                "MddBootstrapInitialize2",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_INT,
                ),
            )
            Arena.ofConfined().use { callArena ->
                val tagSegment = if (versionInfo.versionTag.isEmpty()) {
                    MemorySegment.NULL
                } else {
                    allocateWideString(callArena, versionInfo.versionTag)
                }
                val result = HResult(
                    initialize2.invokeWithArguments(
                        versionInfo.majorMinorVersion,
                        tagSegment,
                        versionInfo.minVersion,
                        0,
                    ) as Int,
                )
                result.requireSuccess("MddBootstrapInitialize2")
            }
            library
        }
    }

    fun shutdown(library: BootstrapLibrary): Result<Unit> {
        return runCatching {
            val shutdown = downcall(
                library,
                "MddBootstrapShutdown",
                FunctionDescriptor.ofVoid(),
            )
            shutdown.invokeWithArguments()
        }
    }

    private fun discoverVersionInfo(bootstrapDll: Path): BootstrapVersionInfo? {
        val versionInfoHeader = versionInfoHeaderCandidates(bootstrapDll)
            .firstOrNull(Files::isRegularFile)
            ?: return null
        return parseVersionInfoHeader(Files.readString(versionInfoHeader))
    }

    internal fun parseVersionInfoHeader(content: String): BootstrapVersionInfo {
        val releaseMajor = releaseMajorRegex.find(content)
            ?.groupValues?.get(1)
            ?.toIntOrNull()
        val releaseMinor = releaseMinorRegex.find(content)
            ?.groupValues?.get(1)
            ?.toIntOrNull()
        val majorMinor = releaseMajorMinorRegex.find(content)
            ?.groupValues?.get(1)
            ?.removePrefix("0x")
            ?.toInt(16)
            ?: error("WINDOWSAPPSDK_RELEASE_MAJORMINOR is missing")
        val versionTag = releaseVersionTagRegex.find(content)?.groupValues?.get(1) ?: ""
        val minVersion = runtimeVersionRegex.find(content)
            ?.groupValues?.get(1)
            ?.removePrefix("0x")
            ?.removeSuffix("u")
            ?.toULong(16)
            ?.toLong()
            ?: error("WINDOWSAPPSDK_RUNTIME_VERSION_UINT64 is missing")

        return BootstrapVersionInfo(
            releaseMajor = releaseMajor,
            releaseMinor = releaseMinor,
            majorMinorVersion = majorMinor,
            versionTag = versionTag,
            minVersion = minVersion,
            frameworkPackageFamilyName = frameworkPackageFamilyNameRegex.find(content)?.groupValues?.get(1),
            mainPackageFamilyName = mainPackageFamilyNameRegex.find(content)?.groupValues?.get(1),
            singletonPackageFamilyName = singletonPackageFamilyNameRegex.find(content)?.groupValues?.get(1),
            ddlmPackageFamilyNameX86 = ddlmPackageFamilyNameX86Regex.find(content)?.groupValues?.get(1),
            ddlmPackageFamilyNameX64 = ddlmPackageFamilyNameX64Regex.find(content)?.groupValues?.get(1),
            ddlmPackageFamilyNameArm64 = ddlmPackageFamilyNameArm64Regex.find(content)?.groupValues?.get(1),
        )
    }

    private fun versionInfoHeaderCandidates(location: Path): List<Path> {
        val initial = if (Files.isDirectory(location)) location else location.parent
        return generateSequence(initial) { current -> current.parent }
            .take(8)
            .map { it.resolve(versionInfoHeaderRelativePath) }
            .toList()
    }

    private fun allocateWideString(arena: Arena, value: String): MemorySegment {
        val bytes = (value + '\u0000').toByteArray(StandardCharsets.UTF_16LE)
        return arena.allocate(bytes.size.toLong(), 2).copyFrom(MemorySegment.ofArray(bytes))
    }
}
