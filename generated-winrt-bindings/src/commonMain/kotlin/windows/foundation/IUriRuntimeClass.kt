package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.Int32
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public interface IUriRuntimeClass {
  public val absoluteUri: String

  public val displayUri: String

  public val domain: String

  public val extension: String

  public val fragment: String

  public val host: String

  public val password: String

  public val path: String

  public val port: Int32

  public val query: String

  public val queryParsed: WwwFormUrlDecoder

  public val rawUri: String

  public val schemeName: String

  public val suspicious: WinRtBoolean

  public val userName: String

  public fun equals(pUri: Uri): WinRtBoolean

  public fun combineUri(relativeUri: String): Uri

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Foundation.IUriRuntimeClass"

    override val projectionTypeKey: String = "Windows.Foundation.IUriRuntimeClass"

    override val iid: Guid = guidOf("9e365e57-48b2-4160-956f-c7385120bbfc")

    public fun from(inspectable: Inspectable): IUriRuntimeClass = inspectable.projectInterface(this,
        ::IUriRuntimeClassProjection)

    public operator fun invoke(inspectable: Inspectable): IUriRuntimeClass = from(inspectable)
  }
}

private class IUriRuntimeClassProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IUriRuntimeClass {
  override val absoluteUri: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val displayUri: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val domain: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 8).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val extension: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 9).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val fragment: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 10).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val host: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 11).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val password: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 12).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val path: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 13).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val port: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 19).getOrThrow())

  override val query: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val queryParsed: WwwFormUrlDecoder
    get() = WwwFormUrlDecoder(PlatformComInterop.invokeObjectMethod(pointer, 15).getOrThrow())

  override val rawUri: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 16).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val schemeName: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 17).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val suspicious: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 20).getOrThrow())

  override val userName: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 18).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override fun equals(pUri: Uri): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithObjectArg(pointer, 21,
      projectedObjectArgumentPointer(pUri, "Windows.Foundation.Uri",
      "rc(Windows.Foundation.Uri;{9e365e57-48b2-4160-956f-c7385120bbfc})")).getOrThrow())

  override fun combineUri(relativeUri: String): Uri =
      Uri(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 22, relativeUri).getOrThrow())
}
