package dev.winrt.kom

import java.lang.foreign.Arena
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup

internal object Jdk22Foreign {
    val linker: Linker = Linker.nativeLinker()
    val arena: Arena = Arena.ofAuto()

    val windowsLookup: SymbolLookup? by lazy {
        if (!System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            null
        } else {
            runCatching { SymbolLookup.libraryLookup("combase", arena) }.getOrNull()
        }
    }

    fun pointerOf(value: ComPtr): MemorySegment = MemorySegment.ofAddress(value.value.rawValue)
}
