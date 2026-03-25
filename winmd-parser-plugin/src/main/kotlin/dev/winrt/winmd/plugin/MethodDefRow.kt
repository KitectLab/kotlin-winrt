package dev.winrt.winmd.plugin

internal data class MethodDefRow(
    val name: String,
    val signature: ByteArray,
    val paramListIndex: Int,
)
