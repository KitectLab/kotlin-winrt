package dev.winrt.sample.jvm

import dev.winrt.kom.KomException
import dev.winrt.kom.PlatformRuntime
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle

object WindowsMessageLoop {
    private val linker: Linker = Linker.nativeLinker()
    private val libraryArena: Arena = Arena.ofAuto()
    private val user32: SymbolLookup = SymbolLookup.libraryLookup("user32", libraryArena)
    private val msgLayout: MemoryLayout = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("hwnd"),
        ValueLayout.JAVA_INT.withName("message"),
        MemoryLayout.paddingLayout(4),
        ValueLayout.ADDRESS.withName("wParam"),
        ValueLayout.ADDRESS.withName("lParam"),
        ValueLayout.JAVA_INT.withName("time"),
        ValueLayout.JAVA_INT.withName("ptX"),
        ValueLayout.JAVA_INT.withName("ptY"),
        ValueLayout.JAVA_INT.withName("lPrivate"),
        MemoryLayout.paddingLayout(4),
    )

    fun run() {
        if (!PlatformRuntime.isWindows) {
            return
        }

        Arena.ofConfined().use { arena ->
            val message = arena.allocate(msgLayout)
            val getMessage = downcall(
                "GetMessageW",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                ),
            )
            val translateMessage = downcall(
                "TranslateMessage",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                ),
            )
            val dispatchMessage = downcall(
                "DispatchMessageW",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS,
                ),
            )

            while (true) {
                val result = getMessage.invokeWithArguments(
                    message,
                    MemorySegment.NULL,
                    0,
                    0,
                ) as Int
                when {
                    result > 0 -> {
                        translateMessage.invokeWithArguments(message)
                        dispatchMessage.invokeWithArguments(message)
                    }
                    result == 0 -> return
                    else -> throw KomException("GetMessageW failed")
                }
            }
        }
    }

    private fun downcall(name: String, descriptor: FunctionDescriptor): MethodHandle {
        val symbol = user32.find(name).orElse(null)
        requireNotNull(symbol) {
            "Win32 symbol not found: $name"
        }
        return linker.downcallHandle(symbol, descriptor)
    }
}
