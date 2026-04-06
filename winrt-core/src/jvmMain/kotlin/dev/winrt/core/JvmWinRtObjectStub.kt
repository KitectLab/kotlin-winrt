package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComStructValue
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

        private val noArgStructInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeNoArgStruct",
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

        private val noArgInt32InvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeNoArgInt32",
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

        private val noArgInt64InvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeNoArgInt64",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
            ),
        )

        private val noArgUInt64InvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeNoArgUInt64",
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

        private val uint32ArgObjectInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeUInt32ArgObject",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
            ),
        )

        private val uint32ArgInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeUInt32Arg",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                Int::class.javaPrimitiveType,
            ),
        )

        private val uint32ObjectArgInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeUInt32ObjectArg",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
            ),
        )

        private val uint32StringArgInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeUInt32StringArg",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
            ),
        )

        private val uint32ArgHStringInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeUInt32ArgHString",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
            ),
        )

        private val objectUInt32ArgBooleanInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeObjectUInt32ArgBoolean",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
            ),
        )

        private val stringUInt32ArgBooleanInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeStringUInt32ArgBoolean",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
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

        private val stringArgInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeStringArg",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
            ),
        )

        private val stringStringArgBooleanInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeStringStringArgBoolean",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
                MemorySegment::class.java,
                MemorySegment::class.java,
                MemorySegment::class.java,
            ),
        )

        private val stringObjectArgBooleanInvokeHandle = lookup.findStatic(
            JvmWinRtObjectStub::class.java,
            "invokeStringObjectArgBoolean",
            MethodType.methodType(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                MemorySegment::class.java,
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
                    spec.noArgStructMethods.keys.maxOrNull() ?: 2,
                    spec.noArgBooleanMethods.keys.maxOrNull() ?: 2,
                    spec.noArgInt32Methods.keys.maxOrNull() ?: 2,
                    spec.noArgUInt32Methods.keys.maxOrNull() ?: 2,
                    spec.noArgInt64Methods.keys.maxOrNull() ?: 2,
                    spec.noArgUInt64Methods.keys.maxOrNull() ?: 2,
                    spec.noArgHStringMethods.keys.maxOrNull() ?: 2,
                    spec.uint32ArgUnitMethods.keys.maxOrNull() ?: 2,
                    spec.uint32ArgObjectMethods.keys.maxOrNull() ?: 2,
                    spec.uint32ObjectArgUnitMethods.keys.maxOrNull() ?: 2,
                    spec.uint32StringArgUnitMethods.keys.maxOrNull() ?: 2,
                    spec.uint32ArgHStringMethods.keys.maxOrNull() ?: 2,
                    spec.objectUInt32ArgBooleanMethods.keys.maxOrNull() ?: 2,
                    spec.stringUInt32ArgBooleanMethods.keys.maxOrNull() ?: 2,
                    spec.objectArgUnitMethods.keys.maxOrNull() ?: 2,
                    spec.objectArgObjectMethods.keys.maxOrNull() ?: 2,
                    spec.stringArgUnitMethods.keys.maxOrNull() ?: 2,
                    spec.stringStringArgBooleanMethods.keys.maxOrNull() ?: 2,
                    spec.stringObjectArgBooleanMethods.keys.maxOrNull() ?: 2,
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
                spec.noArgStructMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(noArgStructInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.noArgStructMethods[interfaceAddress to slot] = method
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
                spec.noArgInt32Methods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(noArgInt32InvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.noArgInt32Methods[interfaceAddress to slot] = method
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
                spec.noArgInt64Methods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(noArgInt64InvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.noArgInt64Methods[interfaceAddress to slot] = method
                }
                spec.noArgUInt64Methods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(noArgUInt64InvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.noArgUInt64Methods[interfaceAddress to slot] = method
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
                spec.uint32ArgUnitMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(uint32ArgInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.uint32ArgUnitMethods[interfaceAddress to slot] = method
                }
                spec.uint32ArgObjectMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(uint32ArgObjectInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.uint32ArgObjectMethods[interfaceAddress to slot] = method
                }
                spec.uint32ObjectArgUnitMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(uint32ObjectArgInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.uint32ObjectArgUnitMethods[interfaceAddress to slot] = method
                }
                spec.uint32StringArgUnitMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(uint32StringArgInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.uint32StringArgUnitMethods[interfaceAddress to slot] = method
                }
                spec.uint32ArgHStringMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(uint32ArgHStringInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.uint32ArgHStringMethods[interfaceAddress to slot] = method
                }
                spec.objectUInt32ArgBooleanMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(objectUInt32ArgBooleanInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.objectUInt32ArgBooleanMethods[interfaceAddress to slot] = method
                }
                spec.stringUInt32ArgBooleanMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(stringUInt32ArgBooleanInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.stringUInt32ArgBooleanMethods[interfaceAddress to slot] = method
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
                spec.stringArgUnitMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(stringArgInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.stringArgUnitMethods[interfaceAddress to slot] = method
                }
                spec.stringStringArgBooleanMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(stringStringArgBooleanInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.stringStringArgBooleanMethods[interfaceAddress to slot] = method
                }
                spec.stringObjectArgBooleanMethods.forEach { (slot, method) ->
                    val stub = linker.upcallStub(
                        MethodHandles.insertArguments(stringObjectArgBooleanInvokeHandle, 0, interfaceAddress, slot),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                        arena,
                    )
                    vtable.setAtIndex(ValueLayout.ADDRESS, slot.toLong(), stub)
                    sharedState.stringObjectArgBooleanMethods[interfaceAddress to slot] = method
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
        private fun invokeNoArgStruct(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.noArgStructMethods[interfaceAddress to slot]
                    ?.invoke()
                    ?: return KnownHResults.E_NOTIMPL.value
                result.reinterpret(value.layout.byteSize.toLong()).copyFrom(MemorySegment.ofArray(value.bytes))
                HResult(0).value
            }.getOrElse {
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
        private fun invokeNoArgInt32(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.noArgInt32Methods[interfaceAddress to slot]
                    ?.invoke()
                    ?: return KnownHResults.E_NOTIMPL.value
                result.reinterpret(ValueLayout.JAVA_INT.byteSize().toLong()).set(
                    ValueLayout.JAVA_INT,
                    0L,
                    value,
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
        private fun invokeNoArgInt64(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.noArgInt64Methods[interfaceAddress to slot]
                    ?.invoke()
                    ?: return KnownHResults.E_NOTIMPL.value
                result.reinterpret(ValueLayout.JAVA_LONG.byteSize().toLong()).set(
                    ValueLayout.JAVA_LONG,
                    0L,
                    value,
                )
                HResult(0).value
            }.getOrElse {
                result.reinterpret(ValueLayout.JAVA_LONG.byteSize().toLong()).set(ValueLayout.JAVA_LONG, 0L, 0L)
                HResult(0x80004005.toInt()).value
            }
        }

        @JvmStatic
        private fun invokeNoArgUInt64(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.noArgUInt64Methods[interfaceAddress to slot]
                    ?.invoke()
                    ?: return KnownHResults.E_NOTIMPL.value
                result.reinterpret(ValueLayout.JAVA_LONG.byteSize().toLong()).set(
                    ValueLayout.JAVA_LONG,
                    0L,
                    value.toLong(),
                )
                HResult(0).value
            }.getOrElse {
                result.reinterpret(ValueLayout.JAVA_LONG.byteSize().toLong()).set(ValueLayout.JAVA_LONG, 0L, 0L)
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
        private fun invokeUInt32ArgObject(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            index: Int,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.uint32ArgObjectMethods[interfaceAddress to slot]
                    ?.invoke(index.toUInt())
                    ?: return KnownHResults.E_NOTIMPL.value
                writeAddress(result, value)
                HResult(0).value
            }.getOrElse {
                writeAddress(result, ComPtr.NULL)
                HResult(0x80004005.toInt()).value
            }
        }

        @JvmStatic
        private fun invokeUInt32Arg(interfaceAddress: Long, slot: Int, thisPointer: MemorySegment, index: Int): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return state.uint32ArgUnitMethods[interfaceAddress to slot]
                ?.invoke(index.toUInt())
                ?.value
                ?: KnownHResults.E_NOTIMPL.value
        }

        @JvmStatic
        private fun invokeUInt32ObjectArg(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            index: Int,
            arg: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return state.uint32ObjectArgUnitMethods[interfaceAddress to slot]
                ?.invoke(index.toUInt(), ComPtr(AbiIntPtr(arg.address())))
                ?.value
                ?: KnownHResults.E_NOTIMPL.value
        }

        @JvmStatic
        private fun invokeUInt32StringArg(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            index: Int,
            arg: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return state.uint32StringArgUnitMethods[interfaceAddress to slot]
                ?.invoke(index.toUInt(), PlatformHStringBridge.toKotlinString(HString(arg.address())))
                ?.value
                ?: KnownHResults.E_NOTIMPL.value
        }

        @JvmStatic
        private fun invokeUInt32ArgHString(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            index: Int,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.uint32ArgHStringMethods[interfaceAddress to slot]
                    ?.invoke(index.toUInt())
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
        private fun invokeObjectUInt32ArgBoolean(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            arg: MemorySegment,
            index: Int,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.objectUInt32ArgBooleanMethods[interfaceAddress to slot]
                    ?.invoke(ComPtr(AbiIntPtr(arg.address())), index.toUInt())
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
        private fun invokeStringUInt32ArgBoolean(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            arg: MemorySegment,
            index: Int,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.stringUInt32ArgBooleanMethods[interfaceAddress to slot]
                    ?.invoke(PlatformHStringBridge.toKotlinString(HString(arg.address())), index.toUInt())
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
        private fun invokeObjectArg(interfaceAddress: Long, slot: Int, thisPointer: MemorySegment, arg: MemorySegment): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return state.objectArgUnitMethods[interfaceAddress to slot]
                ?.invoke(ComPtr(AbiIntPtr(arg.address())))
                ?.value
                ?: KnownHResults.E_NOTIMPL.value
        }

        @JvmStatic
        private fun invokeStringArg(interfaceAddress: Long, slot: Int, thisPointer: MemorySegment, arg: MemorySegment): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return state.stringArgUnitMethods[interfaceAddress to slot]
                ?.invoke(PlatformHStringBridge.toKotlinString(HString(arg.address())))
                ?.value
                ?: KnownHResults.E_NOTIMPL.value
        }

        @JvmStatic
        private fun invokeStringStringArgBoolean(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            first: MemorySegment,
            second: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.stringStringArgBooleanMethods[interfaceAddress to slot]
                    ?.invoke(
                        PlatformHStringBridge.toKotlinString(HString(first.address())),
                        PlatformHStringBridge.toKotlinString(HString(second.address())),
                    )
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
        private fun invokeStringObjectArgBoolean(
            interfaceAddress: Long,
            slot: Int,
            thisPointer: MemorySegment,
            first: MemorySegment,
            second: MemorySegment,
            result: MemorySegment,
        ): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            return runCatching {
                val value = state.stringObjectArgBooleanMethods[interfaceAddress to slot]
                    ?.invoke(
                        PlatformHStringBridge.toKotlinString(HString(first.address())),
                        ComPtr(AbiIntPtr(second.address())),
                    )
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
        val noArgStructMethods: Map<Int, () -> ComStructValue> = emptyMap(),
        val noArgBooleanMethods: Map<Int, () -> Boolean> = emptyMap(),
        val noArgInt32Methods: Map<Int, () -> Int> = emptyMap(),
        val noArgUInt32Methods: Map<Int, () -> UInt> = emptyMap(),
        val noArgInt64Methods: Map<Int, () -> Long> = emptyMap(),
        val noArgUInt64Methods: Map<Int, () -> ULong> = emptyMap(),
        val noArgHStringMethods: Map<Int, () -> String> = emptyMap(),
        val uint32ArgUnitMethods: Map<Int, (UInt) -> HResult> = emptyMap(),
        val uint32ArgObjectMethods: Map<Int, (UInt) -> ComPtr> = emptyMap(),
        val uint32ObjectArgUnitMethods: Map<Int, (UInt, ComPtr) -> HResult> = emptyMap(),
        val uint32StringArgUnitMethods: Map<Int, (UInt, String) -> HResult> = emptyMap(),
        val uint32ArgHStringMethods: Map<Int, (UInt) -> String> = emptyMap(),
        val objectUInt32ArgBooleanMethods: Map<Int, (ComPtr, UInt) -> Boolean> = emptyMap(),
        val objectArgUnitMethods: Map<Int, (ComPtr) -> HResult> = emptyMap(),
        val objectArgObjectMethods: Map<Int, (ComPtr) -> ComPtr> = emptyMap(),
        val stringArgUnitMethods: Map<Int, (String) -> HResult> = emptyMap(),
        val stringUInt32ArgBooleanMethods: Map<Int, (String, UInt) -> Boolean> = emptyMap(),
        val stringStringArgBooleanMethods: Map<Int, (String, String) -> Boolean> = emptyMap(),
        val stringObjectArgBooleanMethods: Map<Int, (String, ComPtr) -> Boolean> = emptyMap(),
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
        val noArgStructMethods = mutableMapOf<Pair<Long, Int>, () -> ComStructValue>()
        val noArgBooleanMethods = mutableMapOf<Pair<Long, Int>, () -> Boolean>()
        val noArgInt32Methods = mutableMapOf<Pair<Long, Int>, () -> Int>()
        val noArgUInt32Methods = mutableMapOf<Pair<Long, Int>, () -> UInt>()
        val noArgInt64Methods = mutableMapOf<Pair<Long, Int>, () -> Long>()
        val noArgUInt64Methods = mutableMapOf<Pair<Long, Int>, () -> ULong>()
        val noArgHStringMethods = mutableMapOf<Pair<Long, Int>, () -> String>()
        val uint32ArgUnitMethods = mutableMapOf<Pair<Long, Int>, (UInt) -> HResult>()
        val uint32ArgObjectMethods = mutableMapOf<Pair<Long, Int>, (UInt) -> ComPtr>()
        val uint32ObjectArgUnitMethods = mutableMapOf<Pair<Long, Int>, (UInt, ComPtr) -> HResult>()
        val uint32StringArgUnitMethods = mutableMapOf<Pair<Long, Int>, (UInt, String) -> HResult>()
        val uint32ArgHStringMethods = mutableMapOf<Pair<Long, Int>, (UInt) -> String>()
        val objectUInt32ArgBooleanMethods = mutableMapOf<Pair<Long, Int>, (ComPtr, UInt) -> Boolean>()
        val objectArgUnitMethods = mutableMapOf<Pair<Long, Int>, (ComPtr) -> HResult>()
        val objectArgObjectMethods = mutableMapOf<Pair<Long, Int>, (ComPtr) -> ComPtr>()
        val stringArgUnitMethods = mutableMapOf<Pair<Long, Int>, (String) -> HResult>()
        val stringUInt32ArgBooleanMethods = mutableMapOf<Pair<Long, Int>, (String, UInt) -> Boolean>()
        val stringStringArgBooleanMethods = mutableMapOf<Pair<Long, Int>, (String, String) -> Boolean>()
        val stringObjectArgBooleanMethods = mutableMapOf<Pair<Long, Int>, (String, ComPtr) -> Boolean>()
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
