package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult
import dev.winrt.kom.HString
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformHStringBridge
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal object JvmWinRtUnitResultDelegates {
    fun createDelegate(
        iid: Guid,
        parameterKinds: List<WinRtDelegateValueKind>,
        invoke: (Array<Any?>) -> Unit,
    ): WinRtDelegateHandle {
        val invoker = object : UnitInvoker {
            override fun invoke(state: State, args: Array<out Any?>) {
                invoke(decodeArguments(parameterKinds, args))
            }
        }
        return createDelegate(
            iid = iid,
            methodType = unitAbiMethodType(parameterKinds),
            descriptor = unitDescriptor(parameterKinds),
            invoker = invoker,
        )
    }

    fun createNoArg(iid: Guid, invoke: () -> Unit): WinRtDelegateHandle {
        val callback = invoke
        return createDelegate(
            iid = iid,
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java),
            descriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS),
            invoker = object : UnitInvoker {
                override fun invoke(state: State, args: Array<out Any?>) {
                    callback()
                }
            },
        )
    }

    fun <T> createInt32Arg(iid: Guid, decode: (Int) -> T, invoke: (T) -> Unit): WinRtDelegateHandle {
        return createDelegate(
            iid = iid,
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, Int::class.javaObjectType),
            descriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
            invoker = object : UnitInvoker {
                override fun invoke(state: State, args: Array<out Any?>) {
                    invoke(decode(args[0] as Int))
                }
            },
        )
    }

    fun <T> createInt64Arg(iid: Guid, decode: (Long) -> T, invoke: (T) -> Unit): WinRtDelegateHandle {
        return createDelegate(
            iid = iid,
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, Long::class.javaObjectType),
            descriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
            invoker = object : UnitInvoker {
                override fun invoke(state: State, args: Array<out Any?>) {
                    invoke(decode(args[0] as Long))
                }
            },
        )
    }

    fun <T> createFloat32Arg(iid: Guid, decode: (Float) -> T, invoke: (T) -> Unit): WinRtDelegateHandle {
        return createDelegate(
            iid = iid,
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, Float::class.javaObjectType),
            descriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT),
            invoker = object : UnitInvoker {
                override fun invoke(state: State, args: Array<out Any?>) {
                    invoke(decode(args[0] as Float))
                }
            },
        )
    }

    fun <T> createFloat64Arg(iid: Guid, decode: (Double) -> T, invoke: (T) -> Unit): WinRtDelegateHandle {
        return createDelegate(
            iid = iid,
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, Double::class.javaObjectType),
            descriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE),
            invoker = object : UnitInvoker {
                override fun invoke(state: State, args: Array<out Any?>) {
                    invoke(decode(args[0] as Double))
                }
            },
        )
    }

    fun <T> createAddressArg(iid: Guid, decode: (MemorySegment) -> T, invoke: (T) -> Unit): WinRtDelegateHandle {
        return createDelegate(
            iid = iid,
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, MemorySegment::class.java),
            descriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
            invoker = object : UnitInvoker {
                override fun invoke(state: State, args: Array<out Any?>) {
                    invoke(decode(args[0] as MemorySegment))
                }
            },
        )
    }

    fun createStringArg(iid: Guid, invoke: (String) -> Unit): WinRtDelegateHandle {
        return createAddressArg(iid, decode = { arg -> PlatformHStringBridge.toKotlinString(HString(arg.address())) }, invoke = invoke)
    }

    fun createObjectArg(iid: Guid, invoke: (ComPtr) -> Unit): WinRtDelegateHandle {
        return createAddressArg(iid, decode = { arg -> ComPtr(AbiIntPtr(arg.address())) }, invoke = invoke)
    }

    private fun unitGenericMethodType(arity: Int): MethodType {
        val parameterTypes = Array(arity) { Any::class.java }
        return MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, *parameterTypes)
    }

    private fun unitAbiMethodType(parameterKinds: List<WinRtDelegateValueKind>): MethodType {
        val parameterTypes = parameterKinds.map { kind ->
            when (kind) {
                WinRtDelegateValueKind.OBJECT,
                WinRtDelegateValueKind.STRING,
                -> MemorySegment::class.java
                WinRtDelegateValueKind.INT32,
                WinRtDelegateValueKind.UINT32,
                WinRtDelegateValueKind.BOOLEAN,
                -> Int::class.javaPrimitiveType
                WinRtDelegateValueKind.INT64,
                WinRtDelegateValueKind.UINT64,
                -> Long::class.javaPrimitiveType
                WinRtDelegateValueKind.FLOAT32 -> Float::class.javaPrimitiveType
                WinRtDelegateValueKind.FLOAT64 -> Double::class.javaPrimitiveType
            }
        }
        return MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, *parameterTypes.toTypedArray())
    }

    private fun unitDescriptor(parameterKinds: List<WinRtDelegateValueKind>): FunctionDescriptor {
        val layouts = parameterKinds.map { layoutFor(it) }.toTypedArray()
        return FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, *layouts)
    }

    private fun decodeArguments(parameterKinds: List<WinRtDelegateValueKind>, args: Array<out Any?>): Array<Any?> {
        return parameterKinds.mapIndexed { index, kind -> decodeArgument(kind, args[index]) }.toTypedArray()
    }

    private fun layoutFor(kind: WinRtDelegateValueKind): ValueLayout {
        return when (kind) {
            WinRtDelegateValueKind.OBJECT,
            WinRtDelegateValueKind.STRING,
            -> ValueLayout.ADDRESS
            WinRtDelegateValueKind.INT32,
            WinRtDelegateValueKind.UINT32,
            WinRtDelegateValueKind.BOOLEAN,
            -> ValueLayout.JAVA_INT
            WinRtDelegateValueKind.INT64,
            WinRtDelegateValueKind.UINT64,
            -> ValueLayout.JAVA_LONG
            WinRtDelegateValueKind.FLOAT32 -> ValueLayout.JAVA_FLOAT
            WinRtDelegateValueKind.FLOAT64 -> ValueLayout.JAVA_DOUBLE
        }
    }

    private fun decodeArgument(kind: WinRtDelegateValueKind, raw: Any?): Any? {
        return when (kind) {
            WinRtDelegateValueKind.OBJECT -> ComPtr(AbiIntPtr((raw as MemorySegment).address()))
            WinRtDelegateValueKind.STRING -> PlatformHStringBridge.toKotlinString(HString((raw as MemorySegment).address()))
            WinRtDelegateValueKind.INT32 -> when (raw) {
                is Int -> raw
                is Number -> raw.toInt()
                else -> error("Expected Int32 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
            }
            WinRtDelegateValueKind.UINT32 -> when (raw) {
                is Int -> raw.toUInt()
                is Number -> raw.toInt().toUInt()
                else -> error("Expected UInt32 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
            }
            WinRtDelegateValueKind.BOOLEAN -> when (raw) {
                is Int -> raw != 0
                is Boolean -> raw
                is Number -> raw.toInt() != 0
                else -> error("Expected Boolean delegate argument, got ${raw?.javaClass?.name ?: "null"}")
            }
            WinRtDelegateValueKind.INT64 -> when (raw) {
                is Long -> raw
                is Number -> raw.toLong()
                else -> error("Expected Int64 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
            }
            WinRtDelegateValueKind.UINT64 -> when (raw) {
                is Long -> raw.toULong()
                is Number -> raw.toLong().toULong()
                else -> error("Expected UInt64 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
            }
            WinRtDelegateValueKind.FLOAT32 -> when (raw) {
                is Float -> raw
                is Number -> raw.toFloat()
                else -> error("Expected Float32 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
            }
            WinRtDelegateValueKind.FLOAT64 -> when (raw) {
                is Double -> raw
                is Number -> raw.toDouble()
                else -> error("Expected Float64 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
            }
        }
    }

    private fun createDelegate(
        iid: Guid,
        methodType: MethodType,
        descriptor: FunctionDescriptor,
        invoker: UnitInvoker,
    ): WinRtDelegateHandle {
        val arena = Arena.ofShared()
        val vtable = arena.allocate(ValueLayout.ADDRESS.byteSize() * 4, ValueLayout.ADDRESS.byteAlignment())
        val instance = arena.allocate(ValueLayout.ADDRESS)
        val state = State(iid, invoker)
        states[instance.address()] = state
        val invokeMethod = lookup.findVirtual(UnitInvoker::class.java, "invokeRaw", unitGenericMethodType(methodType.parameterCount() - 1))
            .bindTo(invoker)
            .asType(methodType)
        val invokeStub = linker.upcallStub(
            invokeMethod,
            descriptor,
            libraryArena,
        )
        vtable.setAtIndex(ValueLayout.ADDRESS, 0, queryInterfaceStub)
        vtable.setAtIndex(ValueLayout.ADDRESS, 1, addRefStub)
        vtable.setAtIndex(ValueLayout.ADDRESS, 2, releaseStub)
        vtable.setAtIndex(ValueLayout.ADDRESS, 3, invokeStub)
        instance.set(ValueLayout.ADDRESS, 0, vtable)
        return object : WinRtDelegateHandle {
            override val pointer: ComPtr = ComPtr(AbiIntPtr(instance.address()))

            override fun close() {
                states.remove(instance.address())
                arena.close()
            }
        }
    }

    private val iUnknownIid = guidOf("00000000-0000-0000-c000-000000000046")
    private val iAgileObjectIid = guidOf("94ea2b94-e9cc-49e0-c0ff-ee64ca8f5b90")
    private val linker: Linker = Linker.nativeLinker()
    private val libraryArena: Arena = Arena.ofAuto()
    private val lookup = MethodHandles.lookup()
    private val states = ConcurrentHashMap<Long, State>()

    private val queryInterfaceStub = linker.upcallStub(
        lookup.findStatic(
            JvmWinRtUnitResultDelegates::class.java,
            "queryInterface",
            MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, MemorySegment::class.java, MemorySegment::class.java),
        ),
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
        libraryArena,
    )

    private val addRefStub = linker.upcallStub(
        lookup.findStatic(
            JvmWinRtUnitResultDelegates::class.java,
            "addRef",
            MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java),
        ),
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS),
        libraryArena,
    )

    private val releaseStub = linker.upcallStub(
        lookup.findStatic(
            JvmWinRtUnitResultDelegates::class.java,
            "release",
            MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java),
        ),
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS),
        libraryArena,
    )

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

    private fun matchesGuid(segment: MemorySegment, expected: Guid): Boolean {
        val guidSegment = segment.reinterpret(16L)
        return guidSegment.get(ValueLayout.JAVA_INT, 0L) == expected.data1 &&
            guidSegment.get(ValueLayout.JAVA_SHORT, 4L) == expected.data2 &&
            guidSegment.get(ValueLayout.JAVA_SHORT, 6L) == expected.data3 &&
            expected.data4.indices.all { index ->
                guidSegment.get(ValueLayout.JAVA_BYTE, 8L + index) == expected.data4[index]
            }
    }

    private class State(
        val iid: Guid,
        val invoker: UnitInvoker,
    ) {
        val refCount = AtomicInteger(1)
    }

    private interface UnitInvoker {
        fun invoke(state: State, args: Array<out Any?>)

        fun invokeRaw(thisPointer: MemorySegment): Int = invokeWithResult(thisPointer)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?): Int = invokeWithResult(thisPointer, arg0)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?): Int = invokeWithResult(thisPointer, arg0, arg1)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, arg2: Any?): Int = invokeWithResult(thisPointer, arg0, arg1, arg2)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?): Int = invokeWithResult(thisPointer, arg0, arg1, arg2, arg3)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?): Int = invokeWithResult(thisPointer, arg0, arg1, arg2, arg3, arg4)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?): Int = invokeWithResult(thisPointer, arg0, arg1, arg2, arg3, arg4, arg5)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?, arg6: Any?): Int = invokeWithResult(thisPointer, arg0, arg1, arg2, arg3, arg4, arg5, arg6)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?, arg6: Any?, arg7: Any?): Int = invokeWithResult(thisPointer, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7)

        private fun invokeWithResult(thisPointer: MemorySegment, vararg args: Any?): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            invoke(state, args)
            return HResult(0).value
        }
    }
}
