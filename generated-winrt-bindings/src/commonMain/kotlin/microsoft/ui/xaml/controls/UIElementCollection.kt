package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import microsoft.ui.xaml.UIElement
import dev.winrt.projection.WinRtMutableListProjection

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

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.UIElementCollection"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "UIElementCollection")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IUIElementCollection"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        private fun createMutableListDelegate(pointer: ComPtr): MutableList<UIElement> {
            return createMutableListProjection(
                sizeProvider = {
                    UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow()).value.toInt()
                },
                getter = { index: Int ->
                    UIElement(
                        PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 6, index.toUInt()).getOrThrow(),
                    )
                },
                append = { value: UIElement ->
                    PlatformComInterop.invokeObjectSetter(pointer, 13, value.pointer).getOrThrow()
                },
                clearer = {
                    PlatformComInterop.invokeUnitMethod(pointer, 16).getOrThrow()
                },
            )
        }

        internal fun <T> createMutableListProjection(
            sizeProvider: () -> Int,
            getter: (Int) -> T,
            append: (T) -> Unit,
            clearer: () -> Unit,
        ): MutableList<T> =
            WinRtMutableListProjection(
                sizeProvider = sizeProvider,
                getter = getter,
                append = append,
                clearer = clearer,
            )

        fun activate(): UIElementCollection = WinRtRuntime.activate(this, ::UIElementCollection)
    }
}
