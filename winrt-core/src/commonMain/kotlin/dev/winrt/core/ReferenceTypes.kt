package dev.winrt.core

@JvmInline
value class EventRegistrationToken(val value: Long)

class IReference<T>(
    val value: T,
)
