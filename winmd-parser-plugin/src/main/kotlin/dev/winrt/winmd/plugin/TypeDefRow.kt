package dev.winrt.winmd.plugin

internal data class TypeDefRow(
    val namespace: String,
    val name: String,
    val flags: Int,
    val extendsCodedIndex: Int,
    val fieldListIndex: Int,
    val methodListIndex: Int,
)
