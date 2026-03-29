package microsoft.ui.xaml

import dev.winrt.core.DateTime
import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.IReference
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.TimeSpan
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr

open class Window(pointer: ComPtr) : dev.winrt.core.Inspectable(pointer) {
    constructor() : this(Companion.activateInstance().pointer)

    private val backingIsVisible = RuntimeProperty(WinRtBoolean.FALSE)
    private val backingCreatedAt = RuntimeProperty(DateTime(0))
    private val backingLifetime = RuntimeProperty(TimeSpan(0))
    private val backingLastToken = RuntimeProperty(EventRegistrationToken(0))
    private val backingOptionalTitle = RuntimeProperty(IReference(""))

    val isVisible: WinRtBoolean
        get() {
            if (pointer.isNull) return backingIsVisible.get()
            return WinRtBoolean(dev.winrt.kom.PlatformComInterop.invokeBooleanGetter(pointer, 8).getOrThrow())
        }

    val createdAt: DateTime
        get() {
            if (pointer.isNull) return backingCreatedAt.get()
            return DateTime(dev.winrt.kom.PlatformComInterop.invokeInt64Getter(pointer, 10).getOrThrow())
        }

    val lifetime: TimeSpan
        get() {
            if (pointer.isNull) return backingLifetime.get()
            return TimeSpan(dev.winrt.kom.PlatformComInterop.invokeInt64Getter(pointer, 11).getOrThrow())
        }

    val lastToken: EventRegistrationToken
        get() {
            if (pointer.isNull) return backingLastToken.get()
            return EventRegistrationToken(dev.winrt.kom.PlatformComInterop.invokeInt64Getter(pointer, 12).getOrThrow())
        }

    val optionalTitle: IReference<String>
        get() {
            if (pointer.isNull) return backingOptionalTitle.get()
            val value = dev.winrt.kom.PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow()
            return try {
                IReference(dev.winrt.core.WinRtStrings.toKotlin(value))
            } finally {
                dev.winrt.core.WinRtStrings.release(value)
            }
        }

    val stableId: dev.winrt.core.GuidValue
        get() = dev.winrt.core.GuidValue(dev.winrt.kom.PlatformComInterop.invokeGuidGetter(pointer, 9).getOrThrow().toString())

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Window"
        override val classId = RuntimeClassId("Microsoft.UI.Xaml", "Window")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IWindow"
        override val activationKind = WinRtActivationKind.Factory

        fun activateInstance(): Window = WinRtRuntime.activate(this, ::Window)
    }
}
