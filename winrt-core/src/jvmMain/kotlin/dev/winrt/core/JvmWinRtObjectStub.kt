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
import java.lang.foreign.SymbolLookup
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
        private val iidArena: Arena = Arena.ofShared()
        private val states = ConcurrentHashMap<Long, SharedState>()
        private val isWindows = System.getProperty("os.name").contains("Windows", ignoreCase = true)
        private val ole32: SymbolLookup? = if (isWindows) SymbolLookup.libraryLookup("ole32", libraryArena) else null
        private val coTaskMemAlloc = ole32?.find("CoTaskMemAlloc")?.get()?.let { symbol ->
            linker.downcallHandle(
                symbol,
                FunctionDescriptor.of(
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_LONG,
                ),
            )
        }

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

        private val noArgObjectInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeNoArgObject",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
            ),
        )

        private val noArgBooleanInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeNoArgBoolean",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
            ),
        )

        private val noArgUInt32InvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeNoArgUInt32",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
            ),
        )

        private val noArgHStringInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeNoArgHString",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
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

        private val stringArgBooleanInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeStringArgBoolean",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
                MemorySegment::class.java,
            ),
        )

        private val stringArgHStringInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeStringArgHString",
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
                    spec.noArgObjectMethods.keys.maxOrNull() ?: 2,
                    spec.noArgBooleanMethods.keys.maxOrNull() ?: 2,
                    spec.noArgUInt32Methods.keys.maxOrNull() ?: 2,
                    spec.noArgHStringMethods.keys.maxOrNull() ?: 2,
                    spec.objectArgUnitMethods.keys.maxOrNull() ?: 2,
                    spec.objectArgObjectMethods.keys.maxOrNull() ?: 2,
                    spec.stringArgObjectMethods.keys.maxOrNull() ?: 2,
                    spec.stringArgBooleanMethods.keys.maxOrNull() ?: 2,
                    spec.stringArgHStringMethods.keys.maxOrNull() ?: 2,
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
                spec.noArgObjectMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(noArgObjectInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.noArgObjectMethods[interfaceAddress to slot] = method
                }
                spec.noArgBooleanMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(noArgBooleanInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.noArgBooleanMethods[interfaceAddress to slot] = method
                }
                spec.noArgUInt32Methods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(noArgUInt32InvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.noArgUInt32Methods[interfaceAddress to slot] = method
                }
                spec.noArgHStringMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(noArgHStringInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.noArgHStringMethods[interfaceAddress to slot] = method
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
                spec.stringArgBooleanMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(stringArgBooleanInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.stringArgBooleanMethods[interfaceAddress to slot] = method
                }
                spec.stringArgHStringMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(stringArgHStringInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.stringArgHStringMethods[interfaceAddress to slot] = method
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
            return runCatching {
                val interfaceIids = state.interfacesByIid.keys.map(::guidOf)
                iidCount.reinterpret(ValueLayout.JAVA_INT.byteSize().toLong()).set(
                    ValueLayout.JAVA_INT,
                    0L,
                    interfaceIids.size,
                )
                if (interfaceIids.isEmpty()) {
                    writeAddress(iids, ComPtr.NULL)
                    return@runCatching HResult(0).value
                }

                val iidArray = allocateIidArray(interfaceIids.size)
                interfaceIids.forEachIndexed { index, iid ->
                    writeGuid(iidArray.asSlice(index * 16L, 16L), iid)
                }
                writeAddress(iids, ComPtr(AbiIntPtr(iidArray.address())))
                HResult(0).value
            }.getOrElse {
                iidCount.reinterpret(ValueLayout.JAVA_INT.byteSize().toLong()).set(ValueLayout.JAVA_INT, 0L, 0)
                writeAddress(iids, ComPtr.NULL)
                HResult(0x8007000E.toInt()).value
            }
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
        private fun invokeNoArgObject(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.noArgObjectMethods[interfaceAddress to slot]
                    ?.invoke()
                    ?: return KnownHResults.E_NOTIMPL.value
                writeAddress(result, value)
                HResult(0).value
            }.getOrElse {
                writeAddress(result, ComPtr.NULL)
                HResult(0x80004005.toInt()).value
            }
        }

        @JvmStatic
        private fun invokeNoArgBoolean(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.noArgBooleanMethods[interfaceAddress to slot]
                    ?.invoke()
                    ?: return KnownHResults.E_NOTIMPL.value
                result.reinterpret(ValueLayout.JAVA_INT.byteSize().toLong()).set(
                    ValueLayout.JAVA_INT,
                    0L,
                    if (value) 1 else 0,
                )
                HResult(0).value
            }.getOrElse {
                result.reinterpret(ValueLayout.JAVA_INT.byteSize().toLong()).set(ValueLayout.JAVA_INT, 0L, 0)
                HResult(0x80004005.toInt()).value
            }
        }

        @JvmStatic
        private fun invokeNoArgUInt32(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.noArgUInt32Methods[interfaceAddress to slot]
                    ?.invoke()
                    ?: return KnownHResults.E_NOTIMPL.value
                result.reinterpret(ValueLayout.JAVA_INT.byteSize().toLong()).set(
                    ValueLayout.JAVA_INT,
                    0L,
                    value.toInt(),
                )
                HResult(0).value
            }.getOrElse {
                result.reinterpret(ValueLayout.JAVA_INT.byteSize().toLong()).set(ValueLayout.JAVA_INT, 0L, 0)
                HResult(0x80004005.toInt()).value
            }
        }

        @JvmStatic
        private fun invokeNoArgHString(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.noArgHStringMethods[interfaceAddress to slot]
                    ?.invoke()
                    ?: return KnownHResults.E_NOTIMPL.value
                val hString = JvmWinRtRuntime.createHString(value)
                writeAddress(result, ComPtr(AbiIntPtr(hString.raw)))
                HResult(0).value
            }.getOrElse {
                writeAddress(result, ComPtr.NULL)
                HResult(0x80004005.toInt()).value
            }
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

        @JvmStatic
        private fun invokeStringArgBoolean(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            arg: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.stringArgBooleanMethods[interfaceAddress to slot]
                    ?.invoke(PlatformHStringBridge.toKotlinString(HString(arg.address())))
                    ?: return KnownHResults.E_NOTIMPL.value
                result.reinterpret(ValueLayout.JAVA_INT.byteSize().toLong()).set(
                    ValueLayout.JAVA_INT,
                    0L,
                    if (value) 1 else 0,
                )
                HResult(0).value
            }.getOrElse {
                result.reinterpret(ValueLayout.JAVA_INT.byteSize().toLong()).set(ValueLayout.JAVA_INT, 0L, 0)
                HResult(0x80004005.toInt()).value
            }
        }

        @JvmStatic
        private fun invokeStringArgHString(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            arg: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.stringArgHStringMethods[interfaceAddress to slot]
                    ?.invoke(PlatformHStringBridge.toKotlinString(HString(arg.address())))
                    ?: return KnownHResults.E_NOTIMPL.value
                val hString = JvmWinRtRuntime.createHString(value)
                writeAddress(result, ComPtr(AbiIntPtr(hString.raw)))
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

        private fun allocateIidArray(count: Int): MemorySegment {
            val size = count * 16L
            return if (isWindows) {
                val allocator = requireNotNull(coTaskMemAlloc) { "CoTaskMemAlloc is unavailable on Windows" }
                val allocated = allocator.invokeWithArguments(size) as MemorySegment
                require(allocated.address() != 0L) { "CoTaskMemAlloc returned null for $size bytes" }
                allocated.reinterpret(size)
            } else {
                iidArena.allocate(size, ValueLayout.JAVA_INT.byteAlignment())
            }
        }

        private fun writeGuid(segment: MemorySegment, value: Guid) {
            val guidSegment = segment.reinterpret(16L)
            guidSegment.set(ValueLayout.JAVA_INT, 0L, value.data1)
            guidSegment.set(ValueLayout.JAVA_SHORT, 4L, value.data2)
            guidSegment.set(ValueLayout.JAVA_SHORT, 6L, value.data3)
            value.data4.forEachIndexed { index, byte ->
                guidSegment.set(ValueLayout.JAVA_BYTE, 8L + index, byte)
            }
        }
    }

    data class InterfaceSpec(
        val iid: Guid,
        val noArgUnitMethods: Map<Int, () -> HResult> = emptyMap(),
        val noArgObjectMethods: Map<Int, () -> ComPtr> = emptyMap(),
        val noArgBooleanMethods: Map<Int, () -> Boolean> = emptyMap(),
        val noArgUInt32Methods: Map<Int, () -> UInt> = emptyMap(),
        val noArgHStringMethods: Map<Int, () -> String> = emptyMap(),
        val objectArgUnitMethods: Map<Int, (ComPtr) -> HResult> = emptyMap(),
        val objectArgObjectMethods: Map<Int, (ComPtr) -> ComPtr> = emptyMap(),
        val stringArgObjectMethods: Map<Int, (String) -> ComPtr> = emptyMap(),
        val stringArgBooleanMethods: Map<Int, (String) -> Boolean> = emptyMap(),
        val stringArgHStringMethods: Map<Int, (String) -> String> = emptyMap(),
    )

    private class SharedState(
        val runtimeClassName: String?,
    ) {
        val refCount = AtomicInteger(1)
        val interfacesByIid = linkedMapOf<String, ComPtr>()
        val noArgUnitMethods = mutableMapOf<Pair<Long, Int>, () -> HResult>()
        val noArgObjectMethods = mutableMapOf<Pair<Long, Int>, () -> ComPtr>()
        val noArgBooleanMethods = mutableMapOf<Pair<Long, Int>, () -> Boolean>()
        val noArgUInt32Methods = mutableMapOf<Pair<Long, Int>, () -> UInt>()
        val noArgHStringMethods = mutableMapOf<Pair<Long, Int>, () -> String>()
        val objectArgUnitMethods = mutableMapOf<Pair<Long, Int>, (ComPtr) -> HResult>()
        val objectArgObjectMethods = mutableMapOf<Pair<Long, Int>, (ComPtr) -> ComPtr>()
        val stringArgObjectMethods = mutableMapOf<Pair<Long, Int>, (String) -> ComPtr>()
        val stringArgBooleanMethods = mutableMapOf<Pair<Long, Int>, (String) -> Boolean>()
        val stringArgHStringMethods = mutableMapOf<Pair<Long, Int>, (String) -> String>()
        val queryInterfaceFallback = AtomicReference<((Guid) -> ComPtr?)?>(null)

        fun ownsPointer(pointer: ComPtr): Boolean {
            return interfacesByIid.values.any { it == pointer } || pointer.value.rawValue in interfacesByIid.values.map { it.value.rawValue }
        }
    }
}

private val Guid.canonical: String
    get() = toString()
