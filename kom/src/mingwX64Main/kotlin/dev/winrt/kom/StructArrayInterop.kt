package dev.winrt.kom

actual fun invokeStructReceiveArrayMethod(
    instance: ComPtr,
    vtableIndex: Int,
    layout: ComStructLayout,
): Result<Array<ComStructValue>> = unsupportedReceiveArray("struct")

actual fun invokeStructReceiveArrayMethod(
    instance: ComPtr,
    vtableIndex: Int,
    layout: ComStructLayout,
    vararg arguments: Any,
): Result<Array<ComStructValue>> = unsupportedReceiveArrayWithInputs("struct")
