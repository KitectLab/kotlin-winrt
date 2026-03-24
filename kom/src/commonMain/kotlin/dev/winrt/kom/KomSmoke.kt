package dev.winrt.kom

object KomSmoke {
    fun description(): String = "kom:${PlatformRuntime.platformName}:windows=${PlatformRuntime.isWindows}:ffi=${PlatformRuntime.ffiBackend}"
}
