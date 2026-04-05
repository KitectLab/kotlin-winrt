package dev.winrt.winmd.plugin

internal data class FieldRow(
    val flags: Int,
    val name: String,
    val signature: ByteArray,
)
