package dev.winrt.kom

expect fun invokeBooleanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<BooleanArray>

expect fun invokeBooleanReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<BooleanArray>
