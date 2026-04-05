package dev.winrt.kom

actual fun invokeBooleanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<BooleanArray> {
    return Result.failure(
        UnsupportedOperationException("Native Boolean[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeBooleanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<BooleanArray> {
    return Result.failure(
        UnsupportedOperationException("Native Boolean[] receive-array invocation with inputs is not wired yet"),
    )
}
