package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult
import dev.winrt.kom.KnownHResults
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class JvmWinRtObjectArgBooleanDelegate private constructor(
    private val arena: Arena,
    private val instance: MemorySegment,
) : AutoCloseable {
    val pointer: ComPtr = ComPtr(AbiIntPtr(instance.address()))

    override fun close() {
        states.remove(instance.address())
        arena.close()
    }

    companion object {
        private val iUnknownIid = guidOf("00000000-0000-0000-c000-000000000046")
        private val iAgileObjectIid = guidOf("94ea2b94-e9cc-49e0-c0ff-ee64ca8f5b90")
        private val linker: Linker = Linker.nativeLinker()
        private val libraryArena: Arena = Arena.ofAuto()
        private val lookup = MethodHandles.lookup()
        private val states = ConcurrentHashMap<Long, State>()

        private val queryInterfaceStub = linker.upcallStub(
            lookup.findStatic(
                JvmWinRtObjectArgBooleanDelegate::class.java,
                "queryInterface",
                MethodType.methodType(
                    Int::class.javaPrimitiveType,
                    MemorySegment::class.java,
                    MemorySegment::class.java,
                    MemorySegment::class.java,
                ),
            ),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
            ),
            libraryArena,
        )

        private val addRefStub = linker.upcallStub(
            lookup.findStatic(
                JvmWinRtObjectArgBooleanDelegate::class.java,
                "addRef",
                MethodType.methodType(
                    Int::class.javaPrimitiveType,
                    MemorySegment::class.java,
                ),
            ),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
            ),
            libraryArena,
        )

        private val releaseStub = linker.upcallStub(
            lookup.findStatic(
                JvmWinRtObjectArgBooleanDelegate::class.java,
                "release",
                MethodType.methodType(
                    Int::class.javaPrimitiveType,
                    MemorySegment::class.java,
                ),
            ),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
            ),
            libraryArena,
        )

        private val invokeStub = linker.upcallStub(
            lookup.findStatic(
                JvmWinRtObjectArgBooleanDelegate::class.java,
                "invoke",
                MethodType.methodType(
                    Int::class.javaPrimitiveType,
                    MemorySegment::class.java,
                    MemorySegment::class.java,
                    MemorySegment::class.java,
                ),
            ),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
            ),
            libraryArena,
        )

        fun create(iid: Guid, invoke: (ComPtr) -> Boolean): JvmWinRtObjectArgBooleanDelegate {
            val arena = Arena.ofShared()
            val vtable = arena.allocate(ValueLayout.ADDRESS.byteSize() * 4, ValueLayout.ADDRESS.byteAlignment())
            val instance = arena.allocate(ValueLayout.ADDRESS)
            vtable.setAtIndex(ValueLayout.ADDRESS, 0, queryInterfaceStub)
            vtable.setAtIndex(ValueLayout.ADDRESS, 1, addRefStub)
            vtable.setAtIndex(ValueLayout.ADDRESS, 2, releaseStub)
            vtable.setAtIndex(ValueLayout.ADDRESS, 3, invokeStub)
            instance.set(ValueLayout.ADDRESS, 0, vtable)
            states[instance.address()] = State(iid, invoke)
            return JvmWinRtObjectArgBooleanDelegate(arena, instance)
        }

        @JvmStatic
        private fun queryInterface(thisPointer: MemorySegment, iid: MemorySegment, result: MemorySegment): Int {
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

        @JvmStatic
        private fun addRef(thisPointer: MemorySegment): Int {
            val state = states[thisPointer.address()] ?: return 1
            return state.refCount.incrementAndGet()
        }

        @JvmStatic
        private fun release(thisPointer: MemorySegment): Int {
            val state = states[thisPointer.address()] ?: return 0
            return state.refCount.decrementAndGet()
        }

        @JvmStatic
        private fun invoke(thisPointer: MemorySegment, arg: MemorySegment, result: MemorySegment): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            result.reinterpret(ValueLayout.JAVA_INT.byteSize().toLong()).set(
                ValueLayout.JAVA_INT,
                0,
                if (state.invoke(ComPtr(AbiIntPtr(arg.address())))) 1 else 0,
            )
            return HResult(0).value
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
    }

    private class State(
        val iid: Guid,
        val invoke: (ComPtr) -> Boolean,
    ) {
        val refCount = AtomicInteger(1)
    }
}
