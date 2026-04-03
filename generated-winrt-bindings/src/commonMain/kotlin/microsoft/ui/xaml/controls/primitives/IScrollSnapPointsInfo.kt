package microsoft.ui.xaml.controls.primitives

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import windows.foundation.EventRegistrationToken

interface IScrollSnapPointsInfo {
    val areHorizontalSnapPointsRegular: Boolean
    val areVerticalSnapPointsRegular: Boolean

    fun get_AreHorizontalSnapPointsRegular(): Boolean
    fun get_AreVerticalSnapPointsRegular(): Boolean

    fun remove_HorizontalSnapPointsChanged(token: EventRegistrationToken)
    fun remove_VerticalSnapPointsChanged(token: EventRegistrationToken)

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String =
            "Microsoft.UI.Xaml.Controls.Primitives.IScrollSnapPointsInfo"

        override val projectionTypeKey: String =
            "Microsoft.UI.Xaml.Controls.Primitives.IScrollSnapPointsInfo"

        override val iid: Guid = guidOf("d3ea6e09-ecf7-51a8-bd54-fc84b9653766")

        fun from(inspectable: Inspectable): IScrollSnapPointsInfo =
            inspectable.projectInterface(this, ::IScrollSnapPointsInfoProjection)

        operator fun invoke(inspectable: Inspectable): IScrollSnapPointsInfo = from(inspectable)
    }
}

private class IScrollSnapPointsInfoProjection(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer), IScrollSnapPointsInfo {
    override val areHorizontalSnapPointsRegular: Boolean
        get() = PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow()

    override val areVerticalSnapPointsRegular: Boolean
        get() = PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow()

    override fun get_AreHorizontalSnapPointsRegular(): Boolean =
        PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow()

    override fun get_AreVerticalSnapPointsRegular(): Boolean =
        PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow()

    override fun remove_HorizontalSnapPointsChanged(token: EventRegistrationToken) {
        PlatformComInterop.invokeInt64Setter(pointer, 9, token.value).getOrThrow()
    }

    override fun remove_VerticalSnapPointsChanged(token: EventRegistrationToken) {
        PlatformComInterop.invokeInt64Setter(pointer, 11, token.value).getOrThrow()
    }
}
