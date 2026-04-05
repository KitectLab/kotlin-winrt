package dev.winrt.kom

expect fun invokeStructReceiveArrayMethod(
    instance: ComPtr,
    vtableIndex: Int,
    layout: ComStructLayout,
): Result<Array<ComStructValue>>

expect fun invokeStructReceiveArrayMethod(
    instance: ComPtr,
    vtableIndex: Int,
    layout: ComStructLayout,
    vararg arguments: Any,
): Result<Array<ComStructValue>>
