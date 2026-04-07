package microsoft.ui.xaml.controls

import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComStructValue
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import microsoft.ui.xaml.DataTemplate
import microsoft.ui.xaml.DependencyProperty
import microsoft.ui.xaml.RoutedEventHandler
import microsoft.ui.xaml.controls.primitives.ToggleSwitchTemplateSettings

public open class ToggleSwitch(
  pointer: ComPtr,
) : Control(pointer),
    IToggleSwitch {
  private val backing_Header: RuntimeProperty<Inspectable> =
      RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

  override var header: Inspectable
    get() {
      if (pointer.isNull) {
        return backing_Header.get()
      }
      return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Header.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 9, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_HeaderTemplate: RuntimeProperty<DataTemplate> =
      RuntimeProperty<DataTemplate>(DataTemplate(ComPtr.NULL))

  override var headerTemplate: DataTemplate
    get() {
      if (pointer.isNull) {
        return backing_HeaderTemplate.get()
      }
      return DataTemplate(PlatformComInterop.invokeObjectMethod(pointer, 10).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_HeaderTemplate.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 11, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_IsOn: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var isOn: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsOn.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsOn.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 7, if (value.value) 1u else 0u).getOrThrow()
    }

  private val backing_OffContent: RuntimeProperty<Inspectable> =
      RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

  override var offContent: Inspectable
    get() {
      if (pointer.isNull) {
        return backing_OffContent.get()
      }
      return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 16).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_OffContent.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 17, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_OffContentTemplate: RuntimeProperty<DataTemplate> =
      RuntimeProperty<DataTemplate>(DataTemplate(ComPtr.NULL))

  override var offContentTemplate: DataTemplate
    get() {
      if (pointer.isNull) {
        return backing_OffContentTemplate.get()
      }
      return DataTemplate(PlatformComInterop.invokeObjectMethod(pointer, 18).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_OffContentTemplate.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 19, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_OnContent: RuntimeProperty<Inspectable> =
      RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

  override var onContent: Inspectable
    get() {
      if (pointer.isNull) {
        return backing_OnContent.get()
      }
      return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_OnContent.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 13, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_OnContentTemplate: RuntimeProperty<DataTemplate> =
      RuntimeProperty<DataTemplate>(DataTemplate(ComPtr.NULL))

  override var onContentTemplate: DataTemplate
    get() {
      if (pointer.isNull) {
        return backing_OnContentTemplate.get()
      }
      return DataTemplate(PlatformComInterop.invokeObjectMethod(pointer, 14).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_OnContentTemplate.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 15, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_TemplateSettings: RuntimeProperty<ToggleSwitchTemplateSettings> =
      RuntimeProperty<ToggleSwitchTemplateSettings>(ToggleSwitchTemplateSettings(ComPtr.NULL))

  override val templateSettings: ToggleSwitchTemplateSettings
    get() {
      if (pointer.isNull) {
        return backing_TemplateSettings.get()
      }
      return ToggleSwitchTemplateSettings(PlatformComInterop.invokeObjectMethod(pointer,
          20).getOrThrow())
    }

  private val backing_HeaderProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val headerProperty: DependencyProperty
    get() = backing_HeaderProperty.get()

  private val backing_HeaderTemplateProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val headerTemplateProperty: DependencyProperty
    get() = backing_HeaderTemplateProperty.get()

  private val backing_IsOnProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val isOnProperty: DependencyProperty
    get() = backing_IsOnProperty.get()

  private val backing_OffContentProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val offContentProperty: DependencyProperty
    get() = backing_OffContentProperty.get()

  private val backing_OffContentTemplateProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val offContentTemplateProperty: DependencyProperty
    get() = backing_OffContentTemplateProperty.get()

  private val backing_OnContentProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val onContentProperty: DependencyProperty
    get() = backing_OnContentProperty.get()

  private val backing_OnContentTemplateProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val onContentTemplateProperty: DependencyProperty
    get() = backing_OnContentTemplateProperty.get()

  public constructor() : this(Companion.activate().pointer)

  public fun onToggled() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 6).getOrThrow()
  }

  public fun onOnContentChanged(oldContent: Inspectable, newContent: Inspectable) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 7,
        projectedObjectArgumentPointer(oldContent, "Object", "cinterface(IInspectable)"),
        projectedObjectArgumentPointer(newContent, "Object",
        "cinterface(IInspectable)")).getOrThrow()
  }

  public fun onOffContentChanged(oldContent: Inspectable, newContent: Inspectable) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 8,
        projectedObjectArgumentPointer(oldContent, "Object", "cinterface(IInspectable)"),
        projectedObjectArgumentPointer(newContent, "Object",
        "cinterface(IInspectable)")).getOrThrow()
  }

  public fun onHeaderChanged(oldContent: Inspectable, newContent: Inspectable) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 9,
        projectedObjectArgumentPointer(oldContent, "Object", "cinterface(IInspectable)"),
        projectedObjectArgumentPointer(newContent, "Object",
        "cinterface(IInspectable)")).getOrThrow()
  }

  override fun add_Toggled(handler: RoutedEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 21,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.RoutedEventHandler",
        "delegate({dae23d85-69ca-5bdf-805b-6161a3a215cc})")).getOrThrow())
  }

  override fun remove_Toggled(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 22, token.toAbi()).getOrThrow()
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.ToggleSwitch"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls",
        "ToggleSwitch")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IToggleSwitch"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics: IToggleSwitchStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IToggleSwitchStatics, ::IToggleSwitchStatics) }

    public val headerProperty: DependencyProperty
      get() = statics.headerProperty

    public val headerTemplateProperty: DependencyProperty
      get() = statics.headerTemplateProperty

    public val isOnProperty: DependencyProperty
      get() = statics.isOnProperty

    public val offContentProperty: DependencyProperty
      get() = statics.offContentProperty

    public val offContentTemplateProperty: DependencyProperty
      get() = statics.offContentTemplateProperty

    public val onContentProperty: DependencyProperty
      get() = statics.onContentProperty

    public val onContentTemplateProperty: DependencyProperty
      get() = statics.onContentTemplateProperty

    public fun activate(): ToggleSwitch = WinRtRuntime.activate(this, ::ToggleSwitch)
  }
}
