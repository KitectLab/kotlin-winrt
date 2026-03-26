package dev.winrt.sample.jvm

import dev.winrt.core.JvmWinRtNoArgDelegate
import dev.winrt.core.JvmWinRtObjectArgDelegate
import dev.winrt.core.guidOf
import dev.winrt.kom.HResult
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.PlatformComInterop
import microsoft.ui.xaml.Application
import microsoft.ui.xaml.IWindow
import microsoft.ui.xaml.Window

object WinUiApplicationStart {
    private const val applicationStartSlot = 7
    private const val dispatcherQueueGetterSlot = 13
    private const val dispatcherQueueTryEnqueueSlot = 7
    private val applicationStaticsIid = guidOf("4e0d09f5-4358-512c-a987-503b52848e95")
    private val callbackIid = guidOf("d8eef1c9-1234-56f1-9963-45dd9c80a661")
    private val dispatcherQueueHandlerIid = guidOf("2e0872a9-4e29-5f14-b688-fb96d5f9d5f8")

    private var application: Application? = null
    private var window: Window? = null
    private var activeCallback: JvmWinRtObjectArgDelegate? = null
    private var activeDispatchHandler: JvmWinRtNoArgDelegate? = null
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
        activeDispatchHandler?.close()
        activeDispatchHandler = null
        activeCallback?.close()
        activeCallback = null
        window = null
        application = null
    }

    fun launchWindow(windowTitle: String, messageText: String): String {
        launchFailure = null
        windowVisible = false
        activeDispatchHandler?.close()
        activeDispatchHandler = null
        activeCallback?.close()
        activeCallback = null
        val activationFactory = JvmWinRtRuntime.getActivationFactory("Microsoft.UI.Xaml.Application").getOrThrow()
        val applicationStatics = PlatformComInterop.queryInterface(activationFactory, applicationStaticsIid).getOrThrow()
        try {
            val callback = JvmWinRtObjectArgDelegate.create(callbackIid) {
                runCatching {
                    application = Application.activate()
                    window = Window.activateInstance()
                    val uiThreadId = WindowsMessageLoop.currentThreadId()
                    val autoQuitVisible = System.getProperty("dev.winrt.autoQuitVisible", "false").equals("true", ignoreCase = true)
                    val iWindow = IWindow.from(window!!)
                    val dispatcherQueue = PlatformComInterop.invokeObjectMethod(
                        iWindow.pointer,
                        dispatcherQueueGetterSlot,
                    ).getOrThrow()
                    val dispatchHandler = JvmWinRtNoArgDelegate.create(dispatcherQueueHandlerIid) {
                        runCatching {
                            val createdWindow = window ?: error("Window was not initialized")
                            createdWindow.activate()
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
                            WindowsMessageLoop.postThreadQuit(uiThreadId)
                            HResult(0x80004005.toInt())
                        }
                    }
                    activeDispatchHandler = dispatchHandler
                    val enqueued = try {
                        PlatformComInterop.invokeBooleanMethodWithObjectArg(
                            dispatcherQueue,
                            dispatcherQueueTryEnqueueSlot,
                            dispatchHandler.pointer,
                        ).getOrThrow()
                    } finally {
                        PlatformComInterop.release(dispatcherQueue)
                    }
                    check(enqueued) { "DispatcherQueue.TryEnqueue returned false" }
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
