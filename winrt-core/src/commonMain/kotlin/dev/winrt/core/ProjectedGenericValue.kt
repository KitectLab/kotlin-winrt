package dev.winrt.core

import dev.winrt.kom.ComMethodResult
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireBoolean
import dev.winrt.kom.requireFloat32
import dev.winrt.kom.requireFloat64
import dev.winrt.kom.requireHString
import dev.winrt.kom.requireInt32
import dev.winrt.kom.requireInt64
import dev.winrt.kom.requireObject
import dev.winrt.kom.requireUInt32
import dev.winrt.kom.requireUInt64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

private const val WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET = 116444736000000000L

fun projectedGenericArgument(
    value: Any,
    signature: String,
    projectionTypeKey: String,
): Any {
    return when {
        signature == WinRtTypeSignature.string() -> value as String
        signature == "b1" || projectionTypeKey == "Boolean" -> value as Boolean
        signature == "i4" || projectionTypeKey == "Int32" -> value as Int
        signature == "u4" || projectionTypeKey == "UInt32" -> value as UInt
        signature == "i8" || projectionTypeKey == "Int64" -> value as Long
        signature == "u8" || projectionTypeKey == "UInt64" -> value as ULong
        signature == "f4" || projectionTypeKey == "Float32" -> value as Float
        signature == "f8" || projectionTypeKey == "Float64" -> value as Double
        signature == "struct(Windows.Foundation.DateTime;i8)" || projectionTypeKey == "DateTime" ->
            instantToUniversalTime(value as Instant)
        signature == "struct(Windows.Foundation.TimeSpan;i8)" || projectionTypeKey == "TimeSpan" ->
            durationToTimeSpanTicks(value as Duration)
        supportsProjectedGenericObject(signature) -> projectedObjectArgumentPointer(value, projectionTypeKey, signature)
        else -> error("Unsupported generic projection argument for signature=$signature projectionTypeKey=$projectionTypeKey")
    }
}

fun projectedGenericMethodResult(
    pointer: ComPtr,
    vtableIndex: Int,
    signature: String,
    projectionTypeKey: String,
    vararg arguments: Any,
): Any {
    val result = PlatformComInterop.invokeMethodWithResultKind(
        pointer,
        vtableIndex,
        projectedGenericResultKind(signature, projectionTypeKey),
        *arguments,
    ).getOrThrow()
    return projectedGenericResultValue(result, signature, projectionTypeKey)
}

private fun projectedGenericResultKind(
    signature: String,
    projectionTypeKey: String,
): ComMethodResultKind {
    return when {
        signature == WinRtTypeSignature.string() -> ComMethodResultKind.HSTRING
        signature == "b1" || projectionTypeKey == "Boolean" -> ComMethodResultKind.BOOLEAN
        signature == "i4" || projectionTypeKey == "Int32" -> ComMethodResultKind.INT32
        signature == "u4" || projectionTypeKey == "UInt32" -> ComMethodResultKind.UINT32
        signature == "i8" || projectionTypeKey == "Int64" -> ComMethodResultKind.INT64
        signature == "u8" || projectionTypeKey == "UInt64" -> ComMethodResultKind.UINT64
        signature == "f4" || projectionTypeKey == "Float32" -> ComMethodResultKind.FLOAT32
        signature == "f8" || projectionTypeKey == "Float64" -> ComMethodResultKind.FLOAT64
        signature == "struct(Windows.Foundation.DateTime;i8)" || projectionTypeKey == "DateTime" ->
            ComMethodResultKind.INT64
        signature == "struct(Windows.Foundation.TimeSpan;i8)" || projectionTypeKey == "TimeSpan" ->
            ComMethodResultKind.INT64
        supportsProjectedGenericObject(signature) -> ComMethodResultKind.OBJECT
        else -> error("Unsupported generic projection result for signature=$signature projectionTypeKey=$projectionTypeKey")
    }
}

private fun projectedGenericResultValue(
    result: ComMethodResult,
    signature: String,
    projectionTypeKey: String,
): Any {
    return when {
        signature == WinRtTypeSignature.string() ->
            result.requireHString().use { value -> value.toKotlinString() }
        signature == "b1" || projectionTypeKey == "Boolean" -> result.requireBoolean()
        signature == "i4" || projectionTypeKey == "Int32" -> result.requireInt32()
        signature == "u4" || projectionTypeKey == "UInt32" -> result.requireUInt32()
        signature == "i8" || projectionTypeKey == "Int64" -> result.requireInt64()
        signature == "u8" || projectionTypeKey == "UInt64" -> result.requireUInt64()
        signature == "f4" || projectionTypeKey == "Float32" -> result.requireFloat32()
        signature == "f8" || projectionTypeKey == "Float64" -> result.requireFloat64()
        signature == "struct(Windows.Foundation.DateTime;i8)" || projectionTypeKey == "DateTime" ->
            universalTimeToInstant(result.requireInt64())
        signature == "struct(Windows.Foundation.TimeSpan;i8)" || projectionTypeKey == "TimeSpan" ->
            (result.requireInt64() * 100).nanoseconds
        supportsProjectedGenericObject(signature) -> {
            val inspectable = Inspectable(result.requireObject())
            WinRtProjectionFactoryRegistry.create(inspectable, projectionTypeKey, signature) ?: inspectable
        }
        else -> error("Unsupported generic projection result for signature=$signature projectionTypeKey=$projectionTypeKey")
    }
}

private fun supportsProjectedGenericObject(signature: String): Boolean {
    return signature == WinRtTypeSignature.object_() ||
        (signature.startsWith("{") && signature.endsWith("}")) ||
        (signature.startsWith("delegate(") && signature.endsWith(")")) ||
        (signature.startsWith("pinterface(") && signature.endsWith(")")) ||
        (signature.startsWith("rc(") && signature.endsWith(")"))
}

private fun universalTimeToInstant(ticks: Long): Instant {
    val unixTicks = ticks - WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET
    return Instant.fromEpochSeconds(
        unixTicks / 10000000L,
        ((unixTicks % 10000000L) * 100).toInt(),
    )
}

private fun instantToUniversalTime(value: Instant): Long {
    return WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET +
        (value.epochSeconds * 10000000L) +
        (value.nanosecondsOfSecond / 100)
}

private fun durationToTimeSpanTicks(value: Duration): Long {
    return value.inWholeNanoseconds / 100
}
