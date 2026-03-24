package dev.winrt.kom

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.foreign.SymbolLookup
import java.lang.invoke.MethodHandle

internal object Jdk22Foreign {
    val linker: Linker = Linker.nativeLinker()
    val arena: Arena = Arena.ofAuto()
    private val addressLayout = ValueLayout.ADDRESS
    private val intLayout = ValueLayout.JAVA_INT
    private val longLayout = ValueLayout.JAVA_LONG

    val windowsLookups: List<SymbolLookup> by lazy {
        if (!System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            emptyList()
        } else {
            sequenceOf("ole32", "combase")
                .mapNotNull { library -> runCatching { SymbolLookup.libraryLookup(library, arena) }.getOrNull() }
                .toList()
        }
    }

    fun pointerOf(value: ComPtr): MemorySegment = MemorySegment.ofAddress(value.value.rawValue)

    fun guidSegment(guid: Guid, arena: Arena): MemorySegment {
        val segment = arena.allocate(16)
        segment.set(intLayout, 0, guid.data1)
        segment.set(ValueLayout.JAVA_SHORT, 4, guid.data2)
        segment.set(ValueLayout.JAVA_SHORT, 6, guid.data3)
        guid.data4.forEachIndexed { index, byte ->
            segment.set(ValueLayout.JAVA_BYTE, 8L + index, byte)
        }
        return segment
    }

    fun dereferencePointer(pointer: ComPtr): MemorySegment {
        return pointerOf(pointer).reinterpret(addressLayout.byteSize())
    }

    fun vtableEntry(instance: ComPtr, index: Int): MemorySegment {
        val objectPointer = dereferencePointer(instance)
        val vtablePointer = objectPointer.get(addressLayout, 0)
        return vtablePointer
            .reinterpret((index + 1L) * addressLayout.byteSize())
            .getAtIndex(addressLayout, index.toLong())
    }

    fun downcall(symbolName: String, descriptor: FunctionDescriptor): MethodHandle {
        val symbol = windowsLookups
            .asSequence()
            .mapNotNull { lookup -> lookup.find(symbolName).orElse(null) }
            .firstOrNull()

        requireNotNull(symbol) {
            "Windows symbol not found: $symbolName"
        }
        return linker.downcallHandle(symbol, descriptor)
    }

    val queryInterfaceHandle: MethodHandle by lazy {
        linker.downcallHandle(
            FunctionDescriptor.of(intLayout, addressLayout, addressLayout, addressLayout),
        )
    }

    val addRefHandle: MethodHandle by lazy {
        linker.downcallHandle(
            FunctionDescriptor.of(intLayout, addressLayout),
        )
    }

    val releaseHandle: MethodHandle by lazy {
        linker.downcallHandle(
            FunctionDescriptor.of(intLayout, addressLayout),
        )
    }

    val coInitializeExHandle: MethodHandle by lazy {
        downcall(
            "CoInitializeEx",
            FunctionDescriptor.of(intLayout, addressLayout, intLayout),
        )
    }

    val coUninitializeHandle: MethodHandle by lazy {
        downcall(
            "CoUninitialize",
            FunctionDescriptor.ofVoid(),
        )
    }

    val roInitializeHandle: MethodHandle by lazy {
        downcall(
            "RoInitialize",
            FunctionDescriptor.of(intLayout, intLayout),
        )
    }

    val roUninitializeHandle: MethodHandle by lazy {
        downcall(
            "RoUninitialize",
            FunctionDescriptor.ofVoid(),
        )
    }

    val windowsCreateStringHandle: MethodHandle by lazy {
        downcall(
            "WindowsCreateString",
            FunctionDescriptor.of(intLayout, addressLayout, intLayout, addressLayout),
        )
    }

    val windowsDeleteStringHandle: MethodHandle by lazy {
        downcall(
            "WindowsDeleteString",
            FunctionDescriptor.of(intLayout, addressLayout),
        )
    }

    val windowsGetStringRawBufferHandle: MethodHandle by lazy {
        downcall(
            "WindowsGetStringRawBuffer",
            FunctionDescriptor.of(addressLayout, addressLayout, addressLayout),
        )
    }

    val roGetActivationFactoryHandle: MethodHandle by lazy {
        downcall(
            "RoGetActivationFactory",
            FunctionDescriptor.of(intLayout, addressLayout, addressLayout, addressLayout),
        )
    }

    val activateInstanceHandle: MethodHandle by lazy {
        linker.downcallHandle(
            FunctionDescriptor.of(intLayout, addressLayout, addressLayout),
        )
    }

    val hstringMethodHandle: MethodHandle by lazy {
        linker.downcallHandle(
            FunctionDescriptor.of(intLayout, addressLayout, addressLayout),
        )
    }

    fun addressResult(segment: MemorySegment): ComPtr = ComPtr(AbiIntPtr(segment.address()))

    fun longToUInt(value: Int): UInt = value.toUInt()
}
