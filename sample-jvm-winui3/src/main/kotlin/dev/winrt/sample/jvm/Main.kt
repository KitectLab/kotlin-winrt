package dev.winrt.sample.jvm

fun main() {
    SampleBootstrap.configure()
    try {
        val result = SampleBootstrap.launch()
        println(result.diagnostics)
        println(result.launcherSummary)
    } finally {
        SampleBootstrap.shutdown()
    }
}
