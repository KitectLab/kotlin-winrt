package microsoft.ui.xaml

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop

open class Application(pointer: ComPtr) : dev.winrt.core.Inspectable(pointer) {
    constructor() : this(Companion.activate().pointer)

    protected open fun onLaunched(args: LaunchActivatedEventArgs) {
        if (pointer.isNull) return
        PlatformComInterop.invokeObjectSetter(pointer, 6, args.pointer).getOrThrow()
    }

    fun start() {
        if (pointer.isNull) return
        PlatformComInterop.invokeUnitMethod(pointer, 6).getOrThrow()
    }

    fun getLaunchCount(): UInt32 {
        if (pointer.isNull) return UInt32(0u)
        return UInt32(PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())
    }

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Application"
        override val classId = RuntimeClassId("Microsoft.UI.Xaml", "Application")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IApplication"
        override val activationKind = WinRtActivationKind.Factory

        val current: Application
            get() = statics.get_Current()

        fun activate(): Application = WinRtRuntime.activate(this, ::Application)

        fun start(callback: ApplicationInitializationCallback) {
            statics.start(callback)
        }

        fun start(callback: (IApplicationInitializationCallbackParams) -> Unit): WinRtDelegateHandle {
            val delegateHandle = WinRtDelegateBridge.createUnitDelegate(
                ApplicationInitializationCallback.iid,
                listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT),
            ) { args ->
                val arg = args.single() as ComPtr
                callback(IApplicationInitializationCallbackParams(arg))
            }
            try {
                statics.start(ApplicationInitializationCallback(delegateHandle.pointer))
            } catch (t: Throwable) {
                delegateHandle.close()
                throw t
            }
            return delegateHandle
        }

        private val statics: IApplicationStatics by lazy {
            WinRtRuntime.projectActivationFactory(this, IApplicationStatics, ::IApplicationStatics)
        }
    }
}
