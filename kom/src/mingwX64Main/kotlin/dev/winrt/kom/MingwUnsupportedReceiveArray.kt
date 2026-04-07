package dev.winrt.kom

internal fun <T> unsupportedReceiveArray(typeName: String): Result<T> =
    Result.failure(
        UnsupportedOperationException("Native $typeName receive-array invocation is not wired yet"),
    )

internal fun <T> unsupportedReceiveArrayWithInputs(typeName: String): Result<T> =
    Result.failure(
        UnsupportedOperationException("Native $typeName receive-array invocation with inputs is not wired yet"),
    )
