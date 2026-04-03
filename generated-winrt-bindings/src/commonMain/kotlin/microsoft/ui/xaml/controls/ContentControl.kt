package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr

open class ContentControl(
    pointer: ComPtr,
) : Control(pointer) {
    constructor() : this(Companion.activate().pointer)

    private val backingContent = RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

    var content: Inspectable
        get() {
            if (pointer.isNull) return backingContent.get()
            return IContentControl.from(this).content
        }
        set(value) {
            if (pointer.isNull) {
                backingContent.set(value)
                return
            }
            IContentControl.from(this).content = value
        }

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.ContentControl"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "ContentControl")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IContentControl"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        fun activate(): ContentControl = WinRtRuntime.activate(this, ::ContentControl)
    }
}
