package dev.winrt.winmd.plugin

import java.nio.file.Path

data class ResolvedWinMdConfiguration(
    val sdkVersion: String,
    val referencesRoot: Path,
    val contracts: List<WindowsSdkContractReference>,
)
