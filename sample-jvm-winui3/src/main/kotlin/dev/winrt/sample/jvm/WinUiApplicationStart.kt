package dev.winrt.sample.jvm

import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.PlatformComInterop
import microsoft.ui.xaml.Application
import microsoft.ui.xaml.ApplicationInitializationCallback
import microsoft.ui.xaml.IApplicationStatics
import microsoft.ui.xaml.IApplicationInitializationCallbackParams
import microsoft.ui.xaml.IWindow
import microsoft.ui.xaml.Window
import microsoft.ui.xaml.controls.StackPanel
import microsoft.ui.xaml.controls.TextBlock

object WinUiApplicationStart {
    private var application: Application? = null
    private var window: Window? = null
    private var activeCallback: WinRtDelegateHandle? = null
    @Volatile private var launchFailure: Throwable? = null
    @Volatile private var windowVisible: Boolean = false

    fun probeCallbackOnly(): Boolean {
        var callbackInvoked = false
        val activationFactory = JvmWinRtRuntime.getActivationFactory("Microsoft.UI.Xaml.Application").getOrThrow()
        val applicationStatics = IApplicationStatics(
            PlatformComInterop.queryInterface(activationFactory, IApplicationStatics.iid).getOrThrow(),
        )
        try {
            val callback = WinRtDelegateBridge.createObjectArgUnitDelegate(ApplicationInitializationCallback.iid) {
                callbackInvoked = true
                val uiThreadId = WindowsMessageLoop.currentThreadId()
                Thread.ofPlatform().daemon(true).start {
                    Thread.sleep(100L)
                    WindowsMessageLoop.postThreadQuit(uiThreadId)
                }
            }
            activeCallback?.close()
            activeCallback = callback
            applicationStatics.start(ApplicationInitializationCallback(callback.pointer))
            return callbackInvoked
        } finally {
            PlatformComInterop.release(applicationStatics.pointer)
            PlatformComInterop.release(activationFactory)
        }
    }

    fun probeCallbackParamsInterface(): Boolean {
        var callbackInvoked = false
        var paramsSupported = false
        val activationFactory = JvmWinRtRuntime.getActivationFactory("Microsoft.UI.Xaml.Application").getOrThrow()
        val applicationStatics = IApplicationStatics(
            PlatformComInterop.queryInterface(activationFactory, IApplicationStatics.iid).getOrThrow(),
        )
        try {
            val callback = WinRtDelegateBridge.createObjectArgUnitDelegate(ApplicationInitializationCallback.iid) { arg ->
                callbackInvoked = true
                paramsSupported = runCatching {
                    val paramsPointer = PlatformComInterop.queryInterface(
                        arg,
                        IApplicationInitializationCallbackParams.iid,
                    ).getOrThrow()
                    PlatformComInterop.release(paramsPointer)
                    true
                }.getOrDefault(false)
                val uiThreadId = WindowsMessageLoop.currentThreadId()
                Thread.ofPlatform().daemon(true).start {
                    Thread.sleep(100L)
                    WindowsMessageLoop.postThreadQuit(uiThreadId)
                }
            }
            activeCallback?.close()
            activeCallback = callback
            applicationStatics.start(ApplicationInitializationCallback(callback.pointer))
            return callbackInvoked && paramsSupported
        } finally {
            PlatformComInterop.release(applicationStatics.pointer)
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
        val applicationStatics = IApplicationStatics(
            PlatformComInterop.queryInterface(activationFactory, IApplicationStatics.iid).getOrThrow(),
        )
        try {
            val callback = WinRtDelegateBridge.createObjectArgUnitDelegate(ApplicationInitializationCallback.iid) {
                runCatching {
                    application = applicationStatics.get_Current()
                    window = Window()
                    val iWindow = IWindow.from(window!!)
                    val stackPanel = StackPanel()
                    iWindow.title = windowTitle
                    iWindow.setContent(stackPanel)
                    iWindow.activate()
                    val uiThreadId = WindowsMessageLoop.currentThreadId()
                    val autoQuitVisible = System.getProperty("dev.winrt.autoQuitVisible", "false").equals("true", ignoreCase = true)
                    Thread.ofPlatform().daemon(true).start {
                        val visible = WindowsWindowProbe.waitForWindowByTitle(windowTitle, timeoutMillis = 5_000L)
                        windowVisible = visible
                        if (!visible || autoQuitVisible) {
                            if (visible) {
                                Thread.sleep(500L)
                                WindowsWindowProbe.closeWindowByTitle(windowTitle)
                            } else {
                                WindowsMessageLoop.postThreadQuit(uiThreadId)
                            }
                        }
                    }
                }.getOrElse { error ->
                    launchFailure = error
                }
            }
            activeCallback = callback
            applicationStatics.start(ApplicationInitializationCallback(callback.pointer))
            launchFailure?.let { throw it }
            return if (windowVisible) {
                "xaml=application-start-visible"
            } else {
                "xaml=window-not-visible"
            }
        } finally {
            PlatformComInterop.release(applicationStatics.pointer)
            PlatformComInterop.release(activationFactory)
        }
    }
}
