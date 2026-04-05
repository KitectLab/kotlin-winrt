package dev.winrt.winmd.plugin

internal data class ConstantRow(
    val type: Int,
    val parentCodedIndex: Int,
    val value: ByteArray,
)
