package dev.winrt.core

import dev.winrt.kom.AbiIntPtr
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HString
import dev.winrt.kom.PlatformHStringBridge
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal object JvmWinRtDelegateArgumentSupport {
    fun abiParameterTypes(parameterKinds: List<WinRtDelegateValueKind>): Array<Class<*>> =
        parameterKinds.map { descriptor(it).abiParameterType }.toTypedArray()

    fun abiParameterLayouts(parameterKinds: List<WinRtDelegateValueKind>): Array<ValueLayout> =
        parameterKinds.map { descriptor(it).layout }.toTypedArray()

    fun decodeArguments(parameterKinds: List<WinRtDelegateValueKind>, args: Array<out Any?>): Array<Any?> =
        parameterKinds.mapIndexed { index, kind -> descriptor(kind).decode(args[index]) }.toTypedArray()

    private fun descriptor(kind: WinRtDelegateValueKind): DelegateArgumentDescriptor =
        when (kind) {
            WinRtDelegateValueKind.OBJECT -> DelegateArgumentDescriptor(
                abiParameterType = MemorySegment::class.java,
                layout = ValueLayout.ADDRESS,
                decode = { raw -> ComPtr(AbiIntPtr((raw as MemorySegment).address())) },
            )
            WinRtDelegateValueKind.STRING -> DelegateArgumentDescriptor(
                abiParameterType = MemorySegment::class.java,
                layout = ValueLayout.ADDRESS,
                decode = { raw -> PlatformHStringBridge.toKotlinString(HString((raw as MemorySegment).address())) },
            )
            WinRtDelegateValueKind.INT32 -> DelegateArgumentDescriptor(
                abiParameterType = Int::class.javaPrimitiveType!!,
                layout = ValueLayout.JAVA_INT,
                decode = { raw ->
                    when (raw) {
                        is Int -> raw
                        is Number -> raw.toInt()
                        else -> error("Expected Int32 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
                    }
                },
            )
            WinRtDelegateValueKind.UINT32 -> DelegateArgumentDescriptor(
                abiParameterType = Int::class.javaPrimitiveType!!,
                layout = ValueLayout.JAVA_INT,
                decode = { raw ->
                    when (raw) {
                        is Int -> raw.toUInt()
                        is Number -> raw.toInt().toUInt()
                        else -> error("Expected UInt32 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
                    }
                },
            )
            WinRtDelegateValueKind.BOOLEAN -> DelegateArgumentDescriptor(
                abiParameterType = Int::class.javaPrimitiveType!!,
                layout = ValueLayout.JAVA_INT,
                decode = { raw ->
                    when (raw) {
                        is Int -> raw != 0
                        is Boolean -> raw
                        is Number -> raw.toInt() != 0
                        else -> error("Expected Boolean delegate argument, got ${raw?.javaClass?.name ?: "null"}")
                    }
                },
            )
            WinRtDelegateValueKind.INT64 -> DelegateArgumentDescriptor(
                abiParameterType = Long::class.javaPrimitiveType!!,
                layout = ValueLayout.JAVA_LONG,
                decode = { raw ->
                    when (raw) {
                        is Long -> raw
                        is Number -> raw.toLong()
                        else -> error("Expected Int64 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
                    }
                },
            )
            WinRtDelegateValueKind.UINT64 -> DelegateArgumentDescriptor(
                abiParameterType = Long::class.javaPrimitiveType!!,
                layout = ValueLayout.JAVA_LONG,
                decode = { raw ->
                    when (raw) {
                        is Long -> raw.toULong()
                        is Number -> raw.toLong().toULong()
                        else -> error("Expected UInt64 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
                    }
                },
            )
            WinRtDelegateValueKind.FLOAT32 -> DelegateArgumentDescriptor(
                abiParameterType = Float::class.javaPrimitiveType!!,
                layout = ValueLayout.JAVA_FLOAT,
                decode = { raw ->
                    when (raw) {
                        is Float -> raw
                        is Number -> raw.toFloat()
                        else -> error("Expected Float32 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
                    }
                },
            )
            WinRtDelegateValueKind.FLOAT64 -> DelegateArgumentDescriptor(
                abiParameterType = Double::class.javaPrimitiveType!!,
                layout = ValueLayout.JAVA_DOUBLE,
                decode = { raw ->
                    when (raw) {
                        is Double -> raw
                        is Number -> raw.toDouble()
                        else -> error("Expected Float64 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
                    }
                },
            )
        }

    private data class DelegateArgumentDescriptor(
        val abiParameterType: Class<*>,
        val layout: ValueLayout,
        val decode: (Any?) -> Any?,
    )
}
