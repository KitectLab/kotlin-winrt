package dev.winrt.kom

actual fun invokeChar16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<CharArray> {
    return Result.failure(
        UnsupportedOperationException("Native Char16[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeChar16ReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<CharArray> {
    return Result.failure(
        UnsupportedOperationException("Native Char16[] receive-array invocation with inputs is not wired yet"),
    )
}
