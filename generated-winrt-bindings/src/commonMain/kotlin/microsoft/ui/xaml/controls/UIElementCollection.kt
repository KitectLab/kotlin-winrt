package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.Inspectable
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import microsoft.ui.xaml.UIElement
import microsoft.ui.xaml.interop.IBindableVector

open class UIElementCollection(
    pointer: ComPtr,
) : dev.winrt.core.Inspectable(pointer),
    MutableList<UIElement> by createMutableListDelegate(pointer) {
    constructor() : this(Companion.activate().pointer)

    fun getAt(index: UInt32): UIElement =
        UIElement(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow())

    fun append(value: UIElement) {
        PlatformComInterop.invokeObjectSetter(pointer, 13, value.pointer).getOrThrow()
    }

    fun asIBindableVector(): IBindableVector = IBindableVector.from(this)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.UIElementCollection"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "UIElementCollection")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IUIElementCollection"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        private fun createMutableListDelegate(pointer: ComPtr): MutableList<UIElement> {
            val inspectable = Inspectable(pointer)
            val bindableVector: Lazy<IBindableVector> = lazy { IBindableVector.from(inspectable) }
            return IBindableVector.Companion.createMutableListProjection(
                sizeProvider = { bindableVector.value.size },
                getter = { index: Int ->
                    UIElement(
                        PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 6, index.toUInt()).getOrThrow(),
                    )
                },
                append = { value: UIElement ->
                    PlatformComInterop.invokeObjectSetter(pointer, 13, value.pointer).getOrThrow()
                },
                clearer = { bindableVector.value.clear() },
            )
        }

        fun activate(): UIElementCollection = WinRtRuntime.activate(this, ::UIElementCollection)
    }
}
