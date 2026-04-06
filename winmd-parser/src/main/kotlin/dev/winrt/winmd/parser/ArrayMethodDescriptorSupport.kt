package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal typealias ArrayArgumentLowerer = (WinMdParameter) -> CodeBlock?
internal typealias ArrayAbiArgumentsBuilder = (List<WinMdParameter>, ArrayArgumentLowerer) -> List<CodeBlock>?
internal typealias ReceiveArrayReturnExpressionBuilder = (Int, List<CodeBlock>) -> CodeBlock

internal data class ReceiveArrayMethodDescriptor(
    val label: String,
    val matches: (WinMdMethod) -> Boolean,
    val abiArguments: ArrayAbiArgumentsBuilder,
    val returnExpression: ReceiveArrayReturnExpressionBuilder,
)

internal data class PassArrayMethodDescriptor(
    val label: String,
    val matches: (WinMdMethod, (String) -> Boolean) -> Boolean,
    val abiArguments: ArrayAbiArgumentsBuilder,
)

internal val standardReceiveArrayMethodDescriptors = listOf(
    ReceiveArrayMethodDescriptor("Int32", WinMdMethod::isInt32ReceiveArrayReturnMethod, ::int32ReceiveArrayAbiArguments, ::int32ReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("UInt8", WinMdMethod::isUInt8ReceiveArrayReturnMethod, ::uint8ReceiveArrayAbiArguments, ::uint8ReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("Int16", WinMdMethod::isInt16ReceiveArrayReturnMethod, ::int16ReceiveArrayAbiArguments, ::int16ReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("UInt16", WinMdMethod::isUInt16ReceiveArrayReturnMethod, ::uint16ReceiveArrayAbiArguments, ::uint16ReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("Char16", WinMdMethod::isChar16ReceiveArrayReturnMethod, ::char16ReceiveArrayAbiArguments, ::char16ReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("Boolean", WinMdMethod::isBooleanReceiveArrayReturnMethod, ::booleanReceiveArrayAbiArguments, ::booleanReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("Guid", WinMdMethod::isGuidReceiveArrayReturnMethod, ::guidReceiveArrayAbiArguments, ::guidReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("Object", WinMdMethod::isObjectReceiveArrayReturnMethod, ::objectReceiveArrayAbiArguments, ::objectReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("DateTime", WinMdMethod::isDateTimeReceiveArrayReturnMethod, ::dateTimeReceiveArrayAbiArguments, ::dateTimeReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("TimeSpan", WinMdMethod::isTimeSpanReceiveArrayReturnMethod, ::timeSpanReceiveArrayAbiArguments, ::timeSpanReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("UInt32", WinMdMethod::isUInt32ReceiveArrayReturnMethod, ::uint32ReceiveArrayAbiArguments, ::uint32ReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("Int64", WinMdMethod::isInt64ReceiveArrayReturnMethod, ::int64ReceiveArrayAbiArguments, ::int64ReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("UInt64", WinMdMethod::isUInt64ReceiveArrayReturnMethod, ::uint64ReceiveArrayAbiArguments, ::uint64ReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("Float32", WinMdMethod::isFloat32ReceiveArrayReturnMethod, ::float32ReceiveArrayAbiArguments, ::float32ReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("Float64", WinMdMethod::isFloat64ReceiveArrayReturnMethod, ::float64ReceiveArrayAbiArguments, ::float64ReceiveArrayReturnExpression),
    ReceiveArrayMethodDescriptor("String", WinMdMethod::isStringReceiveArrayReturnMethod, ::stringReceiveArrayAbiArguments, ::stringReceiveArrayReturnExpression),
)

internal val standardPassArrayMethodDescriptors = listOf(
    PassArrayMethodDescriptor("Int32", { method, supportsObjectReturn -> method.isInt32PassArrayMethod(supportsObjectReturn) }, ::int32PassArrayAbiArguments),
    PassArrayMethodDescriptor("String", { method, supportsObjectReturn -> method.isStringPassArrayMethod(supportsObjectReturn) }, ::stringPassArrayAbiArguments),
    PassArrayMethodDescriptor("Guid", { method, supportsObjectReturn -> method.isGuidPassArrayMethod(supportsObjectReturn) }, ::guidPassArrayAbiArguments),
    PassArrayMethodDescriptor("DateTime", { method, supportsObjectReturn -> method.isDateTimePassArrayMethod(supportsObjectReturn) }, ::dateTimePassArrayAbiArguments),
    PassArrayMethodDescriptor("UInt32", { method, supportsObjectReturn -> method.isUInt32PassArrayMethod(supportsObjectReturn) }, ::uint32PassArrayAbiArguments),
    PassArrayMethodDescriptor("Object", { method, supportsObjectReturn -> method.isObjectPassArrayMethod(supportsObjectReturn) }, ::objectPassArrayAbiArguments),
    PassArrayMethodDescriptor("Boolean", { method, supportsObjectReturn -> method.isBooleanPassArrayMethod(supportsObjectReturn) }, ::booleanPassArrayAbiArguments),
    PassArrayMethodDescriptor("UInt8", { method, supportsObjectReturn -> method.isUInt8PassArrayMethod(supportsObjectReturn) }, ::uint8PassArrayAbiArguments),
    PassArrayMethodDescriptor("Int16", { method, supportsObjectReturn -> method.isInt16PassArrayMethod(supportsObjectReturn) }, ::int16PassArrayAbiArguments),
    PassArrayMethodDescriptor("UInt16", { method, supportsObjectReturn -> method.isUInt16PassArrayMethod(supportsObjectReturn) }, ::uint16PassArrayAbiArguments),
    PassArrayMethodDescriptor("Char16", { method, supportsObjectReturn -> method.isChar16PassArrayMethod(supportsObjectReturn) }, ::char16PassArrayAbiArguments),
    PassArrayMethodDescriptor("Float32", { method, supportsObjectReturn -> method.isFloat32PassArrayMethod(supportsObjectReturn) }, ::float32PassArrayAbiArguments),
    PassArrayMethodDescriptor("Float64", { method, supportsObjectReturn -> method.isFloat64PassArrayMethod(supportsObjectReturn) }, ::float64PassArrayAbiArguments),
    PassArrayMethodDescriptor("Int64", { method, supportsObjectReturn -> method.isInt64PassArrayMethod(supportsObjectReturn) }, ::int64PassArrayAbiArguments),
    PassArrayMethodDescriptor("UInt64", { method, supportsObjectReturn -> method.isUInt64PassArrayMethod(supportsObjectReturn) }, ::uint64PassArrayAbiArguments),
    PassArrayMethodDescriptor("TimeSpan", { method, supportsObjectReturn -> method.isTimeSpanPassArrayMethod(supportsObjectReturn) }, ::timeSpanPassArrayAbiArguments),
)
