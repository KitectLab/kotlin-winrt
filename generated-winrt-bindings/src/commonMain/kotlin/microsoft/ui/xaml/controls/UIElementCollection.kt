package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.projection.WinRtMutableListProjection
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import microsoft.ui.xaml.UIElement
import microsoft.ui.xaml.interop.IBindableVector

open class UIElementCollection(
    pointer: ComPtr,
) : dev.winrt.core.Inspectable(pointer) {
    constructor() : this(Companion.activate().pointer)

    fun getAt(index: UInt32): UIElement =
        UIElement(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow())

    fun append(value: UIElement) {
        PlatformComInterop.invokeObjectSetter(pointer, 13, value.pointer).getOrThrow()
    }

    fun asIBindableVector(): IBindableVector = IBindableVector.from(this)

    fun asMutableList(): MutableList<UIElement> =
        getOrPutHelperWrapper("kotlin.collections.MutableList<Microsoft.UI.Xaml.UIElement>") {
            WinRtMutableListProjection(
                sizeProvider = { asIBindableVector().size.value.toInt() },
                getter = { index -> getAt(UInt32(index.toUInt())) },
                append = ::append,
                clearer = { asIBindableVector().clear() },
            )
        }

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.UIElementCollection"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "UIElementCollection")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IUIElementCollection"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        fun activate(): UIElementCollection = WinRtRuntime.activate(this, ::UIElementCollection)
    }
}
