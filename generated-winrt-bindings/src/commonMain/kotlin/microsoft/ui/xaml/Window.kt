package microsoft.ui.xaml

import dev.winrt.core.DateTime
import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.GuidValue
import dev.winrt.core.IReference
import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.TimeSpan
import dev.winrt.core.WinRtActivationKind
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
        get() {
            if (pointer.isNull) return backingCreatedAt.get()
            return DateTime(PlatformComInterop.invokeInt64Getter(pointer, 10).getOrThrow())
        }

    val lifetime: TimeSpan
        get() {
            if (pointer.isNull) return backingLifetime.get()
            return TimeSpan(PlatformComInterop.invokeInt64Getter(pointer, 11).getOrThrow())
        }

    val lastToken: EventRegistrationToken
        get() {
            if (pointer.isNull) return backingLastToken.get()
            return EventRegistrationToken(PlatformComInterop.invokeInt64Getter(pointer, 12).getOrThrow())
        }

    val stableId: GuidValue
        get() {
            if (pointer.isNull) return backingStableId.get()
            return GuidValue(PlatformComInterop.invokeGuidGetter(pointer, 9).getOrThrow().toString())
        }

    val optionalTitle: IReference<String>
        get() {
            if (pointer.isNull) return backingOptionalTitle.get()
            val value = PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow()
            return try {
                IReference(WinRtStrings.toKotlin(value))
            } finally {
                WinRtStrings.release(value)
            }
        }

    fun activate() {
        if (pointer.isNull) return
        PlatformComInterop.invokeUnitMethod(pointer, 13).getOrThrow()
    }

    fun asIStringable(): IStringable = IStringable.from(this)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Window"
        override val classId = RuntimeClassId("Microsoft.UI.Xaml", "Window")
        override val defaultInterfaceName: String? = "Windows.Foundation.IStringable"
        override val activationKind = WinRtActivationKind.Factory

        fun activateInstance(): Window = WinRtRuntime.activate(this, ::Window)
    }
}
