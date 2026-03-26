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

class JvmWinRtObjectStub private constructor(
    private val arena: Arena,
    val primaryPointer: ComPtr,
    private val interfaceAddresses: Set<Long>,
) : AutoCloseable {
    override fun close() {
        interfaceAddresses.forEach(states::remove)
        arena.close()
    }

    companion object {
        private val iUnknownIid = guidOf("00000000-0000-0000-c000-000000000046")
        private val iAgileObjectIid = guidOf("94ea2b94-e9cc-49e0-c0ff-ee64ca8f5b90")
        private val linker: Linker = Linker.nativeLinker()
        private val lookup = MethodHandles.lookup()
        private val libraryArena: Arena = Arena.ofAuto()
        private val states = ConcurrentHashMap<Long, SharedState>()

        private val queryInterfaceStub = linker.upcallStub(
            lookup.findStatic(
                JvmWinRtObjectStub::class.java,
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
                JvmWinRtObjectStub::class.java,
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
                JvmWinRtObjectStub::class.java,
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

        private val noArgInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeNoArg",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
            ),
        )

        private val objectArgInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeObjectArg",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
            ),
        )

        fun create(vararg interfaces: InterfaceSpec): JvmWinRtObjectStub {
            require(interfaces.isNotEmpty()) { "At least one interface is required" }
            val arena = Arena.ofShared()
            val sharedState = SharedState()
            val interfaceAddresses = linkedSetOf<Long>()

            interfaces.forEach { spec ->
                val maxSlot = maxOf(
                    2,
                    spec.noArgUnitMethods.keys.maxOrNull() ?: 2,
                    spec.objectArgUnitMethods.keys.maxOrNull() ?: 2,
                )
                val vtable = arena.allocate(ValueLayout.ADDRESS.byteSize() * (maxSlot + 1), ValueLayout.ADDRESS.byteAlignment())
                val instance = arena.allocate(ValueLayout.ADDRESS)
                vtable.setAtIndex(ValueLayout.ADDRESS, 0, queryInterfaceStub)
                vtable.setAtIndex(ValueLayout.ADDRESS, 1, addRefStub)
                vtable.setAtIndex(ValueLayout.ADDRESS, 2, releaseStub)

                val interfaceAddress = instance.address()
                spec.noArgUnitMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(noArgInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.noArgUnitMethods[interfaceAddress to slot] = method
                }
                spec.objectArgUnitMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(objectArgInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.objectArgUnitMethods[interfaceAddress to slot] = method
                }

                instance.set(ValueLayout.ADDRESS, 0, vtable)
                val pointer = ComPtr(AbiIntPtr(interfaceAddress))
                sharedState.interfacesByIid[spec.iid.canonical] = pointer
                states[interfaceAddress] = sharedState
                interfaceAddresses += interfaceAddress
            }

            return JvmWinRtObjectStub(
                arena = arena,
                primaryPointer = sharedState.interfacesByIid[interfaces.first().iid.canonical]!!,
                interfaceAddresses = interfaceAddresses,
            )
        }

        @JvmStatic
        private fun queryInterface(thisPointer: MemorySegment, iid: MemorySegment, result: MemorySegment): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            val match = when {
                matchesGuid(iid, iUnknownIid) || matchesGuid(iid, iAgileObjectIid) ->
                    ComPtr(AbiIntPtr(thisPointer.address()))
                else -> state.interfacesByIid.entries.firstOrNull { (_, pointer) ->
                    matchesGuid(iid, parseGuid(pointer, state))
                }?.value
            }
            return if (match != null) {
                result.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0, MemorySegment.ofAddress(match.value.rawValue))
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
        private fun invokeNoArg(interfaceAddress: Long, slot: Int, thisPointer: MemorySegment): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return state.noArgUnitMethods[interfaceAddress to slot]?.invoke()?.value ?: KnownHResults.E_NOTIMPL.value
        }

        @JvmStatic
        private fun invokeObjectArg(interfaceAddress: Long, slot: Int, thisPointer: MemorySegment, arg: MemorySegment): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return state.objectArgUnitMethods[interfaceAddress to slot]
                ?.invoke(ComPtr(AbiIntPtr(arg.address())))
                ?.value
                ?: KnownHResults.E_NOTIMPL.value
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

        private fun parseGuid(pointer: ComPtr, state: SharedState): Guid {
            return state.interfacesByIid.entries.first { it.value == pointer }.let { guidOf(it.key) }
        }
    }

    data class InterfaceSpec(
        val iid: Guid,
        val noArgUnitMethods: Map<Int, () -> HResult> = emptyMap(),
        val objectArgUnitMethods: Map<Int, (ComPtr) -> HResult> = emptyMap(),
    )

    private class SharedState {
        val refCount = AtomicInteger(1)
        val interfacesByIid = linkedMapOf<String, ComPtr>()
        val noArgUnitMethods = mutableMapOf<Pair<Long, Int>, () -> HResult>()
        val objectArgUnitMethods = mutableMapOf<Pair<Long, Int>, (ComPtr) -> HResult>()
    }
}

private val Guid.canonical: String
    get() = toString()
