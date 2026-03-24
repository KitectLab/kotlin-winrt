package dev.winrt.core

class RuntimeProperty<T>(
    private var currentValue: T,
) {
    fun get(): T = currentValue

    fun set(value: T) {
        currentValue = value
    }
}
