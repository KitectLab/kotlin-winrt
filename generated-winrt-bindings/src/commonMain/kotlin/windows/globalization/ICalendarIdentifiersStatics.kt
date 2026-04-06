package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

internal open class ICalendarIdentifiersStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val gregorian: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  public val hebrew: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  public val hijri: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 8).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  public val japanese: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 9).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  public val julian: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 10).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  public val korean: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 11).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  public val taiwan: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 12).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  public val thai: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 13).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  public val umAlQura: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Globalization.ICalendarIdentifiersStatics"

    override val projectionTypeKey: String = "Windows.Globalization.ICalendarIdentifiersStatics"

    override val iid: Guid = guidOf("80653f68-2cb2-4c1f-b590-f0f52bf4fd1a")

    public fun from(inspectable: Inspectable): ICalendarIdentifiersStatics =
        inspectable.projectInterface(this, ::ICalendarIdentifiersStatics)

    public operator fun invoke(inspectable: Inspectable): ICalendarIdentifiersStatics =
        from(inspectable)
  }
}
