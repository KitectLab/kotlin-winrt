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
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, Int::class.javaPrimitiveType),
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
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, Long::class.javaPrimitiveType),
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
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, Float::class.javaPrimitiveType),
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
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, Double::class.javaPrimitiveType),
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
        val invokeStub = linker.upcallStub(
            lookup.findVirtual(UnitInvoker::class.java, "invokeRaw", methodType).bindTo(invoker),
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

        fun invokeRaw(thisPointer: MemorySegment): Int {
            return invokeWithResult(thisPointer)
        }

        fun invokeRaw(thisPointer: MemorySegment, arg0: Int): Int {
            return invokeWithResult(thisPointer, arg0)
        }

        fun invokeRaw(thisPointer: MemorySegment, arg0: Long): Int {
            return invokeWithResult(thisPointer, arg0)
        }

        fun invokeRaw(thisPointer: MemorySegment, arg0: Float): Int {
            return invokeWithResult(thisPointer, arg0)
        }

        fun invokeRaw(thisPointer: MemorySegment, arg0: Double): Int {
            return invokeWithResult(thisPointer, arg0)
        }

        fun invokeRaw(thisPointer: MemorySegment, arg0: MemorySegment): Int {
            return invokeWithResult(thisPointer, arg0)
        }

        private fun invokeWithResult(thisPointer: MemorySegment, vararg args: Any?): Int {
            val state = states[thisPointer.address()] ?: return KnownHResults.E_POINTER.value
            invoke(state, args)
            return HResult(0).value
        }
    }
}
