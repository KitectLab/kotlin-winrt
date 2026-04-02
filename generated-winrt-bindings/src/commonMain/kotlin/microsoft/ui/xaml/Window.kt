package microsoft.ui.xaml

import kotlin.time.Duration
import kotlin.time.Instant
import dev.winrt.core.EventRegistrationToken
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
    private val backingCreatedAt = RuntimeProperty(Instant.fromEpochSeconds(0))
    private val backingLifetime = RuntimeProperty(Duration.parse("0s"))
    private val backingLastToken = RuntimeProperty(EventRegistrationToken(0))
    private val backingOptionalTitle = RuntimeProperty<String?>(null)

    val isVisible: WinRtBoolean
        get() {
            if (pointer.isNull) return backingIsVisible.get()
            return WinRtBoolean(dev.winrt.kom.PlatformComInterop.invokeBooleanGetter(pointer, 8).getOrThrow())
        }

    val createdAt: Instant
        get() {
            if (pointer.isNull) return backingCreatedAt.get()
            return Instant.fromEpochSeconds((dev.winrt.kom.PlatformComInterop.invokeInt64Getter(pointer, 10).getOrThrow() - 116444736000000000) / 10000000L, ((dev.winrt.kom.PlatformComInterop.invokeInt64Getter(pointer, 10).getOrThrow() - 116444736000000000) % 10000000L * 100).toInt())
        }

    val lifetime: Duration
        get() {
            if (pointer.isNull) return backingLifetime.get()
            return Duration.parse("0s")
        }

    val lastToken: EventRegistrationToken
        get() {
            if (pointer.isNull) return backingLastToken.get()
            return EventRegistrationToken(dev.winrt.kom.PlatformComInterop.invokeInt64Getter(pointer, 12).getOrThrow())
        }

    val optionalTitle: String?
        get() {
            if (pointer.isNull) return backingOptionalTitle.get()
            return dev.winrt.kom.PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow().use { value ->
                if (value.isNull) null else value.toKotlinString()
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
