package dev.winrt.winmd.plugin

internal data class SectionHeader(
    val virtualAddress: Int,
    val virtualSize: Int,
    val rawDataSize: Int,
    val rawDataPointer: Int,
)
