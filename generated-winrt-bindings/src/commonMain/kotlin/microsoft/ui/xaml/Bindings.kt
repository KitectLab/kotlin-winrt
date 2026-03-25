package microsoft.ui.xaml

import dev.winrt.core.DateTime
import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.GuidValue
import dev.winrt.core.IReference
import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.TimeSpan
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.WinRtRuntime
import windows.foundation.IStringable
import dev.winrt.kom.ComPtr

open class Application(pointer: ComPtr) : Inspectable(pointer) {
    fun start() {
    }

    fun getLaunchCount(): UInt32 = UInt32(0u)

    fun asIStringable(): IStringable = IStringable.from(this)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Application"
        override val classId = RuntimeClassId("Microsoft.UI.Xaml", "Application")
        override val defaultInterfaceName: String? = "Windows.Foundation.IStringable"

        fun activate(): Application = WinRtRuntime.activate(classId, ::Application)
    }
}

open class Window(pointer: ComPtr) : Inspectable(pointer) {
    private val backingTitle = RuntimeProperty("")
    private val backingIsVisible = RuntimeProperty(WinRtBoolean.FALSE)
    private val backingCreatedAt = RuntimeProperty(DateTime(0))
    private val backingLifetime = RuntimeProperty(TimeSpan(0))
    private val backingLastToken = RuntimeProperty(EventRegistrationToken(0))
    private val backingStableId = RuntimeProperty(GuidValue(""))
    private val backingOptionalTitle = RuntimeProperty(IReference(""))

    var title: String
        get() = backingTitle.get()
        set(value) {
            backingTitle.set(value)
        }

    val isVisible: WinRtBoolean
        get() = backingIsVisible.get()

    val createdAt: DateTime
        get() = backingCreatedAt.get()

    val lifetime: TimeSpan
        get() = backingLifetime.get()

    val lastToken: EventRegistrationToken
        get() = backingLastToken.get()

    val stableId: GuidValue
        get() = backingStableId.get()

    val optionalTitle: IReference<String>
        get() = backingOptionalTitle.get()

    fun activate() {
    }

    fun asIStringable(): IStringable = IStringable.from(this)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Window"
        override val classId = RuntimeClassId("Microsoft.UI.Xaml", "Window")
        override val defaultInterfaceName: String? = "Windows.Foundation.IStringable"

        fun activateInstance(): Window = WinRtRuntime.activate(classId, ::Window)
    }
}
