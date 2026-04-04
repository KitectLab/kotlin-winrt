package dev.winrt.sample.jvm

fun main() {
    SampleBootstrap.configure()
    SampleBootstrap.diagnostics()?.let(::println)
    val result = SampleBootstrap.launch()
    println(result.diagnostics)
    result.winRtSmoke?.let { smoke ->
        println("WinRT smoke: ${smoke.runtimeClass} parsed name=${smoke.parsedName}")
    }
    println(result.launcherSummary)
    if (dev.winrt.kom.PlatformRuntime.isWindows) {
        Runtime.getRuntime().halt(0)
    }
}
