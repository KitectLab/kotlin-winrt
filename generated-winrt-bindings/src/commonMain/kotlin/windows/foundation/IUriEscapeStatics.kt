package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

internal open class IUriEscapeStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun unescapeComponent(toUnescape: String): String {
    val value = PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 6,
        toUnescape).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  public fun escapeComponent(toEscape: String): String {
    val value = PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 7,
        toEscape).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.IUriEscapeStatics"

    override val projectionTypeKey: String = "Windows.Foundation.IUriEscapeStatics"

    override val iid: Guid = guidOf("c1d432ba-c824-4452-a7fd-512bc3bbe9a1")

    public fun from(inspectable: Inspectable): IUriEscapeStatics =
        inspectable.projectInterface(this, ::IUriEscapeStatics)

    public operator fun invoke(inspectable: Inspectable): IUriEscapeStatics = from(inspectable)
  }
}
