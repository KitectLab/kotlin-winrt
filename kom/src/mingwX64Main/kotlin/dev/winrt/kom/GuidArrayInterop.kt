package dev.winrt.kom

actual fun invokeGuidReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<Array<Guid>> {
    return Result.failure(
        UnsupportedOperationException("Native Guid[] receive-array invocation is not wired yet"),
    )
}

actual fun invokeGuidReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Array<Guid>> {
    return Result.failure(
        UnsupportedOperationException("Native Guid[] receive-array invocation with inputs is not wired yet"),
    )
}
