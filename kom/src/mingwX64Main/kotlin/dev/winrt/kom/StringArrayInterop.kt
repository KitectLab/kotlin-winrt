package dev.winrt.kom

actual fun invokeStringReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<Array<String>> {
    return Result.failure(
        UnsupportedOperationException("Native String[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeStringReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Array<String>> {
    return Result.failure(
        UnsupportedOperationException("Native String[] receive-array invocation with inputs is not wired yet"),
    )
}
