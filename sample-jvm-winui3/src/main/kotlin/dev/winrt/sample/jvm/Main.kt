package dev.winrt.sample.jvm

fun main() {
    SampleBootstrap.configure()
    try {
        val result = SampleBootstrap.launch()
        println(result.diagnostics)
        result.winRtSmoke?.let { smoke ->
            println("WinRT smoke: ${smoke.runtimeClass} parsed name=${smoke.parsedName}")
        }
        println(result.launcherSummary)
    } finally {
        SampleBootstrap.shutdown()
    }
}
