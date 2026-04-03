package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop

open class ToggleSwitch(
    pointer: ComPtr,
) : Control(pointer) {
    constructor() : this(Companion.activate().pointer)

    private val backingIsOn = RuntimeProperty(false)
    private val backingOnContent = RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))
    private val backingOffContent = RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

    var isOn: Boolean
        get() {
            if (pointer.isNull) return backingIsOn.get()
            return PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow()
        }
        set(value) {
            if (pointer.isNull) {
                backingIsOn.set(value)
                return
            }
            PlatformComInterop.invokeBooleanSetter(pointer, 7, value).getOrThrow()
        }

    var onContent: Inspectable
        get() {
            if (pointer.isNull) return backingOnContent.get()
            return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())
        }
        set(value) {
            if (pointer.isNull) {
                backingOnContent.set(value)
                return
            }
            PlatformComInterop.invokeObjectSetter(pointer, 13, value.pointer).getOrThrow()
        }

    var offContent: Inspectable
        get() {
            if (pointer.isNull) return backingOffContent.get()
            return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 16).getOrThrow())
        }
        set(value) {
            if (pointer.isNull) {
                backingOffContent.set(value)
                return
            }
            PlatformComInterop.invokeObjectSetter(pointer, 17, value.pointer).getOrThrow()
        }

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.ToggleSwitch"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "ToggleSwitch")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IToggleSwitch"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        fun activate(): ToggleSwitch = WinRtRuntime.activate(this, ::ToggleSwitch)
    }
}
