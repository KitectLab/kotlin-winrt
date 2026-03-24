package dev.winrt.kom

expect object PlatformRuntime {
    val platformName: String
    val isWindows: Boolean
    val ffiBackend: String
}
