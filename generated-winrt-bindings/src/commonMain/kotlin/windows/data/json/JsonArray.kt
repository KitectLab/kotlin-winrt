package windows.data.json

import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import kotlin.collections.Iterator
import windows.foundation.IStringable
import windows.foundation.collections.IVectorView

public open class JsonArray(
  pointer: ComPtr,
) : Inspectable(pointer),
    IJsonArray,
    IStringable {
  private val backing_Size: RuntimeProperty<UInt32> = RuntimeProperty<UInt32>(UInt32(0u))

  public val size: UInt32
    get() {
      if (pointer.isNull) {
        return backing_Size.get()
      }
      return UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
    }

  private val backing_ValueType: RuntimeProperty<JsonValueType> =
      RuntimeProperty<JsonValueType>(JsonValueType.fromValue(0))

  override val valueType: JsonValueType
    get() {
      if (pointer.isNull) {
        return backing_ValueType.get()
      }
      return JsonValueType.fromValue(PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())
    }

  public constructor() : this(Companion.activate().pointer)

  public fun getAt(index: UInt32): IJsonValue {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetAt")
    }
    return IJsonValue.from(Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer,
        7, index.value).getOrThrow()))
  }

  public fun getView(): IVectorView<IJsonValue> {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetView")
    }
    return IVectorView<IJsonValue>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        9).getOrThrow()))
  }

  public fun winRtIndexOf(value: IJsonValue): UInt32? {
    if (pointer.isNull) {
      return null
    }
    val (found, index) = PlatformComInterop.invokeIndexOfMethod(pointer, 10,
        projectedObjectArgumentPointer(value, "Windows.Data.Json.IJsonValue",
        "{a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e}")).getOrThrow()
    return if (found) UInt32(index) else null
  }

  public fun setAt(index: UInt32, value: IJsonValue) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithUInt32AndObjectArgs(pointer, 11, index.value,
        projectedObjectArgumentPointer(value, "Windows.Data.Json.IJsonValue",
        "{a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e}")).getOrThrow()
  }

  public fun insertAt(index: UInt32, value: IJsonValue) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithUInt32AndObjectArgs(pointer, 12, index.value,
        projectedObjectArgumentPointer(value, "Windows.Data.Json.IJsonValue",
        "{a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e}")).getOrThrow()
  }

  public fun removeAt(index: UInt32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 13, index.value).getOrThrow()
  }

  public fun append(value: IJsonValue) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 14, projectedObjectArgumentPointer(value,
        "Windows.Data.Json.IJsonValue", "{a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e}")).getOrThrow()
  }

  public fun removeAtEnd() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 15).getOrThrow()
  }

  public fun clear() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 16).getOrThrow()
  }

  public fun first(): Iterator<IJsonValue> {
    if (pointer.isNull) {
      error("Null runtime object pointer: First")
    }
    return Iterator<IJsonValue>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        6).getOrThrow()))
  }

  override fun toString(): String {
    if (pointer.isNull) {
      return ""
    }
    return run {
          val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
          try {
            value.toKotlinString()
          } finally {
            value.close()
          }
        }
  }

  override fun getObjectAt(index: UInt32): JsonObject {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetObjectAt")
    }
    return JsonObject(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 6,
        index.value).getOrThrow())
  }

  override fun getArrayAt(index: UInt32): JsonArray {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetArrayAt")
    }
    return JsonArray(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7,
        index.value).getOrThrow())
  }

  override fun getStringAt(index: UInt32): String {
    if (pointer.isNull) {
      return ""
    }
    return run {
          val value = PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 8,
              index.value).getOrThrow()
          try {
            value.toKotlinString()
          } finally {
            value.close()
          }
        }
  }

  override fun getNumberAt(index: UInt32): Float64 {
    if (pointer.isNull) {
      return Float64(0.0)
    }
    return Float64(PlatformComInterop.invokeFloat64MethodWithUInt32Arg(pointer, 9,
        index.value).getOrThrow())
  }

  override fun getBooleanAt(index: UInt32): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithUInt32Arg(pointer, 10,
        index.value).getOrThrow())
  }

  override fun get_ValueType(): JsonValueType {
    if (pointer.isNull) {
      error("Null runtime object pointer: Get_ValueType")
    }
    return JsonValueType.fromValue(PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())
  }

  override fun stringify(): String {
    if (pointer.isNull) {
      return ""
    }
    return run {
          val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
          try {
            value.toKotlinString()
          } finally {
            value.close()
          }
        }
  }

  override fun getString(): String {
    if (pointer.isNull) {
      return ""
    }
    return run {
          val value = PlatformComInterop.invokeHStringMethod(pointer, 8).getOrThrow()
          try {
            value.toKotlinString()
          } finally {
            value.close()
          }
        }
  }

  override fun getNumber(): Float64 {
    if (pointer.isNull) {
      return Float64(0.0)
    }
    return Float64(PlatformComInterop.invokeFloat64Method(pointer, 9).getOrThrow())
  }

  override fun getBoolean(): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 10).getOrThrow())
  }

  override fun getObject(): JsonObject {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetObject")
    }
    return JsonObject(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())
  }

  override fun getArray(): JsonArray {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetArray")
    }
    return JsonArray(PlatformComInterop.invokeObjectMethod(pointer, 11).getOrThrow())
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Data.Json.JsonArray"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Data.Json", "JsonArray")

    override val defaultInterfaceName: String? = "Windows.Data.Json.IJsonArray"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics: IJsonArrayStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IJsonArrayStatics, ::IJsonArrayStatics) }

    public fun activate(): JsonArray = WinRtRuntime.activate(this, ::JsonArray)

    public fun parse(input: String): JsonArray = statics.parse(input)
  }
}
