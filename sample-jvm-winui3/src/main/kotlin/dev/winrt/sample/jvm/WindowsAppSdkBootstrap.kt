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
    private const val defaultMajorMinorVersion = 0x00010006
    private const val tag = ""
    private const val minVersion = 0L
    private val releaseMajorMinorRegex = Regex("""#define\s+WINDOWSAPPSDK_RELEASE_MAJORMINOR\s+(0x[0-9A-Fa-f]+)""")
    private val releaseVersionTagRegex = Regex("""#define\s+WINDOWSAPPSDK_RELEASE_VERSION_TAG_W\s+L"([^"]*)"""")
    private val runtimeVersionRegex = Regex("""#define\s+WINDOWSAPPSDK_RUNTIME_VERSION_UINT64\s+(0x[0-9A-Fa-f]+)u""")

    private val arena: Arena = Arena.ofAuto()
    private val linker: Linker = Linker.nativeLinker()

    data class BootstrapLibrary(
        val path: Path,
        val lookup: SymbolLookup,
    )

    data class BootstrapVersionInfo(
        val majorMinorVersion: Int,
        val versionTag: String,
        val minVersion: Long,
    )

    fun discoverBootstrapLibrary(): BootstrapLibrary? {
        val explicitCandidates = buildList {
            System.getenv("WINAPPSDK_BOOTSTRAP_DLL")?.let { add(Path.of(it)) }
            System.getProperty("dev.winrt.bootstrapDll")?.takeIf { it.isNotBlank() }?.let { add(Path.of(it)) }
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
            val versionInfo = discoverVersionInfo(library.path) ?: BootstrapVersionInfo(
                majorMinorVersion = majorMinorVersion,
                versionTag = tag,
                minVersion = minVersion,
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
        val candidates = buildList {
            inferPackageRoot(bootstrapDll)?.let { add(it.resolve("include").resolve("WindowsAppSDK-VersionInfo.h")) }
        }
        val versionInfoHeader = candidates.firstOrNull(Files::isRegularFile) ?: return null
        val content = Files.readString(versionInfoHeader)

        val majorMinor = releaseMajorMinorRegex.find(content)
            ?.groupValues?.get(1)
            ?.removePrefix("0x")
            ?.toInt(16)
            ?: return null
        val versionTag = releaseVersionTagRegex.find(content)?.groupValues?.get(1) ?: ""
        val minVersion = runtimeVersionRegex.find(content)
            ?.groupValues?.get(1)
            ?.removePrefix("0x")
            ?.removeSuffix("u")
            ?.toULong(16)
            ?.toLong()
            ?: return null

        return BootstrapVersionInfo(
            majorMinorVersion = majorMinor,
            versionTag = versionTag,
            minVersion = minVersion,
        )
    }

    private fun inferPackageRoot(bootstrapDll: Path): Path? {
        return bootstrapDll.parent?.parent?.parent?.parent
    }

    private fun allocateWideString(arena: Arena, value: String): MemorySegment {
        val bytes = (value + '\u0000').toByteArray(StandardCharsets.UTF_16LE)
        return arena.allocate(bytes.size.toLong(), 2).copyFrom(MemorySegment.ofArray(bytes))
    }
}
