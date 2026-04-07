package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult
import dev.winrt.kom.HString
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformHStringBridge
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

internal object JvmWinRtUnitResultDelegates : JvmWinRtDelegateSupport() {
    private val lookup = MethodHandles.lookup()

    fun createDelegate(
        iid: Guid,
        parameterKinds: List<WinRtDelegateValueKind>,
        invoke: (Array<Any?>) -> Unit,
    ): WinRtDelegateHandle {
        val invoker = object : UnitInvoker {
            override fun invoke(args: Array<out Any?>) {
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
                override fun invoke(args: Array<out Any?>) {
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
                override fun invoke(args: Array<out Any?>) {
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
                override fun invoke(args: Array<out Any?>) {
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
                override fun invoke(args: Array<out Any?>) {
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
                override fun invoke(args: Array<out Any?>) {
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
                override fun invoke(args: Array<out Any?>) {
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
        return MethodType.methodType(
            Int::class.javaPrimitiveType,
            MemorySegment::class.java,
            *JvmWinRtDelegateArgumentSupport.abiParameterTypes(parameterKinds),
        )
    }

    private fun unitDescriptor(parameterKinds: List<WinRtDelegateValueKind>): FunctionDescriptor {
        val layouts = JvmWinRtDelegateArgumentSupport.abiParameterLayouts(parameterKinds)
        return FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, *layouts)
    }

    private fun decodeArguments(parameterKinds: List<WinRtDelegateValueKind>, args: Array<out Any?>): Array<Any?> =
        JvmWinRtDelegateArgumentSupport.decodeArguments(parameterKinds, args)

    private fun createDelegate(
        iid: Guid,
        methodType: MethodType,
        descriptor: FunctionDescriptor,
        invoker: UnitInvoker,
    ): WinRtDelegateHandle {
        val invokeMethod = lookup.findVirtual(
            UnitInvoker::class.java,
            "invokeRaw",
            unitGenericMethodType(methodType.parameterCount() - 1),
        ).bindTo(invoker)
            .asType(methodType)
        val invokeStub = linker.upcallStub(invokeMethod, descriptor, libraryArena)
        return createHandle(iid, invokeStub)
    }

    private fun isLivePointer(thisPointer: MemorySegment): Boolean = hasState(thisPointer)

    private interface UnitInvoker {
        fun invoke(args: Array<out Any?>)

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
            if (!JvmWinRtUnitResultDelegates.isLivePointer(thisPointer)) {
                return KnownHResults.E_POINTER.value
            }
            invoke(args)
            return HResult(0).value
        }
    }
}
