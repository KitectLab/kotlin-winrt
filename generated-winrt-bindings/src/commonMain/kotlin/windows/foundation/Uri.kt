package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.Int32
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public open class Uri(
  pointer: ComPtr,
) : Inspectable(pointer),
    IUriRuntimeClass,
    IUriRuntimeClassWithAbsoluteCanonicalUri,
    IStringable {
  private val backing_AbsoluteCanonicalUri: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val absoluteCanonicalUri: String
    get() {
      if (pointer.isNull) {
        return backing_AbsoluteCanonicalUri.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_DisplayIri: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val displayIri: String
    get() {
      if (pointer.isNull) {
        return backing_DisplayIri.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_AbsoluteUri: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val absoluteUri: String
    get() {
      if (pointer.isNull) {
        return backing_AbsoluteUri.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_DisplayUri: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val displayUri: String
    get() {
      if (pointer.isNull) {
        return backing_DisplayUri.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_Domain: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val domain: String
    get() {
      if (pointer.isNull) {
        return backing_Domain.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 8).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_Extension: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val extension: String
    get() {
      if (pointer.isNull) {
        return backing_Extension.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 9).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_Fragment: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val fragment: String
    get() {
      if (pointer.isNull) {
        return backing_Fragment.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 10).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_Host: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val host: String
    get() {
      if (pointer.isNull) {
        return backing_Host.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 11).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_Password: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val password: String
    get() {
      if (pointer.isNull) {
        return backing_Password.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 12).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_Path: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val path: String
    get() {
      if (pointer.isNull) {
        return backing_Path.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 13).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_Port: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  override val port: Int32
    get() {
      if (pointer.isNull) {
        return backing_Port.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 19).getOrThrow())
    }

  private val backing_Query: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val query: String
    get() {
      if (pointer.isNull) {
        return backing_Query.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_QueryParsed: RuntimeProperty<WwwFormUrlDecoder> =
      RuntimeProperty<WwwFormUrlDecoder>(WwwFormUrlDecoder(ComPtr.NULL))

  override val queryParsed: WwwFormUrlDecoder
    get() {
      if (pointer.isNull) {
        return backing_QueryParsed.get()
      }
      return WwwFormUrlDecoder(PlatformComInterop.invokeObjectMethod(pointer, 15).getOrThrow())
    }

  private val backing_RawUri: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val rawUri: String
    get() {
      if (pointer.isNull) {
        return backing_RawUri.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 16).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_SchemeName: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val schemeName: String
    get() {
      if (pointer.isNull) {
        return backing_SchemeName.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 17).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_Suspicious: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override val suspicious: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_Suspicious.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 20).getOrThrow())
    }

  private val backing_UserName: RuntimeProperty<String> = RuntimeProperty<String>("")

  override val userName: String
    get() {
      if (pointer.isNull) {
        return backing_UserName.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 18).getOrThrow().use {
          it.toKotlinString() }
    }

  public constructor(uri: String) : this(Companion.factoryCreateUri(uri).pointer)

  public constructor(baseUri: String, relativeUri: String) :
      this(Companion.factoryCreateWithRelativeUri(baseUri, relativeUri).pointer)

  override fun toString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow().use { it.toKotlinString()
        }
  }

  override fun equals(pUri: Uri): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithObjectArg(pointer, 21,
        pUri.pointer).getOrThrow())
  }

  override fun combineUri(relativeUri: String): Uri {
    if (pointer.isNull) {
      error("Null runtime object pointer: CombineUri")
    }
    return Uri(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 22,
        relativeUri).getOrThrow())
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Foundation.Uri"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Foundation", "Uri")

    override val defaultInterfaceName: String? = "Windows.Foundation.IUriRuntimeClass"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val factory: IUriRuntimeClassFactory by lazy {
        WinRtRuntime.projectActivationFactory(this, IUriRuntimeClassFactory,
        ::IUriRuntimeClassFactory) }

    private val statics: IUriEscapeStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IUriEscapeStatics, ::IUriEscapeStatics) }

    private fun factoryCreateUri(uri: String): Uri = factory.createUri(uri)

    private fun factoryCreateWithRelativeUri(baseUri: String, relativeUri: String): Uri =
        factory.createWithRelativeUri(baseUri, relativeUri)

    public fun unescapeComponent(toUnescape: String): String = statics.unescapeComponent(toUnescape)

    public fun escapeComponent(toEscape: String): String = statics.escapeComponent(toEscape)
  }
}
