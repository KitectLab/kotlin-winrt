package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import windows.foundation.IStringable

open class Application(pointer: ComPtr) : Inspectable(pointer) {
    constructor() : this(Companion.activate().pointer)

    fun start() {
        if (pointer.isNull) return
        PlatformComInterop.invokeUnitMethod(pointer, 6).getOrThrow()
    }

    fun getLaunchCount(): UInt32 {
        if (pointer.isNull) return UInt32(0u)
        return UInt32(PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())
    }

    fun asIStringable(): IStringable = IStringable.from(this)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Application"
        override val classId = RuntimeClassId("Microsoft.UI.Xaml", "Application")
        override val defaultInterfaceName: String? = "Windows.Foundation.IStringable"
        override val activationKind = WinRtActivationKind.Factory

        val current: Application
            get() = statics.get_Current()

        fun activate(): Application = WinRtRuntime.activate(this, ::Application)

        fun start(callback: ApplicationInitializationCallback) {
            statics.start(callback)
        }

        fun start(callback: (IApplicationInitializationCallbackParams) -> Unit): WinRtDelegateHandle {
            val delegateHandle = WinRtDelegateBridge.createObjectArgUnitDelegate(ApplicationInitializationCallback.iid) { arg ->
                callback(IApplicationInitializationCallbackParams(arg))
            }
            statics.start(ApplicationInitializationCallback(delegateHandle.pointer))
            return delegateHandle
        }

        private val statics: IApplicationStatics by lazy {
            WinRtRuntime.projectActivationFactory(this, IApplicationStatics, ::IApplicationStatics)
        }
    }
}
