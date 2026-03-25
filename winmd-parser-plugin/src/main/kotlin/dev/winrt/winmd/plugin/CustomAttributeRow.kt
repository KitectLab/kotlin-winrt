package dev.winrt.winmd.plugin

internal data class CustomAttributeRow(
    val parentCodedIndex: Int,
    val typeCodedIndex: Int,
    val value: ByteArray,
)
