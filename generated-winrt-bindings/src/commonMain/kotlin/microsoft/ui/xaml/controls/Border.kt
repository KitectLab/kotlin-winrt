package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import microsoft.ui.xaml.FrameworkElement
import microsoft.ui.xaml.UIElement

open class Border(
    pointer: ComPtr,
) : FrameworkElement(pointer) {
    constructor() : this(Companion.activate().pointer)

    private val backingChild = RuntimeProperty(UIElement(ComPtr.NULL))

    var child: UIElement
        get() {
            if (pointer.isNull) return backingChild.get()
            return UIElement(PlatformComInterop.invokeObjectMethod(pointer, 18).getOrThrow())
        }
        set(value) {
            if (pointer.isNull) {
                backingChild.set(value)
                return
            }
            PlatformComInterop.invokeObjectSetter(pointer, 19, value.pointer).getOrThrow()
        }

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.Border"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "Border")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IBorder"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        fun activate(): Border = WinRtRuntime.activate(this, ::Border)
    }
}
