package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult
import dev.winrt.kom.HString
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformHStringBridge
import dev.winrt.kom.JvmWinRtRuntime
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class JvmWinRtObjectStub private constructor(
    private val arena: Arena,
    val primaryPointer: ComPtr,
    private val interfaceAddresses: Set<Long>,
    private val sharedState: SharedState,
) : AutoCloseable {
    fun setQueryInterfaceFallback(resolver: (Guid) -> ComPtr?) {
        sharedState.queryInterfaceFallback.set(resolver)
    }

    fun clearQueryInterfaceFallback() {
        sharedState.queryInterfaceFallback.set(null)
    }

    override fun close() {
        interfaceAddresses.forEach(states::remove)
        arena.close()
    }

    companion object {
        private val iUnknownIid = guidOf("00000000-0000-0000-c000-000000000046")
        private val iInspectableIid = Inspectable.iinspectableIid
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

        private val getIidsStub = linker.upcallStub(
            lookup.findStatic(
                JvmWinRtObjectStub::class.java,
                "getIids",
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

        private val getRuntimeClassNameStub = linker.upcallStub(
            lookup.findStatic(
                JvmWinRtObjectStub::class.java,
                "getRuntimeClassName",
                MethodType.methodType(
                    Int::class.javaPrimitiveType,
                    MemorySegment::class.java,
                    MemorySegment::class.java,
                ),
            ),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
            ),
            libraryArena,
        )

        private val getTrustLevelStub = linker.upcallStub(
            lookup.findStatic(
                JvmWinRtObjectStub::class.java,
                "getTrustLevel",
                MethodType.methodType(
                    Int::class.javaPrimitiveType,
                    MemorySegment::class.java,
                    MemorySegment::class.java,
                ),
            ),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
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

        private val objectArgObjectInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeObjectArgObject",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
                MemorySegment::class.java,
            ),
        )

        private val stringArgObjectInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeStringArgObject",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
                MemorySegment::class.java,
            ),
        )

        fun create(vararg interfaces: InterfaceSpec): JvmWinRtObjectStub {
            return createWithRuntimeClassName(null, *interfaces)
        }

        fun createWithRuntimeClassName(runtimeClassName: String?, vararg interfaces: InterfaceSpec): JvmWinRtObjectStub {
            require(interfaces.isNotEmpty()) { "At least one interface is required" }
            val arena = Arena.ofShared()
            val sharedState = SharedState(runtimeClassName)
            val interfaceAddresses = linkedSetOf<Long>()

            interfaces.forEach { spec ->
                val maxSlot = maxOf(
                    5,
                    spec.noArgUnitMethods.keys.maxOrNull() ?: 2,
                    spec.objectArgUnitMethods.keys.maxOrNull() ?: 2,
                    spec.objectArgObjectMethods.keys.maxOrNull() ?: 2,
                    spec.stringArgObjectMethods.keys.maxOrNull() ?: 2,
                )
                val vtable = arena.allocate(ValueLayout.ADDRESS.byteSize() * (maxSlot + 1), ValueLayout.ADDRESS.byteAlignment())
                val instance = arena.allocate(ValueLayout.ADDRESS)
                vtable.setAtIndex(ValueLayout.ADDRESS, 0, queryInterfaceStub)
                vtable.setAtIndex(ValueLayout.ADDRESS, 1, addRefStub)
                vtable.setAtIndex(ValueLayout.ADDRESS, 2, releaseStub)
                vtable.setAtIndex(ValueLayout.ADDRESS, 3, getIidsStub)
                vtable.setAtIndex(ValueLayout.ADDRESS, 4, getRuntimeClassNameStub)
                vtable.setAtIndex(ValueLayout.ADDRESS, 5, getTrustLevelStub)

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
                spec.objectArgObjectMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(objectArgObjectInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.objectArgObjectMethods[interfaceAddress to slot] = method
                }
                spec.stringArgObjectMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(stringArgObjectInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.stringArgObjectMethods[interfaceAddress to slot] = method
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
                sharedState = sharedState,
            )
        }

        @JvmStatic
        private fun queryInterface(thisPointer: MemorySegment, iid: MemorySegment, result: MemorySegment): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            val match = when {
                matchesGuid(iid, iUnknownIid) || matchesGuid(iid, iInspectableIid) || matchesGuid(iid, iAgileObjectIid) ->
                    ComPtr(AbiIntPtr(thisPointer.address()))
                else -> state.interfacesByIid.entries.firstOrNull { (_, pointer) ->
                    matchesGuid(iid, parseGuid(pointer, state))
                }?.value ?: runCatching {
                    state.queryInterfaceFallback.get()?.invoke(readGuid(iid))
                }.getOrNull()
            }
            return if (match != null) {
                writeAddress(result, match)
                if (state.ownsPointer(match)) {
                    state.refCount.incrementAndGet()
                }
                0
            } else {
                writeAddress(result, ComPtr.NULL)
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
        private fun getIids(thisPointer: MemorySegment, iidCount: MemorySegment, iids: MemorySegment): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            iidCount.reinterpret(ValueLayout.JAVA_INT.byteSize().toLong()).set(ValueLayout.JAVA_INT, 0L, 0)
            writeAddress(iids, ComPtr.NULL)
            return HResult(0).value
        }

        @JvmStatic
        private fun getRuntimeClassName(thisPointer: MemorySegment, className: MemorySegment): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.runtimeClassName?.let(JvmWinRtRuntime::createHString)
                writeAddress(className, value?.let { ComPtr(AbiIntPtr(it.raw)) } ?: ComPtr.NULL)
                HResult(0).value
            }.getOrElse {
                writeAddress(className, ComPtr.NULL)
                HResult(0x80004005.toInt()).value
            }
        }

        @JvmStatic
        private fun getTrustLevel(thisPointer: MemorySegment, trustLevel: MemorySegment): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            trustLevel.reinterpret(ValueLayout.JAVA_INT.byteSize().toLong()).set(ValueLayout.JAVA_INT, 0L, 0)
            return HResult(0).value
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

        @JvmStatic
        private fun invokeObjectArgObject(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            arg: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.objectArgObjectMethods[interfaceAddress to slot]
                    ?.invoke(ComPtr(AbiIntPtr(arg.address())))
                    ?: return KnownHResults.E_NOTIMPL.value
                writeAddress(result, value)
                HResult(0).value
            }.getOrElse {
                writeAddress(result, ComPtr.NULL)
                HResult(0x80004005.toInt()).value
            }
        }

        @JvmStatic
        private fun invokeStringArgObject(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            arg: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.stringArgObjectMethods[interfaceAddress to slot]
                    ?.invoke(PlatformHStringBridge.toKotlinString(HString(arg.address())))
                    ?: return KnownHResults.E_NOTIMPL.value
                writeAddress(result, value)
                HResult(0).value
            }.getOrElse {
                writeAddress(result, ComPtr.NULL)
                HResult(0x80004005.toInt()).value
            }
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

        private fun readGuid(segment: MemorySegment): Guid {
            val guidSegment = segment.reinterpret(16L)
            return Guid(
                data1 = guidSegment.get(ValueLayout.JAVA_INT, 0L),
                data2 = guidSegment.get(ValueLayout.JAVA_SHORT, 4L),
                data3 = guidSegment.get(ValueLayout.JAVA_SHORT, 6L),
                data4 = ByteArray(8) { index ->
                    guidSegment.get(ValueLayout.JAVA_BYTE, 8L + index)
                },
            )
        }

        private fun writeAddress(result: MemorySegment, pointer: ComPtr) {
            result.reinterpret(ValueLayout.ADDRESS.byteSize()).set(
                ValueLayout.ADDRESS,
                0,
                if (pointer.isNull) MemorySegment.NULL else MemorySegment.ofAddress(pointer.value.rawValue),
            )
        }
    }

    data class InterfaceSpec(
        val iid: Guid,
        val noArgUnitMethods: Map<Int, () -> HResult> = emptyMap(),
        val objectArgUnitMethods: Map<Int, (ComPtr) -> HResult> = emptyMap(),
        val objectArgObjectMethods: Map<Int, (ComPtr) -> ComPtr> = emptyMap(),
        val stringArgObjectMethods: Map<Int, (String) -> ComPtr> = emptyMap(),
    )

    private class SharedState(
        val runtimeClassName: String?,
    ) {
        val refCount = AtomicInteger(1)
        val interfacesByIid = linkedMapOf<String, ComPtr>()
        val noArgUnitMethods = mutableMapOf<Pair<Long, Int>, () -> HResult>()
        val objectArgUnitMethods = mutableMapOf<Pair<Long, Int>, (ComPtr) -> HResult>()
        val objectArgObjectMethods = mutableMapOf<Pair<Long, Int>, (ComPtr) -> ComPtr>()
        val stringArgObjectMethods = mutableMapOf<Pair<Long, Int>, (String) -> ComPtr>()
        val queryInterfaceFallback = AtomicReference<((Guid) -> ComPtr?)?>(null)

        fun ownsPointer(pointer: ComPtr): Boolean {
            return interfacesByIid.values.any { it == pointer } || pointer.value.rawValue in interfacesByIid.values.map { it.value.rawValue }
        }
    }
}

private val Guid.canonical: String
    get() = toString()
