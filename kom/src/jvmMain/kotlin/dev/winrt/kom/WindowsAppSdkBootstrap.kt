package dev.winrt.kom

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.nio.file.Files
import java.nio.file.Path

object WindowsAppSdkBootstrap {
    private const val defaultMajorMinorVersion = 0x00010006
    private const val tag = ""
    private const val minVersion = 0L

    private val arena: Arena = Arena.ofAuto()

    data class BootstrapLibrary(
        val path: Path,
        val lookup: SymbolLookup,
    )

    fun discoverBootstrapLibrary(): BootstrapLibrary? {
        val explicitCandidates = buildList {
            System.getenv("WINAPPSDK_BOOTSTRAP_DLL")?.let { add(Path.of(it)) }
            System.getProperty("dev.winrt.bootstrapDll")?.takeIf { it.isNotBlank() }?.let { add(Path.of(it)) }
            add(Path.of("C:/Program Files (x86)/Microsoft SDKs/Windows App SDK/bootstrap/Microsoft.WindowsAppRuntime.Bootstrap.dll"))
            add(Path.of("C:/Program Files (x86)/Mica For Everyone/Microsoft.WindowsAppRuntime.Bootstrap.dll"))
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
        return Jdk22Foreign.linker.downcallHandle(symbol, descriptor)
    }

    fun initialize(majorMinorVersion: Int = defaultMajorMinorVersion): Result<BootstrapLibrary> {
        return runCatching {
            val library = discoverBootstrapLibrary()
                ?: error("Microsoft.WindowsAppRuntime.Bootstrap.dll was not found")
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
                val tagSegment = if (tag.isEmpty()) {
                    MemorySegment.NULL
                } else {
                    callArena.allocateFrom(tag)
                }
                val result = HResult(
                    initialize2.invokeWithArguments(
                        majorMinorVersion,
                        tagSegment,
                        minVersion,
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
}
