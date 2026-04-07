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
        descriptors.getValue(kind)

    private val addressDescriptor = DelegateArgumentDescriptor(
        abiParameterType = MemorySegment::class.java,
        layout = ValueLayout.ADDRESS,
        decode = { raw -> raw as MemorySegment },
    )

    private val int32Descriptor = DelegateArgumentDescriptor(
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

    private val int64Descriptor = DelegateArgumentDescriptor(
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

    private val float32Descriptor = DelegateArgumentDescriptor(
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

    private val float64Descriptor = DelegateArgumentDescriptor(
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

    private val descriptors = mapOf(
        WinRtDelegateValueKind.OBJECT to addressDescriptor.copy(
            decode = { raw -> ComPtr(AbiIntPtr((raw as MemorySegment).address())) },
        ),
        WinRtDelegateValueKind.STRING to addressDescriptor.copy(
            decode = { raw -> PlatformHStringBridge.toKotlinString(HString((raw as MemorySegment).address())) },
        ),
        WinRtDelegateValueKind.INT32 to int32Descriptor,
        WinRtDelegateValueKind.UINT32 to int32Descriptor.copy(
            decode = { raw ->
                when (raw) {
                    is Int -> raw.toUInt()
                    is Number -> raw.toInt().toUInt()
                    else -> error("Expected UInt32 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
                }
            },
        ),
        WinRtDelegateValueKind.BOOLEAN to int32Descriptor.copy(
            decode = { raw ->
                when (raw) {
                    is Int -> raw != 0
                    is Boolean -> raw
                    is Number -> raw.toInt() != 0
                    else -> error("Expected Boolean delegate argument, got ${raw?.javaClass?.name ?: "null"}")
                }
            },
        ),
        WinRtDelegateValueKind.INT64 to int64Descriptor,
        WinRtDelegateValueKind.UINT64 to int64Descriptor.copy(
            decode = { raw ->
                when (raw) {
                    is Long -> raw.toULong()
                    is Number -> raw.toLong().toULong()
                    else -> error("Expected UInt64 delegate argument, got ${raw?.javaClass?.name ?: "null"}")
                }
            },
        ),
        WinRtDelegateValueKind.FLOAT32 to float32Descriptor,
        WinRtDelegateValueKind.FLOAT64 to float64Descriptor,
    )

    private data class DelegateArgumentDescriptor(
        val abiParameterType: Class<*>,
        val layout: ValueLayout,
        val decode: (Any?) -> Any?,
    )
}
