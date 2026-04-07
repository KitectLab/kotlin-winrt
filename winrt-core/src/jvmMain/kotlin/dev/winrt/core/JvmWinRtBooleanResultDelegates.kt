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

internal object JvmWinRtBooleanResultDelegates : JvmWinRtDelegateSupport() {
    private val lookup = MethodHandles.lookup()

    fun createDelegate(
        iid: Guid,
        parameterKinds: List<WinRtDelegateValueKind>,
        invoke: (Array<Any?>) -> Boolean,
    ): WinRtDelegateHandle {
        val invoker = object : BooleanInvoker {
            override fun invoke(args: Array<out Any?>): Boolean {
                return invoke(decodeArguments(parameterKinds, args))
            }
        }
        return createDelegate(
            iid = iid,
            methodType = booleanAbiMethodType(parameterKinds),
            descriptor = booleanDescriptor(parameterKinds),
            invoker = invoker,
        )
    }

    fun createNoArg(iid: Guid, invoke: () -> Boolean): WinRtDelegateHandle {
        val callback = invoke
        return createDelegate(
            iid = iid,
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, MemorySegment::class.java),
            descriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
            invoker = object : BooleanInvoker {
                override fun invoke(args: Array<out Any?>): Boolean = callback()
            },
        )
    }

    fun <T> createInt32Arg(iid: Guid, decode: (Int) -> T, invoke: (T) -> Boolean): WinRtDelegateHandle {
        return createDelegate(
            iid = iid,
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, Int::class.javaObjectType, MemorySegment::class.java),
            descriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS),
            invoker = object : BooleanInvoker {
                override fun invoke(args: Array<out Any?>): Boolean = invoke(decode(args[0] as Int))
            },
        )
    }

    fun <T> createInt64Arg(iid: Guid, decode: (Long) -> T, invoke: (T) -> Boolean): WinRtDelegateHandle {
        return createDelegate(
            iid = iid,
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, Long::class.javaObjectType, MemorySegment::class.java),
            descriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
            invoker = object : BooleanInvoker {
                override fun invoke(args: Array<out Any?>): Boolean = invoke(decode(args[0] as Long))
            },
        )
    }

    fun <T> createFloat32Arg(iid: Guid, decode: (Float) -> T, invoke: (T) -> Boolean): WinRtDelegateHandle {
        return createDelegate(
            iid = iid,
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, Float::class.javaObjectType, MemorySegment::class.java),
            descriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT, ValueLayout.ADDRESS),
            invoker = object : BooleanInvoker {
                override fun invoke(args: Array<out Any?>): Boolean = invoke(decode(args[0] as Float))
            },
        )
    }

    fun <T> createFloat64Arg(iid: Guid, decode: (Double) -> T, invoke: (T) -> Boolean): WinRtDelegateHandle {
        return createDelegate(
            iid = iid,
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, Double::class.javaObjectType, MemorySegment::class.java),
            descriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE, ValueLayout.ADDRESS),
            invoker = object : BooleanInvoker {
                override fun invoke(args: Array<out Any?>): Boolean = invoke(decode(args[0] as Double))
            },
        )
    }

    fun <T> createAddressArg(iid: Guid, decode: (MemorySegment) -> T, invoke: (T) -> Boolean): WinRtDelegateHandle {
        return createDelegate(
            iid = iid,
            methodType = MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, MemorySegment::class.java, MemorySegment::class.java),
            descriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
            invoker = object : BooleanInvoker {
                override fun invoke(args: Array<out Any?>): Boolean = invoke(decode(args[0] as MemorySegment))
            },
        )
    }

    fun createStringArg(iid: Guid, invoke: (String) -> Boolean): WinRtDelegateHandle {
        return createAddressArg(iid, decode = { arg -> PlatformHStringBridge.toKotlinString(HString(arg.address())) }, invoke = invoke)
    }

    fun createObjectArg(iid: Guid, invoke: (ComPtr) -> Boolean): WinRtDelegateHandle {
        return createAddressArg(iid, decode = { arg -> ComPtr(AbiIntPtr(arg.address())) }, invoke = invoke)
    }

    private fun booleanGenericMethodType(arity: Int): MethodType {
        val parameterTypes = Array(arity) { Any::class.java }
        return MethodType.methodType(Int::class.javaPrimitiveType, MemorySegment::class.java, *parameterTypes, MemorySegment::class.java)
    }

    private fun booleanAbiMethodType(parameterKinds: List<WinRtDelegateValueKind>): MethodType {
        return MethodType.methodType(
            Int::class.javaPrimitiveType,
            MemorySegment::class.java,
            *JvmWinRtDelegateArgumentSupport.abiParameterTypes(parameterKinds),
            MemorySegment::class.java,
        )
    }

    private fun booleanDescriptor(parameterKinds: List<WinRtDelegateValueKind>): FunctionDescriptor {
        val layouts = JvmWinRtDelegateArgumentSupport.abiParameterLayouts(parameterKinds)
        return FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, *layouts, ValueLayout.ADDRESS)
    }

    private fun decodeArguments(parameterKinds: List<WinRtDelegateValueKind>, args: Array<out Any?>): Array<Any?> =
        JvmWinRtDelegateArgumentSupport.decodeArguments(parameterKinds, args)

    private fun createDelegate(
        iid: Guid,
        methodType: MethodType,
        descriptor: FunctionDescriptor,
        invoker: BooleanInvoker,
    ): WinRtDelegateHandle {
        val invokeMethod = lookup.findVirtual(
            BooleanInvoker::class.java,
            "invokeRaw",
            booleanGenericMethodType(methodType.parameterCount() - 2),
        ).bindTo(invoker)
            .asType(methodType)
        val invokeStub = linker.upcallStub(invokeMethod, descriptor, libraryArena)
        return createHandle(iid, invokeStub)
    }

    private fun isLivePointer(thisPointer: MemorySegment): Boolean = hasState(thisPointer)

    private interface BooleanInvoker {
        fun invoke(args: Array<out Any?>): Boolean

        fun invokeRaw(thisPointer: MemorySegment, result: MemorySegment): Int = writeBooleanResult(thisPointer, result)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, result: MemorySegment): Int = writeBooleanResult(thisPointer, result, arg0)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, result: MemorySegment): Int = writeBooleanResult(thisPointer, result, arg0, arg1)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, arg2: Any?, result: MemorySegment): Int = writeBooleanResult(thisPointer, result, arg0, arg1, arg2)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, result: MemorySegment): Int = writeBooleanResult(thisPointer, result, arg0, arg1, arg2, arg3)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, result: MemorySegment): Int = writeBooleanResult(thisPointer, result, arg0, arg1, arg2, arg3, arg4)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?, result: MemorySegment): Int = writeBooleanResult(thisPointer, result, arg0, arg1, arg2, arg3, arg4, arg5)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?, arg6: Any?, result: MemorySegment): Int = writeBooleanResult(thisPointer, result, arg0, arg1, arg2, arg3, arg4, arg5, arg6)
        fun invokeRaw(thisPointer: MemorySegment, arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?, arg6: Any?, arg7: Any?, result: MemorySegment): Int = writeBooleanResult(thisPointer, result, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7)

        private fun writeBooleanResult(thisPointer: MemorySegment, result: MemorySegment, vararg args: Any?): Int {
            if (!JvmWinRtBooleanResultDelegates.isLivePointer(thisPointer)) {
                return KnownHResults.E_POINTER.value
            }
            result.reinterpret(ValueLayout.JAVA_INT.byteSize()).set(
                ValueLayout.JAVA_INT,
                0,
                if (invoke(args)) 1 else 0,
            )
            return HResult(0).value
        }
    }
}
