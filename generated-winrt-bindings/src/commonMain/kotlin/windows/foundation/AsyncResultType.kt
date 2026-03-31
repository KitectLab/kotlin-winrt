package windows.foundation

import dev.winrt.core.UInt32
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop

public class AsyncResultType<TResult> internal constructor(
  public val signature: String,
  internal val getResults: (ComPtr, Int) -> TResult,
)

public object AsyncResultTypes {
  public fun <TResult> signature(signature: String): AsyncResultType<TResult> =
      AsyncResultType(signature) { pointer, vtableIndex ->
        decodeScalar(signature, pointer, vtableIndex)
      }

  public fun <TResult> projected(
    signature: String,
    decode: (ComPtr) -> TResult,
  ): AsyncResultType<TResult> = AsyncResultType(signature) { pointer, vtableIndex ->
    decode(PlatformComInterop.invokeObjectMethod(pointer, vtableIndex).getOrThrow())
  }

  public val string: AsyncResultType<String> =
      AsyncResultType(WinRtTypeSignature.string()) { pointer, vtableIndex ->
        PlatformComInterop.invokeHStringMethod(pointer, vtableIndex).getOrThrow().use { it.toKotlinString() }
      }

  public val boolean: AsyncResultType<Boolean> =
      AsyncResultType("b1") { pointer, vtableIndex ->
        PlatformComInterop.invokeBooleanGetter(pointer, vtableIndex).getOrThrow()
      }

  public val int32: AsyncResultType<Int> =
      AsyncResultType("i4") { pointer, vtableIndex ->
        PlatformComInterop.invokeInt32Method(pointer, vtableIndex).getOrThrow()
      }

  public val uint32: AsyncResultType<UInt32> =
      AsyncResultType("u4") { pointer, vtableIndex ->
        UInt32(PlatformComInterop.invokeUInt32Method(pointer, vtableIndex).getOrThrow())
      }

  public val int64: AsyncResultType<Long> =
      AsyncResultType("i8") { pointer, vtableIndex ->
        PlatformComInterop.invokeInt64Getter(pointer, vtableIndex).getOrThrow()
      }

  public val uint64: AsyncResultType<ULong> =
      AsyncResultType("u8") { pointer, vtableIndex ->
        PlatformComInterop.invokeUInt64Method(pointer, vtableIndex).getOrThrow()
      }

  public val float32: AsyncResultType<Float> =
      AsyncResultType("f4") { pointer, vtableIndex ->
        PlatformComInterop.invokeFloat32Method(pointer, vtableIndex).getOrThrow()
      }

  public val float64: AsyncResultType<Double> =
      AsyncResultType("f8") { pointer, vtableIndex ->
        PlatformComInterop.invokeFloat64Method(pointer, vtableIndex).getOrThrow()
      }

  public fun <TResult> interfaceType(
    metadata: WinRtInterfaceMetadata,
    decode: (ComPtr) -> TResult,
  ): AsyncResultType<TResult> = projected(
      signature = WinRtTypeSignature.guid(metadata.iid.toString()),
      decode = decode,
  )

  public fun <TResult> interfaceType(metadata: WinRtInterfaceMetadata): AsyncResultType<TResult> =
      AsyncResultType(WinRtTypeSignature.guid(metadata.iid.toString())) { _, _ ->
        error("Async interface result projection requires an explicit decoder for ${metadata.qualifiedName}")
      }

  @Suppress("UNCHECKED_CAST")
  private fun <TResult> decodeScalar(
    signature: String,
    pointer: ComPtr,
    vtableIndex: Int,
  ): TResult = when (signature) {
    WinRtTypeSignature.string() ->
      PlatformComInterop.invokeHStringMethod(pointer, vtableIndex).getOrThrow().use { it.toKotlinString() }
    "b1" -> PlatformComInterop.invokeBooleanGetter(pointer, vtableIndex).getOrThrow()
    "i4" -> PlatformComInterop.invokeInt32Method(pointer, vtableIndex).getOrThrow()
    "u4" -> UInt32(PlatformComInterop.invokeUInt32Method(pointer, vtableIndex).getOrThrow())
    "i8" -> PlatformComInterop.invokeInt64Getter(pointer, vtableIndex).getOrThrow()
    "u8" -> PlatformComInterop.invokeUInt64Method(pointer, vtableIndex).getOrThrow()
    "f4" -> PlatformComInterop.invokeFloat32Method(pointer, vtableIndex).getOrThrow()
    "f8" -> PlatformComInterop.invokeFloat64Method(pointer, vtableIndex).getOrThrow()
    else -> error("Async result projection requires an explicit decoder for signature $signature")
  } as TResult
}
