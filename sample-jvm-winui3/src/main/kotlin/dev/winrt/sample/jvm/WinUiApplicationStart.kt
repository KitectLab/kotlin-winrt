package dev.winrt.sample.jvm

import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.guidOf
import microsoft.ui.xaml.Application
import microsoft.ui.xaml.ApplicationInitializationCallback
import microsoft.ui.xaml.IApplicationInitializationCallbackParams
import microsoft.ui.xaml.IWindow
import microsoft.ui.xaml.Window
import microsoft.ui.xaml.media.DesktopAcrylicBackdrop
import microsoft.ui.xaml.media.MicaBackdrop

object WinUiApplicationStart {
    private val iidIXamlMetadataProvider = guidOf("a96251f0-2214-5d53-8746-ce99a2593cd7")
    private var application: SampleWinUiApp? = null
    private var window: Window? = null
    private var activeCallback: WinRtDelegateHandle? = null
    @Volatile private var launchFailure: Throwable? = null
    @Volatile private var windowVisible: Boolean = false

    fun probeCallbackOnly(): Boolean {
        var callbackInvoked = false
        val callback = WinRtDelegateBridge.createUnitDelegate(
            ApplicationInitializationCallback.iid,
            listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT),
        ) {
            val arg = it.single() as dev.winrt.kom.ComPtr
            callbackInvoked = true
            val uiThreadId = WindowsMessageLoop.currentThreadId()
            Thread.ofPlatform().daemon(true).start {
                Thread.sleep(100L)
                WindowsMessageLoop.postThreadQuit(uiThreadId)
            }
        }
        activeCallback?.close()
        activeCallback = callback
        Application.start(ApplicationInitializationCallback(callback.pointer))
        return callbackInvoked
    }

    fun probeCallbackParamsInterface(): Boolean {
        var callbackInvoked = false
        var paramsSupported = false
        val callback = WinRtDelegateBridge.createUnitDelegate(
            ApplicationInitializationCallback.iid,
            listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT),
        ) {
            val arg = it.single() as dev.winrt.kom.ComPtr
            callbackInvoked = true
            paramsSupported = runCatching {
                val paramsPointer = dev.winrt.kom.PlatformComInterop.queryInterface(
                    arg,
                    IApplicationInitializationCallbackParams.iid,
                ).getOrThrow()
                dev.winrt.kom.PlatformComInterop.release(paramsPointer)
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
        Application.start(ApplicationInitializationCallback(callback.pointer))
        return callbackInvoked && paramsSupported
    }

    fun shutdown() {
        activeCallback?.close()
        activeCallback = null
        application?.close()
        window = null
        application = null
    }

    fun launchWindow(windowTitle: String, messageText: String): String {
        launchFailure = null
        windowVisible = false
        activeCallback?.close()
        activeCallback = null
        val callback = WinRtDelegateBridge.createUnitDelegate(
            ApplicationInitializationCallback.iid,
            listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT),
        ) {
            val arg = it.single() as dev.winrt.kom.ComPtr
            runCatching {
                println("winui: application callback invoked")
                val sampleApp = SampleWinUiApp.create()
                application = sampleApp
                println("winui: application instance created")
                runCatching {
                    val metadataProvider = dev.winrt.kom.PlatformComInterop.queryInterface(
                        sampleApp.pointer,
                        iidIXamlMetadataProvider,
                    ).getOrThrow()
                    dev.winrt.kom.PlatformComInterop.release(metadataProvider)
                    println("winui: application supports IXamlMetadataProvider=true")
                }.onFailure {
                    println("winui: application supports IXamlMetadataProvider=false")
                }
                window = Window()
                println("winui: window created")
                val iWindow = IWindow.from(window!!)
                iWindow.title = windowTitle
                println("winui: window title set")
                sampleApp.attachControlResources()
                runCatching {
                    window!!.systemBackdrop = MicaBackdrop()
                    println("winui: window backdrop set=mica")
                }.recoverCatching {
                    window!!.systemBackdrop = DesktopAcrylicBackdrop()
                    println("winui: window backdrop set=desktop-acrylic")
                }.onFailure { error ->
                    println("winui: window backdrop skipped: ${error::class.simpleName}: ${error.message}")
                }
                val layout = WinUiSampleLayout.build(windowTitle, messageText)
                iWindow.setContent(layout.root)
                println("winui: window content set")
                iWindow.activate()
                println("winui: window activated")
                val uiThreadId = WindowsMessageLoop.currentThreadId()
                val autoQuitVisible = System.getProperty("dev.winrt.autoQuitVisible", "false").equals("true", ignoreCase = true)
                Thread.ofPlatform().daemon(true).start {
                    val visible = WindowsWindowProbe.waitForWindowByTitle(windowTitle, timeoutMillis = 5_000L)
                    windowVisible = visible
                    println("winui: window visible=$visible")
                    if (!visible || autoQuitVisible) {
                        if (visible) {
                            Thread.sleep(500L)
                            WindowsWindowProbe.closeWindowByTitle(windowTitle)
                            println("winui: window close requested")
                            WindowsMessageLoop.postThreadQuit(uiThreadId)
                        } else {
                            WindowsMessageLoop.postThreadQuit(uiThreadId)
                        }
                    }
                }
            }.getOrElse { error ->
                launchFailure = error
                println("winui: launch failed: ${error::class.simpleName}: ${error.message}")
            }
        }
        activeCallback = callback
        Application.start(ApplicationInitializationCallback(callback.pointer))
        activeCallback?.close()
        activeCallback = null
        application?.releaseControlResources()
        println("winui: Application.start returned")
        launchFailure?.let { throw it }
        return if (windowVisible) {
            "xaml=application-start-visible"
        } else {
            "xaml=window-not-visible"
        }
    }
}
