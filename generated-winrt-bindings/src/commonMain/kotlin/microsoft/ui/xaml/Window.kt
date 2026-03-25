package microsoft.ui.xaml

import dev.winrt.core.DateTime
import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.GuidValue
import dev.winrt.core.IReference
import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.TimeSpan
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.WinRtStrings
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import windows.foundation.IStringable

open class Window(pointer: ComPtr) : Inspectable(pointer) {
    private val backingTitle = RuntimeProperty("")
    private val backingIsVisible = RuntimeProperty(WinRtBoolean.FALSE)
    private val backingCreatedAt = RuntimeProperty(DateTime(0))
    private val backingLifetime = RuntimeProperty(TimeSpan(0))
    private val backingLastToken = RuntimeProperty(EventRegistrationToken(0))
    private val backingStableId = RuntimeProperty(GuidValue(""))
    private val backingOptionalTitle = RuntimeProperty(IReference(""))

    var title: String
        get() {
            if (pointer.isNull) return backingTitle.get()
            val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
            return try {
                WinRtStrings.toKotlin(value)
            } finally {
                WinRtStrings.release(value)
            }
        }
        set(value) {
            if (pointer.isNull) {
                backingTitle.set(value)
                return
            }
            PlatformComInterop.invokeStringSetter(pointer, 7, value).getOrThrow()
        }

    val isVisible: WinRtBoolean
        get() {
            if (pointer.isNull) return backingIsVisible.get()
            return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 8).getOrThrow())
        }

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
