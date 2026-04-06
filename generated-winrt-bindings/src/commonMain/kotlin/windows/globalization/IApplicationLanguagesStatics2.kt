package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import windows.foundation.collections.IVectorView
import windows.system.User

internal open class IApplicationLanguagesStatics2(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun getLanguagesForUser(user: User): IVectorView<String> =
      IVectorView.from(Inspectable(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 6,
      projectedObjectArgumentPointer(user, "Windows.System.User",
      "rc(Windows.System.User;{df9a26c6-e746-4bcd-b5d4-120103c4209b})")).getOrThrow()), "string",
      "String")

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Globalization.IApplicationLanguagesStatics2"

    override val projectionTypeKey: String = "Windows.Globalization.IApplicationLanguagesStatics2"

    override val iid: Guid = guidOf("1df0de4f-072b-4d7b-8f06-cb2db40f2bb5")

    public fun from(inspectable: Inspectable): IApplicationLanguagesStatics2 =
        inspectable.projectInterface(this, ::IApplicationLanguagesStatics2)

    public operator fun invoke(inspectable: Inspectable): IApplicationLanguagesStatics2 =
        from(inspectable)
  }
}
