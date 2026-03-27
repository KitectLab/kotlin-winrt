package dev.winrt.sample.jvm

import dev.winrt.kom.PlatformRuntime
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.nio.charset.StandardCharsets

object WindowsWindowProbe {
    private const val wmClose = 0x0010
    private val linker: Linker = Linker.nativeLinker()
    private val arena: Arena = Arena.ofAuto()
    private val user32: SymbolLookup = SymbolLookup.libraryLookup("user32", arena)

    fun findWindowByTitle(title: String): Boolean {
        val hwnd = findWindowHandleByTitle(title) ?: return false
        return hwnd != MemorySegment.NULL && hwnd.address() != 0L
    }

    fun closeWindowByTitle(title: String): Boolean {
        if (!PlatformRuntime.isWindows) {
            return false
        }

        val hwnd = findWindowHandleByTitle(title) ?: return false
        val postMessage = downcall(
            "PostMessageW",
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
            ),
        )
        val result = postMessage.invokeWithArguments(
            hwnd,
            wmClose,
            MemorySegment.NULL,
            MemorySegment.NULL,
        ) as Int
        return result != 0
    }

    private fun findWindowHandleByTitle(title: String): MemorySegment? {
        if (!PlatformRuntime.isWindows) {
            return null
        }

        Arena.ofConfined().use { callArena ->
            val titleSegment = allocateWideString(callArena, title)
            val findWindow = downcall(
                "FindWindowW",
                FunctionDescriptor.of(
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                ),
            )
            val hwnd = findWindow.invokeWithArguments(
                MemorySegment.NULL,
                titleSegment,
            ) as MemorySegment
            return hwnd
        }
    }

    fun waitForWindowByTitle(title: String, timeoutMillis: Long = 2_000L, pollMillis: Long = 50L): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (findWindowByTitle(title)) {
                return true
            }
            Thread.sleep(pollMillis)
        }
        return findWindowByTitle(title)
    }

    private fun allocateWideString(arena: Arena, value: String): MemorySegment {
        val bytes = (value + '\u0000').toByteArray(StandardCharsets.UTF_16LE)
        return arena.allocate(bytes.size.toLong(), 2).copyFrom(MemorySegment.ofArray(bytes))
    }

    private fun downcall(name: String, descriptor: FunctionDescriptor): MethodHandle {
        val symbol = user32.find(name).orElse(null)
        requireNotNull(symbol) {
            "Win32 symbol not found: $name"
        }
        return linker.downcallHandle(symbol, descriptor)
    }
}
