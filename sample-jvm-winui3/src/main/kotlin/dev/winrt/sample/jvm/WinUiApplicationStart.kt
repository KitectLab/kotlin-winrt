package dev.winrt.sample.jvm

import dev.winrt.core.JvmWinRtObjectArgDelegate
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.PlatformComInterop
import microsoft.ui.xaml.Application
import microsoft.ui.xaml.Window

object WinUiApplicationStart {
    private const val applicationStartSlot = 7
    private val applicationStaticsIid = guidOf("4e0d09f5-4358-512c-a987-503b52848e95")
    private val callbackIid = guidOf("d8eef1c9-1234-56f1-9963-45dd9c80a661")

    private var application: Application? = null
    private var window: Window? = null
    private var activeCallback: JvmWinRtObjectArgDelegate? = null
    @Volatile private var launchFailure: Throwable? = null
    @Volatile private var windowVisible: Boolean = false

    fun probeCallbackOnly(): Boolean {
        var callbackInvoked = false
        val activationFactory = JvmWinRtRuntime.getActivationFactory("Microsoft.UI.Xaml.Application").getOrThrow()
        val applicationStatics = PlatformComInterop.queryInterface(activationFactory, applicationStaticsIid).getOrThrow()
        try {
            val callback = JvmWinRtObjectArgDelegate.create(callbackIid) {
                callbackInvoked = true
                val uiThreadId = WindowsMessageLoop.currentThreadId()
                Thread.ofPlatform().daemon(true).start {
                    Thread.sleep(100L)
                    WindowsMessageLoop.postThreadQuit(uiThreadId)
                }
                HResult(0)
            }
            activeCallback?.close()
            activeCallback = callback
            PlatformComInterop.invokeObjectSetter(applicationStatics, applicationStartSlot, callback.pointer).getOrThrow()
            return callbackInvoked
        } finally {
            PlatformComInterop.release(applicationStatics)
            PlatformComInterop.release(activationFactory)
        }
    }

    fun shutdown() {
        activeCallback?.close()
        activeCallback = null
        window = null
        application = null
    }

    fun launchWindow(windowTitle: String, messageText: String): String {
        launchFailure = null
        windowVisible = false
        activeCallback?.close()
        activeCallback = null
        val activationFactory = JvmWinRtRuntime.getActivationFactory("Microsoft.UI.Xaml.Application").getOrThrow()
        val applicationStatics = PlatformComInterop.queryInterface(activationFactory, applicationStaticsIid).getOrThrow()
        try {
            val callback = JvmWinRtObjectArgDelegate.create(callbackIid) {
                runCatching {
                    application = Application.activate()
                    window = Window.activateInstance().also { createdWindow ->
                        createdWindow.title = windowTitle
                        createdWindow.activate()
                    }
                    val uiThreadId = WindowsMessageLoop.currentThreadId()
                    val autoQuitVisible = System.getProperty("dev.winrt.autoQuitVisible", "false").equals("true", ignoreCase = true)
                    Thread.ofPlatform().daemon(true).start {
                        val visible = WindowsWindowProbe.waitForWindowByTitle(windowTitle, timeoutMillis = 5_000L)
                        windowVisible = visible
                        if (!visible || autoQuitVisible) {
                            if (visible) {
                                Thread.sleep(500L)
                            }
                            WindowsMessageLoop.postThreadQuit(uiThreadId)
                        }
                    }
                    HResult(0)
                }.getOrElse { error ->
                    launchFailure = error
                    HResult(0x80004005.toInt())
                }
            }
            activeCallback = callback
            PlatformComInterop.invokeObjectSetter(applicationStatics, applicationStartSlot, callback.pointer).getOrThrow()
            launchFailure?.let { throw it }
            return if (windowVisible) {
                "xaml=application-start-visible"
            } else {
                "xaml=window-not-visible"
            }
        } finally {
            PlatformComInterop.release(applicationStatics)
            PlatformComInterop.release(activationFactory)
        }
    }
}
