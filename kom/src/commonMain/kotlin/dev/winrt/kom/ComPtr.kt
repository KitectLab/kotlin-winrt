package dev.winrt.kom

import kotlin.jvm.JvmInline

@JvmInline
value class ComPtr(val value: AbiIntPtr) {
    companion object {
        val NULL = ComPtr(AbiIntPtr.NULL)
    }

    val isNull: Boolean
        get() = value.isNull
}
