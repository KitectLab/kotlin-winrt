package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.KnownHResults
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal abstract class JvmWinRtDelegateSupport {
    protected val linker: Linker = Linker.nativeLinker()
    protected val libraryArena: Arena = Arena.ofAuto()

    private val iUnknownIid = guidOf("00000000-0000-0000-c000-000000000046")
    private val iAgileObjectIid = guidOf("94ea2b94-e9cc-49e0-c0ff-ee64ca8f5b90")
    private val lookup = MethodHandles.lookup()
    private val states = ConcurrentHashMap<Long, State>()

    private val queryInterfaceStub = linker.upcallStub(
        lookup.findVirtual(
            JvmWinRtDelegateSupport::class.java,
            "queryInterface",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
                MemorySegment::class.java,
            ),
        ).bindTo(this),
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
        ),
        libraryArena,
    )

    private val addRefStub = linker.upcallStub(
        lookup.findVirtual(
            JvmWinRtDelegateSupport::class.java,
            "addRef",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
            ),
        ).bindTo(this),
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
        ),
        libraryArena,
    )

    private val releaseStub = linker.upcallStub(
        lookup.findVirtual(
            JvmWinRtDelegateSupport::class.java,
            "release",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
            ),
        ).bindTo(this),
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
        ),
        libraryArena,
    )

    protected fun createHandle(iid: Guid, invokeStub: MemorySegment): WinRtDelegateHandle {
        val arena = Arena.ofShared()
        val vtable = arena.allocate(ValueLayout.ADDRESS.byteSize() * 4, ValueLayout.ADDRESS.byteAlignment())
        val instance = arena.allocate(ValueLayout.ADDRESS)
        states[instance.address()] = State(iid)
        vtable.setAtIndex(ValueLayout.ADDRESS, 0, queryInterfaceStub)
        vtable.setAtIndex(ValueLayout.ADDRESS, 1, addRefStub)
        vtable.setAtIndex(ValueLayout.ADDRESS, 2, releaseStub)
        vtable.setAtIndex(ValueLayout.ADDRESS, 3, invokeStub)
        instance.set(ValueLayout.ADDRESS, 0, vtable)
        return object : WinRtDelegateHandle {
            override val pointer: ComPtr = ComPtr(AbiIntPtr(instance.address()))
            private val closed = AtomicBoolean(false)

            override fun close() {
                if (!closed.compareAndSet(false, true)) {
                    return
                }
                states.remove(instance.address())
                arena.close()
            }
        }
    }

    protected fun hasState(thisPointer: MemorySegment): Boolean = states.containsKey(thisPointer.address())

    @Suppress("unused")
    fun queryInterface(thisPointer: MemorySegment, iid: MemorySegment, result: MemorySegment): Int {
        val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
        return if (
            matchesGuid(iid, iUnknownIid) ||
            matchesGuid(iid, iAgileObjectIid) ||
            matchesGuid(iid, state.iid)
        ) {
            result.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0, thisPointer)
            state.refCount.incrementAndGet()
            0
        } else {
            result.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0, MemorySegment.NULL)
            KnownHResults.E_NOINTERFACE.value
        }
    }

    @Suppress("unused")
    fun addRef(thisPointer: MemorySegment): Int {
        val state = states[thisPointer.address()] ?: return 1
        return state.refCount.incrementAndGet()
    }

    @Suppress("unused")
    fun release(thisPointer: MemorySegment): Int {
        val state = states[thisPointer.address()] ?: return 0
        return state.refCount.decrementAndGet()
    }

    private fun matchesGuid(segment: MemorySegment, expected: Guid): Boolean {
        val guidSegment = segment.reinterpret(16L)
        return guidSegment.get(ValueLayout.JAVA_INT, 0L) == expected.data1 &&
            guidSegment.get(ValueLayout.JAVA_SHORT, 4L) == expected.data2 &&
            guidSegment.get(ValueLayout.JAVA_SHORT, 6L) == expected.data3 &&
            expected.data4.indices.all { index ->
                guidSegment.get(ValueLayout.JAVA_BYTE, 8L + index) == expected.data4[index]
            }
    }

    private data class State(
        val iid: Guid,
        val refCount: AtomicInteger = AtomicInteger(1),
    )
}
