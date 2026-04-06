package microsoft.ui.xaml

import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.Float32
import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.Int32
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComStructValue
import dev.winrt.kom.PlatformComInterop
import java.lang.AutoCloseable
import kotlin.String
import kotlin.Unit
import kotlin.collections.Iterable
import kotlin.collections.MutableMap
import microsoft.ui.composition.AnimationPropertyInfo
import microsoft.ui.composition.IAnimationObject
import microsoft.ui.composition.ICompositionAnimationBase
import microsoft.ui.composition.IVisualElement
import microsoft.ui.composition.IVisualElement2
import microsoft.ui.composition.Visual
import microsoft.ui.input.InputCursor
import microsoft.ui.input.PointerPoint
import microsoft.ui.xaml.automation.peers.AutomationPeer
import microsoft.ui.xaml.controls.primitives.FlyoutBase
import microsoft.ui.xaml.input.AccessKeyDisplayDismissedEventArgs
import microsoft.ui.xaml.input.AccessKeyDisplayRequestedEventArgs
import microsoft.ui.xaml.input.AccessKeyInvokedEventArgs
import microsoft.ui.xaml.input.CharacterReceivedRoutedEventArgs
import microsoft.ui.xaml.input.ContextRequestedEventArgs
import microsoft.ui.xaml.input.DoubleTappedEventHandler
import microsoft.ui.xaml.input.GettingFocusEventArgs
import microsoft.ui.xaml.input.HoldingEventHandler
import microsoft.ui.xaml.input.KeyEventHandler
import microsoft.ui.xaml.input.KeyTipPlacementMode
import microsoft.ui.xaml.input.KeyboardAccelerator
import microsoft.ui.xaml.input.KeyboardAcceleratorInvokedEventArgs
import microsoft.ui.xaml.input.KeyboardAcceleratorPlacementMode
import microsoft.ui.xaml.input.KeyboardNavigationMode
import microsoft.ui.xaml.input.LosingFocusEventArgs
import microsoft.ui.xaml.input.ManipulationCompletedEventHandler
import microsoft.ui.xaml.input.ManipulationDeltaEventHandler
import microsoft.ui.xaml.input.ManipulationInertiaStartingEventHandler
import microsoft.ui.xaml.input.ManipulationModes
import microsoft.ui.xaml.input.ManipulationStartedEventHandler
import microsoft.ui.xaml.input.ManipulationStartingEventHandler
import microsoft.ui.xaml.input.NoFocusCandidateFoundEventArgs
import microsoft.ui.xaml.input.Pointer
import microsoft.ui.xaml.input.PointerEventHandler
import microsoft.ui.xaml.input.ProcessKeyboardAcceleratorEventArgs
import microsoft.ui.xaml.input.RightTappedEventHandler
import microsoft.ui.xaml.input.TappedEventHandler
import microsoft.ui.xaml.input.XYFocusKeyboardNavigationMode
import microsoft.ui.xaml.input.XYFocusNavigationStrategy
import microsoft.ui.xaml.media.CacheMode
import microsoft.ui.xaml.media.ElementCompositeMode
import microsoft.ui.xaml.media.GeneralTransform
import microsoft.ui.xaml.media.Projection
import microsoft.ui.xaml.media.RectangleGeometry
import microsoft.ui.xaml.media.Shadow
import microsoft.ui.xaml.media.Transform
import microsoft.ui.xaml.media.XamlLight
import microsoft.ui.xaml.media.animation.TransitionCollection
import microsoft.ui.xaml.media.media3d.Transform3D
import windows.applicationmodel.datatransfer.DataPackageOperation
import windows.foundation.IAsyncOperation
import windows.foundation.Point
import windows.foundation.Rect
import windows.foundation.Size
import windows.foundation.TypedEventHandler
import windows.foundation.collections.IVector
import windows.foundation.collections.IVectorView
import windows.foundation.numerics.Matrix4x4
import windows.foundation.numerics.Vector2
import windows.foundation.numerics.Vector3

public open class UIElement(
  pointer: ComPtr,
) : DependencyObject(pointer),
    IUIElement,
    IUIElementProtected,
    IAnimationObject,
    IVisualElement,
    IVisualElement2 {
  private val backing_ProtectedCursor: RuntimeProperty<InputCursor> =
      RuntimeProperty<InputCursor>(InputCursor(ComPtr.NULL))

  override var protectedCursor: InputCursor
    get() {
      if (pointer.isNull) {
        return backing_ProtectedCursor.get()
      }
      return InputCursor(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_ProtectedCursor.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 7, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_AccessKey: RuntimeProperty<String> = RuntimeProperty<String>("")

  override var accessKey: String
    get() {
      if (pointer.isNull) {
        return backing_AccessKey.get()
      }
      return run {
            val value = PlatformComInterop.invokeHStringMethod(pointer, 58).getOrThrow()
            try {
              value.toKotlinString()
            } finally {
              value.close()
            }
          }
    }
    set(value) {
      if (pointer.isNull) {
        backing_AccessKey.set(value)
        return
      }
      PlatformComInterop.invokeStringSetter(pointer, 59, value).getOrThrow()
    }

  private val backing_AccessKeyScopeOwner: RuntimeProperty<DependencyObject> =
      RuntimeProperty<DependencyObject>(DependencyObject(ComPtr.NULL))

  override var accessKeyScopeOwner: DependencyObject
    get() {
      if (pointer.isNull) {
        return backing_AccessKeyScopeOwner.get()
      }
      return DependencyObject(PlatformComInterop.invokeObjectMethod(pointer, 56).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_AccessKeyScopeOwner.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 57, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_ActualOffset: RuntimeProperty<Vector3> =
      RuntimeProperty<Vector3>(Vector3.fromAbi(ComStructValue(Vector3.ABI_LAYOUT,
      ByteArray(Vector3.ABI_LAYOUT.byteSize))))

  override val actualOffset: Vector3
    get() {
      if (pointer.isNull) {
        return backing_ActualOffset.get()
      }
      return Vector3.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 107,
          Vector3.ABI_LAYOUT).getOrThrow())
    }

  private val backing_ActualSize: RuntimeProperty<Vector2> =
      RuntimeProperty<Vector2>(Vector2.fromAbi(ComStructValue(Vector2.ABI_LAYOUT,
      ByteArray(Vector2.ABI_LAYOUT.byteSize))))

  override val actualSize: Vector2
    get() {
      if (pointer.isNull) {
        return backing_ActualSize.get()
      }
      return Vector2.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 108,
          Vector2.ABI_LAYOUT).getOrThrow())
    }

  private val backing_AllowDrop: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var allowDrop: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_AllowDrop.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_AllowDrop.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 8, value.value).getOrThrow()
    }

  private val backing_CacheMode: RuntimeProperty<CacheMode> =
      RuntimeProperty<CacheMode>(CacheMode(ComPtr.NULL))

  override var cacheMode: CacheMode
    get() {
      if (pointer.isNull) {
        return backing_CacheMode.get()
      }
      return CacheMode(PlatformComInterop.invokeObjectMethod(pointer, 30).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_CacheMode.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 31, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_CanBeScrollAnchor: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var canBeScrollAnchor: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_CanBeScrollAnchor.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 50).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_CanBeScrollAnchor.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 51, value.value).getOrThrow()
    }

  private val backing_CanDrag: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var canDrag: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_CanDrag.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 36).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_CanDrag.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 37, value.value).getOrThrow()
    }

  private val backing_CenterPoint: RuntimeProperty<Vector3> =
      RuntimeProperty<Vector3>(Vector3.fromAbi(ComStructValue(Vector3.ABI_LAYOUT,
      ByteArray(Vector3.ABI_LAYOUT.byteSize))))

  override var centerPoint: Vector3
    get() {
      if (pointer.isNull) {
        return backing_CenterPoint.get()
      }
      return Vector3.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 103,
          Vector3.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_CenterPoint.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 104, value.toAbi()).getOrThrow()
    }

  private val backing_Clip: RuntimeProperty<RectangleGeometry> =
      RuntimeProperty<RectangleGeometry>(RectangleGeometry(ComPtr.NULL))

  override var clip: RectangleGeometry
    get() {
      if (pointer.isNull) {
        return backing_Clip.get()
      }
      return RectangleGeometry(PlatformComInterop.invokeObjectMethod(pointer, 11).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Clip.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 12, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_CompositeMode: RuntimeProperty<ElementCompositeMode> =
      RuntimeProperty<ElementCompositeMode>(ElementCompositeMode.fromValue(0))

  override var compositeMode: ElementCompositeMode
    get() {
      if (pointer.isNull) {
        return backing_CompositeMode.get()
      }
      return ElementCompositeMode.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          47).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_CompositeMode.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 48, value.value).getOrThrow()
    }

  private val backing_ContextFlyout: RuntimeProperty<FlyoutBase> =
      RuntimeProperty<FlyoutBase>(FlyoutBase(ComPtr.NULL))

  override var contextFlyout: FlyoutBase
    get() {
      if (pointer.isNull) {
        return backing_ContextFlyout.get()
      }
      return FlyoutBase(PlatformComInterop.invokeObjectMethod(pointer, 45).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_ContextFlyout.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 46, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_DesiredSize: RuntimeProperty<Size> =
      RuntimeProperty<Size>(Size.fromAbi(ComStructValue(Size.ABI_LAYOUT,
      ByteArray(Size.ABI_LAYOUT.byteSize))))

  override val desiredSize: Size
    get() {
      if (pointer.isNull) {
        return backing_DesiredSize.get()
      }
      return Size.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 6,
          Size.ABI_LAYOUT).getOrThrow())
    }

  private val backing_ExitDisplayModeOnAccessKeyInvoked: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var exitDisplayModeOnAccessKeyInvoked: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_ExitDisplayModeOnAccessKeyInvoked.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 52).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_ExitDisplayModeOnAccessKeyInvoked.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 53, value.value).getOrThrow()
    }

  private val backing_FocusState: RuntimeProperty<FocusState> =
      RuntimeProperty<FocusState>(FocusState.fromValue(0))

  override val focusState: FocusState
    get() {
      if (pointer.isNull) {
        return backing_FocusState.get()
      }
      return FocusState.fromValue(PlatformComInterop.invokeInt32Method(pointer, 115).getOrThrow())
    }

  private val backing_HighContrastAdjustment: RuntimeProperty<ElementHighContrastAdjustment> =
      RuntimeProperty<ElementHighContrastAdjustment>(ElementHighContrastAdjustment.fromValue(0u))

  override var highContrastAdjustment: ElementHighContrastAdjustment
    get() {
      if (pointer.isNull) {
        return backing_HighContrastAdjustment.get()
      }
      return ElementHighContrastAdjustment.fromValue(PlatformComInterop.invokeUInt32Method(pointer,
          83).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_HighContrastAdjustment.set(value)
        return
      }
      PlatformComInterop.invokeUInt32Setter(pointer, 84, value.value).getOrThrow()
    }

  private val backing_IsAccessKeyScope: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var isAccessKeyScope: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsAccessKeyScope.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 54).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsAccessKeyScope.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 55, value.value).getOrThrow()
    }

  private val backing_IsDoubleTapEnabled: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var isDoubleTapEnabled: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsDoubleTapEnabled.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 34).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsDoubleTapEnabled.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 35, value.value).getOrThrow()
    }

  private val backing_IsHitTestVisible: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var isHitTestVisible: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsHitTestVisible.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 21).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsHitTestVisible.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 22, value.value).getOrThrow()
    }

  private val backing_IsHoldingEnabled: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var isHoldingEnabled: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsHoldingEnabled.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 40).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsHoldingEnabled.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 41, value.value).getOrThrow()
    }

  private val backing_IsRightTapEnabled: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var isRightTapEnabled: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsRightTapEnabled.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 38).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsRightTapEnabled.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 39, value.value).getOrThrow()
    }

  private val backing_IsTabStop: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var isTabStop: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsTabStop.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 126).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsTabStop.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 127, value.value).getOrThrow()
    }

  private val backing_IsTapEnabled: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var isTapEnabled: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsTapEnabled.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 32).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsTapEnabled.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 33, value.value).getOrThrow()
    }

  private val backing_KeyTipHorizontalOffset: RuntimeProperty<Float64> =
      RuntimeProperty<Float64>(Float64(0.0))

  override var keyTipHorizontalOffset: Float64
    get() {
      if (pointer.isNull) {
        return backing_KeyTipHorizontalOffset.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 62).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_KeyTipHorizontalOffset.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 63, value.value).getOrThrow()
    }

  private val backing_KeyTipPlacementMode: RuntimeProperty<KeyTipPlacementMode> =
      RuntimeProperty<KeyTipPlacementMode>(KeyTipPlacementMode.fromValue(0))

  override var keyTipPlacementMode: KeyTipPlacementMode
    get() {
      if (pointer.isNull) {
        return backing_KeyTipPlacementMode.get()
      }
      return KeyTipPlacementMode.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          60).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_KeyTipPlacementMode.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 61, value.value).getOrThrow()
    }

  private val backing_KeyTipTarget: RuntimeProperty<DependencyObject> =
      RuntimeProperty<DependencyObject>(DependencyObject(ComPtr.NULL))

  override var keyTipTarget: DependencyObject
    get() {
      if (pointer.isNull) {
        return backing_KeyTipTarget.get()
      }
      return DependencyObject(PlatformComInterop.invokeObjectMethod(pointer, 66).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_KeyTipTarget.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 67, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_KeyTipVerticalOffset: RuntimeProperty<Float64> =
      RuntimeProperty<Float64>(Float64(0.0))

  override var keyTipVerticalOffset: Float64
    get() {
      if (pointer.isNull) {
        return backing_KeyTipVerticalOffset.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 64).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_KeyTipVerticalOffset.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 65, value.value).getOrThrow()
    }

  private val backing_KeyboardAcceleratorPlacementMode:
      RuntimeProperty<KeyboardAcceleratorPlacementMode> =
      RuntimeProperty<KeyboardAcceleratorPlacementMode>(KeyboardAcceleratorPlacementMode.fromValue(0))

  override var keyboardAcceleratorPlacementMode: KeyboardAcceleratorPlacementMode
    get() {
      if (pointer.isNull) {
        return backing_KeyboardAcceleratorPlacementMode.get()
      }
      return KeyboardAcceleratorPlacementMode.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          81).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_KeyboardAcceleratorPlacementMode.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 82, value.value).getOrThrow()
    }

  private val backing_KeyboardAcceleratorPlacementTarget: RuntimeProperty<DependencyObject> =
      RuntimeProperty<DependencyObject>(DependencyObject(ComPtr.NULL))

  override var keyboardAcceleratorPlacementTarget: DependencyObject
    get() {
      if (pointer.isNull) {
        return backing_KeyboardAcceleratorPlacementTarget.get()
      }
      return DependencyObject(PlatformComInterop.invokeObjectMethod(pointer, 79).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_KeyboardAcceleratorPlacementTarget.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 80, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_ManipulationMode: RuntimeProperty<ManipulationModes> =
      RuntimeProperty<ManipulationModes>(ManipulationModes.fromValue(0u))

  override var manipulationMode: ManipulationModes
    get() {
      if (pointer.isNull) {
        return backing_ManipulationMode.get()
      }
      return ManipulationModes.fromValue(PlatformComInterop.invokeUInt32Method(pointer,
          42).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_ManipulationMode.set(value)
        return
      }
      PlatformComInterop.invokeUInt32Setter(pointer, 43, value.value).getOrThrow()
    }

  private val backing_Opacity: RuntimeProperty<Float64> = RuntimeProperty<Float64>(Float64(0.0))

  override var opacity: Float64
    get() {
      if (pointer.isNull) {
        return backing_Opacity.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 9).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Opacity.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 10, value.value).getOrThrow()
    }

  private val backing_OpacityTransition: RuntimeProperty<ScalarTransition> =
      RuntimeProperty<ScalarTransition>(ScalarTransition(ComPtr.NULL))

  override var opacityTransition: ScalarTransition
    get() {
      if (pointer.isNull) {
        return backing_OpacityTransition.get()
      }
      return ScalarTransition(PlatformComInterop.invokeObjectMethod(pointer, 87).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_OpacityTransition.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 88, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_Projection: RuntimeProperty<Projection> =
      RuntimeProperty<Projection>(Projection(ComPtr.NULL))

  override var projection: Projection
    get() {
      if (pointer.isNull) {
        return backing_Projection.get()
      }
      return Projection(PlatformComInterop.invokeObjectMethod(pointer, 15).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Projection.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 16, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_RasterizationScale: RuntimeProperty<Float64> =
      RuntimeProperty<Float64>(Float64(0.0))

  override var rasterizationScale: Float64
    get() {
      if (pointer.isNull) {
        return backing_RasterizationScale.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 113).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_RasterizationScale.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 114, value.value).getOrThrow()
    }

  private val backing_RenderSize: RuntimeProperty<Size> =
      RuntimeProperty<Size>(Size.fromAbi(ComStructValue(Size.ABI_LAYOUT,
      ByteArray(Size.ABI_LAYOUT.byteSize))))

  override val renderSize: Size
    get() {
      if (pointer.isNull) {
        return backing_RenderSize.get()
      }
      return Size.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 25,
          Size.ABI_LAYOUT).getOrThrow())
    }

  private val backing_RenderTransform: RuntimeProperty<Transform> =
      RuntimeProperty<Transform>(Transform(ComPtr.NULL))

  override var renderTransform: Transform
    get() {
      if (pointer.isNull) {
        return backing_RenderTransform.get()
      }
      return Transform(PlatformComInterop.invokeObjectMethod(pointer, 13).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_RenderTransform.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 14, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_RenderTransformOrigin: RuntimeProperty<Point> =
      RuntimeProperty<Point>(Point.fromAbi(ComStructValue(Point.ABI_LAYOUT,
      ByteArray(Point.ABI_LAYOUT.byteSize))))

  override var renderTransformOrigin: Point
    get() {
      if (pointer.isNull) {
        return backing_RenderTransformOrigin.get()
      }
      return Point.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 19,
          Point.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_RenderTransformOrigin.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 20, value.toAbi()).getOrThrow()
    }

  private val backing_Rotation: RuntimeProperty<Float32> = RuntimeProperty<Float32>(Float32(0f))

  override var rotation: Float32
    get() {
      if (pointer.isNull) {
        return backing_Rotation.get()
      }
      return Float32(PlatformComInterop.invokeFloat32Method(pointer, 93).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Rotation.set(value)
        return
      }
      PlatformComInterop.invokeFloat32Setter(pointer, 94, value.value).getOrThrow()
    }

  private val backing_RotationAxis: RuntimeProperty<Vector3> =
      RuntimeProperty<Vector3>(Vector3.fromAbi(ComStructValue(Vector3.ABI_LAYOUT,
      ByteArray(Vector3.ABI_LAYOUT.byteSize))))

  override var rotationAxis: Vector3
    get() {
      if (pointer.isNull) {
        return backing_RotationAxis.get()
      }
      return Vector3.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 105,
          Vector3.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_RotationAxis.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 106, value.toAbi()).getOrThrow()
    }

  private val backing_RotationTransition: RuntimeProperty<ScalarTransition> =
      RuntimeProperty<ScalarTransition>(ScalarTransition(ComPtr.NULL))

  override var rotationTransition: ScalarTransition
    get() {
      if (pointer.isNull) {
        return backing_RotationTransition.get()
      }
      return ScalarTransition(PlatformComInterop.invokeObjectMethod(pointer, 95).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_RotationTransition.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 96, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_Scale: RuntimeProperty<Vector3> =
      RuntimeProperty<Vector3>(Vector3.fromAbi(ComStructValue(Vector3.ABI_LAYOUT,
      ByteArray(Vector3.ABI_LAYOUT.byteSize))))

  override var scale: Vector3
    get() {
      if (pointer.isNull) {
        return backing_Scale.get()
      }
      return Vector3.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 97,
          Vector3.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Scale.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 98, value.toAbi()).getOrThrow()
    }

  private val backing_ScaleTransition: RuntimeProperty<Vector3Transition> =
      RuntimeProperty<Vector3Transition>(Vector3Transition(ComPtr.NULL))

  override var scaleTransition: Vector3Transition
    get() {
      if (pointer.isNull) {
        return backing_ScaleTransition.get()
      }
      return Vector3Transition(PlatformComInterop.invokeObjectMethod(pointer, 99).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_ScaleTransition.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 100, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_Shadow: RuntimeProperty<Shadow> = RuntimeProperty<Shadow>(Shadow(ComPtr.NULL))

  override var shadow: Shadow
    get() {
      if (pointer.isNull) {
        return backing_Shadow.get()
      }
      return Shadow(PlatformComInterop.invokeObjectMethod(pointer, 111).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Shadow.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 112, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_TabFocusNavigation: RuntimeProperty<KeyboardNavigationMode> =
      RuntimeProperty<KeyboardNavigationMode>(KeyboardNavigationMode.fromValue(0))

  override var tabFocusNavigation: KeyboardNavigationMode
    get() {
      if (pointer.isNull) {
        return backing_TabFocusNavigation.get()
      }
      return KeyboardNavigationMode.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          85).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_TabFocusNavigation.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 86, value.value).getOrThrow()
    }

  private val backing_TabIndex: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  override var tabIndex: Int32
    get() {
      if (pointer.isNull) {
        return backing_TabIndex.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 128).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_TabIndex.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 129, value.value).getOrThrow()
    }

  private val backing_Transform3D: RuntimeProperty<Transform3D> =
      RuntimeProperty<Transform3D>(Transform3D(ComPtr.NULL))

  override var transform3D: Transform3D
    get() {
      if (pointer.isNull) {
        return backing_Transform3D.get()
      }
      return Transform3D(PlatformComInterop.invokeObjectMethod(pointer, 17).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Transform3D.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 18, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_TransformMatrix: RuntimeProperty<Matrix4x4> =
      RuntimeProperty<Matrix4x4>(Matrix4x4.fromAbi(ComStructValue(Matrix4x4.ABI_LAYOUT,
      ByteArray(Matrix4x4.ABI_LAYOUT.byteSize))))

  override var transformMatrix: Matrix4x4
    get() {
      if (pointer.isNull) {
        return backing_TransformMatrix.get()
      }
      return Matrix4x4.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 101,
          Matrix4x4.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_TransformMatrix.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 102, value.toAbi()).getOrThrow()
    }

  private val backing_Transitions: RuntimeProperty<TransitionCollection> =
      RuntimeProperty<TransitionCollection>(TransitionCollection(ComPtr.NULL))

  override var transitions: TransitionCollection
    get() {
      if (pointer.isNull) {
        return backing_Transitions.get()
      }
      return TransitionCollection(PlatformComInterop.invokeObjectMethod(pointer, 28).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Transitions.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 29, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_Translation: RuntimeProperty<Vector3> =
      RuntimeProperty<Vector3>(Vector3.fromAbi(ComStructValue(Vector3.ABI_LAYOUT,
      ByteArray(Vector3.ABI_LAYOUT.byteSize))))

  override var translation: Vector3
    get() {
      if (pointer.isNull) {
        return backing_Translation.get()
      }
      return Vector3.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 89,
          Vector3.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Translation.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 90, value.toAbi()).getOrThrow()
    }

  private val backing_TranslationTransition: RuntimeProperty<Vector3Transition> =
      RuntimeProperty<Vector3Transition>(Vector3Transition(ComPtr.NULL))

  override var translationTransition: Vector3Transition
    get() {
      if (pointer.isNull) {
        return backing_TranslationTransition.get()
      }
      return Vector3Transition(PlatformComInterop.invokeObjectMethod(pointer, 91).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_TranslationTransition.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 92, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_UseLayoutRounding: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var useLayoutRounding: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_UseLayoutRounding.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 26).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_UseLayoutRounding.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 27, value.value).getOrThrow()
    }

  private val backing_UseSystemFocusVisuals: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var useSystemFocusVisuals: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_UseSystemFocusVisuals.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 116).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_UseSystemFocusVisuals.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 117, value.value).getOrThrow()
    }

  private val backing_Visibility: RuntimeProperty<Visibility> =
      RuntimeProperty<Visibility>(Visibility.fromValue(0))

  override var visibility: Visibility
    get() {
      if (pointer.isNull) {
        return backing_Visibility.get()
      }
      return Visibility.fromValue(PlatformComInterop.invokeInt32Method(pointer, 23).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Visibility.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 24, value.value).getOrThrow()
    }

  private val backing_XYFocusDown: RuntimeProperty<DependencyObject> =
      RuntimeProperty<DependencyObject>(DependencyObject(ComPtr.NULL))

  override var xYFocusDown: DependencyObject
    get() {
      if (pointer.isNull) {
        return backing_XYFocusDown.get()
      }
      return DependencyObject(PlatformComInterop.invokeObjectMethod(pointer, 124).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_XYFocusDown.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 125, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_XYFocusDownNavigationStrategy: RuntimeProperty<XYFocusNavigationStrategy> =
      RuntimeProperty<XYFocusNavigationStrategy>(XYFocusNavigationStrategy.fromValue(0))

  override var xYFocusDownNavigationStrategy: XYFocusNavigationStrategy
    get() {
      if (pointer.isNull) {
        return backing_XYFocusDownNavigationStrategy.get()
      }
      return XYFocusNavigationStrategy.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          72).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_XYFocusDownNavigationStrategy.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 73, value.value).getOrThrow()
    }

  private val backing_XYFocusKeyboardNavigation: RuntimeProperty<XYFocusKeyboardNavigationMode> =
      RuntimeProperty<XYFocusKeyboardNavigationMode>(XYFocusKeyboardNavigationMode.fromValue(0))

  override var xYFocusKeyboardNavigation: XYFocusKeyboardNavigationMode
    get() {
      if (pointer.isNull) {
        return backing_XYFocusKeyboardNavigation.get()
      }
      return XYFocusKeyboardNavigationMode.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          68).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_XYFocusKeyboardNavigation.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 69, value.value).getOrThrow()
    }

  private val backing_XYFocusLeft: RuntimeProperty<DependencyObject> =
      RuntimeProperty<DependencyObject>(DependencyObject(ComPtr.NULL))

  override var xYFocusLeft: DependencyObject
    get() {
      if (pointer.isNull) {
        return backing_XYFocusLeft.get()
      }
      return DependencyObject(PlatformComInterop.invokeObjectMethod(pointer, 118).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_XYFocusLeft.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 119, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_XYFocusLeftNavigationStrategy: RuntimeProperty<XYFocusNavigationStrategy> =
      RuntimeProperty<XYFocusNavigationStrategy>(XYFocusNavigationStrategy.fromValue(0))

  override var xYFocusLeftNavigationStrategy: XYFocusNavigationStrategy
    get() {
      if (pointer.isNull) {
        return backing_XYFocusLeftNavigationStrategy.get()
      }
      return XYFocusNavigationStrategy.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          74).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_XYFocusLeftNavigationStrategy.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 75, value.value).getOrThrow()
    }

  private val backing_XYFocusRight: RuntimeProperty<DependencyObject> =
      RuntimeProperty<DependencyObject>(DependencyObject(ComPtr.NULL))

  override var xYFocusRight: DependencyObject
    get() {
      if (pointer.isNull) {
        return backing_XYFocusRight.get()
      }
      return DependencyObject(PlatformComInterop.invokeObjectMethod(pointer, 120).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_XYFocusRight.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 121, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_XYFocusRightNavigationStrategy: RuntimeProperty<XYFocusNavigationStrategy> =
      RuntimeProperty<XYFocusNavigationStrategy>(XYFocusNavigationStrategy.fromValue(0))

  override var xYFocusRightNavigationStrategy: XYFocusNavigationStrategy
    get() {
      if (pointer.isNull) {
        return backing_XYFocusRightNavigationStrategy.get()
      }
      return XYFocusNavigationStrategy.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          76).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_XYFocusRightNavigationStrategy.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 77, value.value).getOrThrow()
    }

  private val backing_XYFocusUp: RuntimeProperty<DependencyObject> =
      RuntimeProperty<DependencyObject>(DependencyObject(ComPtr.NULL))

  override var xYFocusUp: DependencyObject
    get() {
      if (pointer.isNull) {
        return backing_XYFocusUp.get()
      }
      return DependencyObject(PlatformComInterop.invokeObjectMethod(pointer, 122).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_XYFocusUp.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 123, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_XYFocusUpNavigationStrategy: RuntimeProperty<XYFocusNavigationStrategy> =
      RuntimeProperty<XYFocusNavigationStrategy>(XYFocusNavigationStrategy.fromValue(0))

  override var xYFocusUpNavigationStrategy: XYFocusNavigationStrategy
    get() {
      if (pointer.isNull) {
        return backing_XYFocusUpNavigationStrategy.get()
      }
      return XYFocusNavigationStrategy.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          70).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_XYFocusUpNavigationStrategy.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 71, value.value).getOrThrow()
    }

  private val backing_XamlRoot: RuntimeProperty<XamlRoot> =
      RuntimeProperty<XamlRoot>(XamlRoot(ComPtr.NULL))

  override var xamlRoot: XamlRoot
    get() {
      if (pointer.isNull) {
        return backing_XamlRoot.get()
      }
      return XamlRoot(PlatformComInterop.invokeObjectMethod(pointer, 109).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_XamlRoot.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 110, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val dragStartingEventSlot: DragStartingEvent = DragStartingEvent()

  public val dragStartingEvent: DragStartingEvent
    get() = dragStartingEventSlot

  private val dropCompletedEventSlot: DropCompletedEvent = DropCompletedEvent()

  public val dropCompletedEvent: DropCompletedEvent
    get() = dropCompletedEventSlot

  private val characterReceivedEventSlot: CharacterReceivedEvent = CharacterReceivedEvent()

  public val characterReceivedEvent: CharacterReceivedEvent
    get() = characterReceivedEventSlot

  private val contextRequestedEventSlot: ContextRequestedEvent = ContextRequestedEvent()

  public val contextRequestedEvent: ContextRequestedEvent
    get() = contextRequestedEventSlot

  private val contextCanceledEventSlot: ContextCanceledEvent = ContextCanceledEvent()

  public val contextCanceledEvent: ContextCanceledEvent
    get() = contextCanceledEventSlot

  private val accessKeyDisplayRequestedEventSlot: AccessKeyDisplayRequestedEvent =
      AccessKeyDisplayRequestedEvent()

  public val accessKeyDisplayRequestedEvent: AccessKeyDisplayRequestedEvent
    get() = accessKeyDisplayRequestedEventSlot

  private val accessKeyDisplayDismissedEventSlot: AccessKeyDisplayDismissedEvent =
      AccessKeyDisplayDismissedEvent()

  public val accessKeyDisplayDismissedEvent: AccessKeyDisplayDismissedEvent
    get() = accessKeyDisplayDismissedEventSlot

  private val accessKeyInvokedEventSlot: AccessKeyInvokedEvent = AccessKeyInvokedEvent()

  public val accessKeyInvokedEvent: AccessKeyInvokedEvent
    get() = accessKeyInvokedEventSlot

  private val processKeyboardAcceleratorsEventSlot: ProcessKeyboardAcceleratorsEvent =
      ProcessKeyboardAcceleratorsEvent()

  public val processKeyboardAcceleratorsEvent: ProcessKeyboardAcceleratorsEvent
    get() = processKeyboardAcceleratorsEventSlot

  private val gettingFocusEventSlot: GettingFocusEvent = GettingFocusEvent()

  public val gettingFocusEvent: GettingFocusEvent
    get() = gettingFocusEventSlot

  private val losingFocusEventSlot: LosingFocusEvent = LosingFocusEvent()

  public val losingFocusEvent: LosingFocusEvent
    get() = losingFocusEventSlot

  private val noFocusCandidateFoundEventSlot: NoFocusCandidateFoundEvent =
      NoFocusCandidateFoundEvent()

  public val noFocusCandidateFoundEvent: NoFocusCandidateFoundEvent
    get() = noFocusCandidateFoundEventSlot

  private val bringIntoViewRequestedEventSlot: BringIntoViewRequestedEvent =
      BringIntoViewRequestedEvent()

  public val bringIntoViewRequestedEvent: BringIntoViewRequestedEvent
    get() = bringIntoViewRequestedEventSlot

  public fun onCreateAutomationPeer(): AutomationPeer {
    if (pointer.isNull) {
      error("Null runtime object pointer: OnCreateAutomationPeer")
    }
    return AutomationPeer(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
  }

  public fun onDisconnectVisualChildren() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 7).getOrThrow()
  }

  public fun findSubElementsForTouchTargeting(point: Point, boundingRect: Rect):
      Iterable<Iterable<Point>> {
    if (pointer.isNull) {
      error("Null runtime object pointer: FindSubElementsForTouchTargeting")
    }
    return Iterable<Iterable<Point>>.from(Inspectable(PlatformComInterop.invokeObjectMethodWithArgs(pointer,
        8, point.toAbi(), boundingRect.toAbi()).getOrThrow()))
  }

  public fun getChildrenInTabFocusOrder(): Iterable<DependencyObject> {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetChildrenInTabFocusOrder")
    }
    return Iterable<DependencyObject>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        9).getOrThrow()))
  }

  public fun onKeyboardAcceleratorInvoked(args: KeyboardAcceleratorInvokedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 10, projectedObjectArgumentPointer(args,
        "Microsoft.UI.Xaml.Input.KeyboardAcceleratorInvokedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.KeyboardAcceleratorInvokedEventArgs;{62c9fdb0-b574-527d-97eb-5c7f674441e0})")).getOrThrow()
  }

  public fun onProcessKeyboardAccelerators(args: ProcessKeyboardAcceleratorEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 11, projectedObjectArgumentPointer(args,
        "Microsoft.UI.Xaml.Input.ProcessKeyboardAcceleratorEventArgs",
        "rc(Microsoft.UI.Xaml.Input.ProcessKeyboardAcceleratorEventArgs;{9be0d058-3d26-5811-b50a-3bb80ca766c9})")).getOrThrow()
  }

  public fun onBringIntoViewRequested(e: BringIntoViewRequestedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 12, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.BringIntoViewRequestedEventArgs",
        "rc(Microsoft.UI.Xaml.BringIntoViewRequestedEventArgs;{807de8f9-b1dc-5a63-8101-5ee966841a27})")).getOrThrow()
  }

  public fun populatePropertyInfoOverride(propertyName: String,
      animationPropertyInfo: AnimationPropertyInfo) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithStringAndObjectArgs(pointer, 13, propertyName,
        projectedObjectArgumentPointer(animationPropertyInfo,
        "Microsoft.UI.Composition.AnimationPropertyInfo",
        "rc(Microsoft.UI.Composition.AnimationPropertyInfo;{3d721a2b-9ccd-57bd-b6c2-ce9e04ae3606})")).getOrThrow()
  }

  override fun populatePropertyInfo(propertyName: String, propertyInfo: AnimationPropertyInfo) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithStringAndObjectArgs(pointer, 6, propertyName,
        projectedObjectArgumentPointer(propertyInfo,
        "Microsoft.UI.Composition.AnimationPropertyInfo",
        "rc(Microsoft.UI.Composition.AnimationPropertyInfo;{3d721a2b-9ccd-57bd-b6c2-ce9e04ae3606})")).getOrThrow()
  }

  override fun getVisualInternal(): Visual {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetVisualInternal")
    }
    return Visual(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
  }

  override fun get_PointerCaptures(): IVectorView<Pointer> {
    if (pointer.isNull) {
      error("Null runtime object pointer: get_PointerCaptures")
    }
    return IVectorView<Pointer>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        44).getOrThrow()))
  }

  override fun get_Lights(): IVector<XamlLight> {
    if (pointer.isNull) {
      error("Null runtime object pointer: get_Lights")
    }
    return IVector<XamlLight>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        49).getOrThrow()))
  }

  override fun get_KeyboardAccelerators(): IVector<KeyboardAccelerator> {
    if (pointer.isNull) {
      error("Null runtime object pointer: get_KeyboardAccelerators")
    }
    return IVector<KeyboardAccelerator>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        78).getOrThrow()))
  }

  override fun add_KeyUp(handler: KeyEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        130, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.KeyEventHandler",
        "delegate({db68e7cc-9a2b-527d-9989-25284daccc03})")).getOrThrow())
  }

  override fun remove_KeyUp(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 131, token.toAbi()).getOrThrow()
  }

  override fun add_KeyDown(handler: KeyEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        132, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.KeyEventHandler",
        "delegate({db68e7cc-9a2b-527d-9989-25284daccc03})")).getOrThrow())
  }

  override fun remove_KeyDown(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 133, token.toAbi()).getOrThrow()
  }

  override fun add_GotFocus(handler: RoutedEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        134, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.RoutedEventHandler",
        "delegate({dae23d85-69ca-5bdf-805b-6161a3a215cc})")).getOrThrow())
  }

  override fun remove_GotFocus(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 135, token.toAbi()).getOrThrow()
  }

  override fun add_LostFocus(handler: RoutedEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        136, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.RoutedEventHandler",
        "delegate({dae23d85-69ca-5bdf-805b-6161a3a215cc})")).getOrThrow())
  }

  override fun remove_LostFocus(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 137, token.toAbi()).getOrThrow()
  }

  override fun add_DragStarting(handler: TypedEventHandler<UIElement, DragStartingEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        138, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.DragStartingEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.DragStartingEventArgs;{ad17bace-9613-5666-a31b-79a73fba77cf}))")).getOrThrow())
  }

  override fun remove_DragStarting(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 139, token.toAbi()).getOrThrow()
  }

  override fun add_DropCompleted(handler: TypedEventHandler<UIElement, DropCompletedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        140, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.DropCompletedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.DropCompletedEventArgs;{e700082d-c640-5d44-b23a-f213dfbeb245}))")).getOrThrow())
  }

  override fun remove_DropCompleted(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 141, token.toAbi()).getOrThrow()
  }

  override
      fun add_CharacterReceived(handler: TypedEventHandler<UIElement, CharacterReceivedRoutedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        142, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.Input.CharacterReceivedRoutedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.Input.CharacterReceivedRoutedEventArgs;{e26ca5bb-34c3-5c1e-9a16-00b80b07a899}))")).getOrThrow())
  }

  override fun remove_CharacterReceived(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 143, token.toAbi()).getOrThrow()
  }

  override fun add_DragEnter(handler: DragEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        144, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.DragEventHandler",
        "delegate({277afc83-cb67-56c8-b601-1b9c0f1c3d32})")).getOrThrow())
  }

  override fun remove_DragEnter(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 145, token.toAbi()).getOrThrow()
  }

  override fun add_DragLeave(handler: DragEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        146, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.DragEventHandler",
        "delegate({277afc83-cb67-56c8-b601-1b9c0f1c3d32})")).getOrThrow())
  }

  override fun remove_DragLeave(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 147, token.toAbi()).getOrThrow()
  }

  override fun add_DragOver(handler: DragEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        148, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.DragEventHandler",
        "delegate({277afc83-cb67-56c8-b601-1b9c0f1c3d32})")).getOrThrow())
  }

  override fun remove_DragOver(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 149, token.toAbi()).getOrThrow()
  }

  override fun add_Drop(handler: DragEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        150, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.DragEventHandler",
        "delegate({277afc83-cb67-56c8-b601-1b9c0f1c3d32})")).getOrThrow())
  }

  override fun remove_Drop(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 151, token.toAbi()).getOrThrow()
  }

  override fun add_PointerPressed(handler: PointerEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        152, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.PointerEventHandler",
        "delegate({a48a71e1-8bb4-5597-9e31-903a3f6a04fb})")).getOrThrow())
  }

  override fun remove_PointerPressed(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 153, token.toAbi()).getOrThrow()
  }

  override fun add_PointerMoved(handler: PointerEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        154, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.PointerEventHandler",
        "delegate({a48a71e1-8bb4-5597-9e31-903a3f6a04fb})")).getOrThrow())
  }

  override fun remove_PointerMoved(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 155, token.toAbi()).getOrThrow()
  }

  override fun add_PointerReleased(handler: PointerEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        156, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.PointerEventHandler",
        "delegate({a48a71e1-8bb4-5597-9e31-903a3f6a04fb})")).getOrThrow())
  }

  override fun remove_PointerReleased(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 157, token.toAbi()).getOrThrow()
  }

  override fun add_PointerEntered(handler: PointerEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        158, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.PointerEventHandler",
        "delegate({a48a71e1-8bb4-5597-9e31-903a3f6a04fb})")).getOrThrow())
  }

  override fun remove_PointerEntered(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 159, token.toAbi()).getOrThrow()
  }

  override fun add_PointerExited(handler: PointerEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        160, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.PointerEventHandler",
        "delegate({a48a71e1-8bb4-5597-9e31-903a3f6a04fb})")).getOrThrow())
  }

  override fun remove_PointerExited(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 161, token.toAbi()).getOrThrow()
  }

  override fun add_PointerCaptureLost(handler: PointerEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        162, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.PointerEventHandler",
        "delegate({a48a71e1-8bb4-5597-9e31-903a3f6a04fb})")).getOrThrow())
  }

  override fun remove_PointerCaptureLost(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 163, token.toAbi()).getOrThrow()
  }

  override fun add_PointerCanceled(handler: PointerEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        164, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.PointerEventHandler",
        "delegate({a48a71e1-8bb4-5597-9e31-903a3f6a04fb})")).getOrThrow())
  }

  override fun remove_PointerCanceled(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 165, token.toAbi()).getOrThrow()
  }

  override fun add_PointerWheelChanged(handler: PointerEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        166, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.PointerEventHandler",
        "delegate({a48a71e1-8bb4-5597-9e31-903a3f6a04fb})")).getOrThrow())
  }

  override fun remove_PointerWheelChanged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 167, token.toAbi()).getOrThrow()
  }

  override fun add_Tapped(handler: TappedEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        168, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.TappedEventHandler",
        "delegate({b60074f3-125b-534e-8f9c-9769bd3f0f64})")).getOrThrow())
  }

  override fun remove_Tapped(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 169, token.toAbi()).getOrThrow()
  }

  override fun add_DoubleTapped(handler: DoubleTappedEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        170, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.DoubleTappedEventHandler",
        "delegate({f7a501b9-e277-5611-87b0-0e0607622183})")).getOrThrow())
  }

  override fun remove_DoubleTapped(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 171, token.toAbi()).getOrThrow()
  }

  override fun add_Holding(handler: HoldingEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        172, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.HoldingEventHandler",
        "delegate({fe23c5bd-4984-56b6-b92b-fc9d1216b24e})")).getOrThrow())
  }

  override fun remove_Holding(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 173, token.toAbi()).getOrThrow()
  }

  override
      fun add_ContextRequested(handler: TypedEventHandler<UIElement, ContextRequestedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        174, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.Input.ContextRequestedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.Input.ContextRequestedEventArgs;{bcedcb98-77b5-53c0-802e-fd52f3806e51}))")).getOrThrow())
  }

  override fun remove_ContextRequested(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 175, token.toAbi()).getOrThrow()
  }

  override fun add_ContextCanceled(handler: TypedEventHandler<UIElement, RoutedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        176, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.RoutedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.RoutedEventArgs;{0908c407-1c7d-5de3-9c50-d971c62ec8ec}))")).getOrThrow())
  }

  override fun remove_ContextCanceled(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 177, token.toAbi()).getOrThrow()
  }

  override fun add_RightTapped(handler: RightTappedEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        178, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.RightTappedEventHandler",
        "delegate({5070e32f-3dc7-56cf-8fdd-de1b40d0b472})")).getOrThrow())
  }

  override fun remove_RightTapped(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 179, token.toAbi()).getOrThrow()
  }

  override fun add_ManipulationStarting(handler: ManipulationStartingEventHandler):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        180, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.ManipulationStartingEventHandler",
        "delegate({44f528f1-f0e4-505c-a0bb-0c4839b29df5})")).getOrThrow())
  }

  override fun remove_ManipulationStarting(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 181, token.toAbi()).getOrThrow()
  }

  override fun add_ManipulationInertiaStarting(handler: ManipulationInertiaStartingEventHandler):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        182, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.ManipulationInertiaStartingEventHandler",
        "delegate({5de296bd-6f1c-5f60-9180-10705282576c})")).getOrThrow())
  }

  override fun remove_ManipulationInertiaStarting(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 183, token.toAbi()).getOrThrow()
  }

  override fun add_ManipulationStarted(handler: ManipulationStartedEventHandler):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        184, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.ManipulationStartedEventHandler",
        "delegate({41060669-304c-53ac-9d43-bc311235aae4})")).getOrThrow())
  }

  override fun remove_ManipulationStarted(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 185, token.toAbi()).getOrThrow()
  }

  override fun add_ManipulationDelta(handler: ManipulationDeltaEventHandler):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        186, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.ManipulationDeltaEventHandler",
        "delegate({83f2d4ce-105f-5392-a38a-b7467b7c2ea5})")).getOrThrow())
  }

  override fun remove_ManipulationDelta(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 187, token.toAbi()).getOrThrow()
  }

  override fun add_ManipulationCompleted(handler: ManipulationCompletedEventHandler):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        188, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.ManipulationCompletedEventHandler",
        "delegate({d51df8db-71cd-5bfd-8426-767218ee55ec})")).getOrThrow())
  }

  override fun remove_ManipulationCompleted(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 189, token.toAbi()).getOrThrow()
  }

  override
      fun add_AccessKeyDisplayRequested(handler: TypedEventHandler<UIElement, AccessKeyDisplayRequestedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        190, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.Input.AccessKeyDisplayRequestedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.Input.AccessKeyDisplayRequestedEventArgs;{c4ed84d8-2b27-59b1-9cf0-7f9164de58cb}))")).getOrThrow())
  }

  override fun remove_AccessKeyDisplayRequested(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 191, token.toAbi()).getOrThrow()
  }

  override
      fun add_AccessKeyDisplayDismissed(handler: TypedEventHandler<UIElement, AccessKeyDisplayDismissedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        192, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.Input.AccessKeyDisplayDismissedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.Input.AccessKeyDisplayDismissedEventArgs;{125a83d8-7f86-5ea9-9063-b9407e644587}))")).getOrThrow())
  }

  override fun remove_AccessKeyDisplayDismissed(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 193, token.toAbi()).getOrThrow()
  }

  override
      fun add_AccessKeyInvoked(handler: TypedEventHandler<UIElement, AccessKeyInvokedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        194, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.Input.AccessKeyInvokedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.Input.AccessKeyInvokedEventArgs;{d00c11a4-f9fb-5707-9692-98b80bb8546d}))")).getOrThrow())
  }

  override fun remove_AccessKeyInvoked(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 195, token.toAbi()).getOrThrow()
  }

  override
      fun add_ProcessKeyboardAccelerators(handler: TypedEventHandler<UIElement, ProcessKeyboardAcceleratorEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        196, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.Input.ProcessKeyboardAcceleratorEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.Input.ProcessKeyboardAcceleratorEventArgs;{9be0d058-3d26-5811-b50a-3bb80ca766c9}))")).getOrThrow())
  }

  override fun remove_ProcessKeyboardAccelerators(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 197, token.toAbi()).getOrThrow()
  }

  override fun add_GettingFocus(handler: TypedEventHandler<UIElement, GettingFocusEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        198, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.Input.GettingFocusEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.Input.GettingFocusEventArgs;{37fd3af0-bd3c-5bf5-a9cd-71a1e87af950}))")).getOrThrow())
  }

  override fun remove_GettingFocus(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 199, token.toAbi()).getOrThrow()
  }

  override fun add_LosingFocus(handler: TypedEventHandler<UIElement, LosingFocusEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        200, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.Input.LosingFocusEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.Input.LosingFocusEventArgs;{fa0e5ffa-2b1b-52f8-bb66-e35f51e73cf3}))")).getOrThrow())
  }

  override fun remove_LosingFocus(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 201, token.toAbi()).getOrThrow()
  }

  override
      fun add_NoFocusCandidateFound(handler: TypedEventHandler<UIElement, NoFocusCandidateFoundEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        202, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.Input.NoFocusCandidateFoundEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.Input.NoFocusCandidateFoundEventArgs;{a2d7153a-cd2a-59cb-a574-ac82e30b9201}))")).getOrThrow())
  }

  override fun remove_NoFocusCandidateFound(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 203, token.toAbi()).getOrThrow()
  }

  override fun add_PreviewKeyDown(handler: KeyEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        204, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.KeyEventHandler",
        "delegate({db68e7cc-9a2b-527d-9989-25284daccc03})")).getOrThrow())
  }

  override fun remove_PreviewKeyDown(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 205, token.toAbi()).getOrThrow()
  }

  override fun add_PreviewKeyUp(handler: KeyEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        206, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Input.KeyEventHandler",
        "delegate({db68e7cc-9a2b-527d-9989-25284daccc03})")).getOrThrow())
  }

  override fun remove_PreviewKeyUp(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 207, token.toAbi()).getOrThrow()
  }

  override
      fun add_BringIntoViewRequested(handler: TypedEventHandler<UIElement, BringIntoViewRequestedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer,
        208, EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.UIElement, Microsoft.UI.Xaml.BringIntoViewRequestedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b});rc(Microsoft.UI.Xaml.BringIntoViewRequestedEventArgs;{807de8f9-b1dc-5a63-8101-5ee966841a27}))")).getOrThrow())
  }

  override fun remove_BringIntoViewRequested(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 209, token.toAbi()).getOrThrow()
  }

  override fun measure(availableSize: Size) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 210, availableSize.toAbi()).getOrThrow()
  }

  override fun arrange(finalRect: Rect) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 211, finalRect.toAbi()).getOrThrow()
  }

  override fun capturePointer(value: Pointer): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithObjectArg(pointer, 212,
        projectedObjectArgumentPointer(value, "Microsoft.UI.Xaml.Input.Pointer",
        "rc(Microsoft.UI.Xaml.Input.Pointer;{1f9afbf5-11a3-5e68-aa1b-72febfa0ab23})")).getOrThrow())
  }

  override fun releasePointerCapture(value: Pointer) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 213, projectedObjectArgumentPointer(value,
        "Microsoft.UI.Xaml.Input.Pointer",
        "rc(Microsoft.UI.Xaml.Input.Pointer;{1f9afbf5-11a3-5e68-aa1b-72febfa0ab23})")).getOrThrow()
  }

  override fun releasePointerCaptures() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 214).getOrThrow()
  }

  override fun removeHandler(routedEvent: RoutedEvent, handler: Inspectable) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 216,
        projectedObjectArgumentPointer(routedEvent, "Microsoft.UI.Xaml.RoutedEvent",
        "rc(Microsoft.UI.Xaml.RoutedEvent;{b2b432bc-efca-575e-9d2a-703f8b9c380f})"),
        projectedObjectArgumentPointer(handler, "Object", "cinterface(IInspectable)")).getOrThrow()
  }

  override fun transformToVisual(visual: UIElement): GeneralTransform {
    if (pointer.isNull) {
      error("Null runtime object pointer: TransformToVisual")
    }
    return GeneralTransform(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 217,
        projectedObjectArgumentPointer(visual, "Microsoft.UI.Xaml.UIElement",
        "rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b})")).getOrThrow())
  }

  override fun invalidateMeasure() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 218).getOrThrow()
  }

  override fun invalidateArrange() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 219).getOrThrow()
  }

  override fun updateLayout() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 220).getOrThrow()
  }

  override fun cancelDirectManipulations(): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 221).getOrThrow())
  }

  override fun startDragAsync(pointerPoint: PointerPoint): IAsyncOperation<DataPackageOperation> {
    if (pointer.isNull) {
      error("Null runtime object pointer: StartDragAsync")
    }
    return IAsyncOperation<DataPackageOperation>.from(Inspectable(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer,
        222, projectedObjectArgumentPointer(pointerPoint, "Microsoft.UI.Input.PointerPoint",
        "rc(Microsoft.UI.Input.PointerPoint;{0d430ee6-252c-59a4-b2a2-d44264dc6a40})")).getOrThrow()))
  }

  override fun startBringIntoView() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 223).getOrThrow()
  }

  override fun startBringIntoView(options: BringIntoViewOptions) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 224, projectedObjectArgumentPointer(options,
        "Microsoft.UI.Xaml.BringIntoViewOptions",
        "rc(Microsoft.UI.Xaml.BringIntoViewOptions;{eeb4a447-eb9e-5003-a479-b9e3a886b708})")).getOrThrow()
  }

  override fun tryInvokeKeyboardAccelerator(args: ProcessKeyboardAcceleratorEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 225, projectedObjectArgumentPointer(args,
        "Microsoft.UI.Xaml.Input.ProcessKeyboardAcceleratorEventArgs",
        "rc(Microsoft.UI.Xaml.Input.ProcessKeyboardAcceleratorEventArgs;{9be0d058-3d26-5811-b50a-3bb80ca766c9})")).getOrThrow()
  }

  override fun focus(value: FocusState): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithInt32Arg(pointer, 226,
        value.value).getOrThrow())
  }

  override fun startAnimation(animation: ICompositionAnimationBase) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 227, projectedObjectArgumentPointer(animation,
        "Microsoft.UI.Composition.ICompositionAnimationBase",
        "{a77c0e5a-f059-4e85-bcef-c068694cec78}")).getOrThrow()
  }

  override fun stopAnimation(animation: ICompositionAnimationBase) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 228, projectedObjectArgumentPointer(animation,
        "Microsoft.UI.Composition.ICompositionAnimationBase",
        "{a77c0e5a-f059-4e85-bcef-c068694cec78}")).getOrThrow()
  }

  public inner class DragStartingEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<UIElement, DragStartingEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 138,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<UIElement, DragStartingEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, DragStartingEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), DragStartingEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<UIElement, DragStartingEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, DragStartingEventArgs) -> Unit): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator fun plusAssign(handler: TypedEventHandler<UIElement, DragStartingEventArgs>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<UIElement, DragStartingEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, DragStartingEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 139, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class DropCompletedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<UIElement, DropCompletedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 140,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<UIElement, DropCompletedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, DropCompletedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), DropCompletedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<UIElement, DropCompletedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, DropCompletedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator fun plusAssign(handler: TypedEventHandler<UIElement, DropCompletedEventArgs>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<UIElement, DropCompletedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, DropCompletedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 141, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class CharacterReceivedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<UIElement, CharacterReceivedRoutedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 142,
        handler.pointer).getOrThrow())

    public
        fun subscribeScoped(handler: TypedEventHandler<UIElement, CharacterReceivedRoutedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, CharacterReceivedRoutedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), CharacterReceivedRoutedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<UIElement, CharacterReceivedRoutedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, CharacterReceivedRoutedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<UIElement, CharacterReceivedRoutedEventArgs>) {
      subscribe(handler)
    }

    public operator
        fun invoke(handler: TypedEventHandler<UIElement, CharacterReceivedRoutedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, CharacterReceivedRoutedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 143, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class ContextRequestedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<UIElement, ContextRequestedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 174,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<UIElement, ContextRequestedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, ContextRequestedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), ContextRequestedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<UIElement, ContextRequestedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, ContextRequestedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<UIElement, ContextRequestedEventArgs>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<UIElement, ContextRequestedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, ContextRequestedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 175, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class ContextCanceledEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<UIElement, RoutedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 176,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<UIElement, RoutedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, RoutedEventArgs) -> Unit): EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), RoutedEventArgs(args[1] as ComPtr)) }
      try {
        val token = subscribe(TypedEventHandler<UIElement, RoutedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, RoutedEventArgs) -> Unit): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator fun plusAssign(handler: TypedEventHandler<UIElement, RoutedEventArgs>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<UIElement, RoutedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, RoutedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 177, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class AccessKeyDisplayRequestedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<UIElement, AccessKeyDisplayRequestedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 190,
        handler.pointer).getOrThrow())

    public
        fun subscribeScoped(handler: TypedEventHandler<UIElement, AccessKeyDisplayRequestedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, AccessKeyDisplayRequestedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), AccessKeyDisplayRequestedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<UIElement, AccessKeyDisplayRequestedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, AccessKeyDisplayRequestedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<UIElement, AccessKeyDisplayRequestedEventArgs>) {
      subscribe(handler)
    }

    public operator
        fun invoke(handler: TypedEventHandler<UIElement, AccessKeyDisplayRequestedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, AccessKeyDisplayRequestedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 191, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class AccessKeyDisplayDismissedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<UIElement, AccessKeyDisplayDismissedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 192,
        handler.pointer).getOrThrow())

    public
        fun subscribeScoped(handler: TypedEventHandler<UIElement, AccessKeyDisplayDismissedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, AccessKeyDisplayDismissedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), AccessKeyDisplayDismissedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<UIElement, AccessKeyDisplayDismissedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, AccessKeyDisplayDismissedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<UIElement, AccessKeyDisplayDismissedEventArgs>) {
      subscribe(handler)
    }

    public operator
        fun invoke(handler: TypedEventHandler<UIElement, AccessKeyDisplayDismissedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, AccessKeyDisplayDismissedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 193, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class AccessKeyInvokedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<UIElement, AccessKeyInvokedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 194,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<UIElement, AccessKeyInvokedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, AccessKeyInvokedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), AccessKeyInvokedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<UIElement, AccessKeyInvokedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, AccessKeyInvokedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<UIElement, AccessKeyInvokedEventArgs>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<UIElement, AccessKeyInvokedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, AccessKeyInvokedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 195, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class ProcessKeyboardAcceleratorsEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public
        fun subscribe(handler: TypedEventHandler<UIElement, ProcessKeyboardAcceleratorEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 196,
        handler.pointer).getOrThrow())

    public
        fun subscribeScoped(handler: TypedEventHandler<UIElement, ProcessKeyboardAcceleratorEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, ProcessKeyboardAcceleratorEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), ProcessKeyboardAcceleratorEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<UIElement, ProcessKeyboardAcceleratorEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, ProcessKeyboardAcceleratorEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<UIElement, ProcessKeyboardAcceleratorEventArgs>) {
      subscribe(handler)
    }

    public operator
        fun invoke(handler: TypedEventHandler<UIElement, ProcessKeyboardAcceleratorEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, ProcessKeyboardAcceleratorEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 197, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class GettingFocusEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<UIElement, GettingFocusEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 198,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<UIElement, GettingFocusEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, GettingFocusEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), GettingFocusEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<UIElement, GettingFocusEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, GettingFocusEventArgs) -> Unit): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator fun plusAssign(handler: TypedEventHandler<UIElement, GettingFocusEventArgs>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<UIElement, GettingFocusEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, GettingFocusEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 199, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class LosingFocusEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<UIElement, LosingFocusEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 200,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<UIElement, LosingFocusEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, LosingFocusEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), LosingFocusEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<UIElement, LosingFocusEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, LosingFocusEventArgs) -> Unit): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator fun plusAssign(handler: TypedEventHandler<UIElement, LosingFocusEventArgs>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<UIElement, LosingFocusEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, LosingFocusEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 201, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class NoFocusCandidateFoundEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<UIElement, NoFocusCandidateFoundEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 202,
        handler.pointer).getOrThrow())

    public
        fun subscribeScoped(handler: TypedEventHandler<UIElement, NoFocusCandidateFoundEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, NoFocusCandidateFoundEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), NoFocusCandidateFoundEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<UIElement, NoFocusCandidateFoundEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, NoFocusCandidateFoundEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<UIElement, NoFocusCandidateFoundEventArgs>) {
      subscribe(handler)
    }

    public operator
        fun invoke(handler: TypedEventHandler<UIElement, NoFocusCandidateFoundEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, NoFocusCandidateFoundEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 203, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class BringIntoViewRequestedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<UIElement, BringIntoViewRequestedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 208,
        handler.pointer).getOrThrow())

    public
        fun subscribeScoped(handler: TypedEventHandler<UIElement, BringIntoViewRequestedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (UIElement, BringIntoViewRequestedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(UIElement(args[0] as
          ComPtr), BringIntoViewRequestedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<UIElement, BringIntoViewRequestedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (UIElement, BringIntoViewRequestedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<UIElement, BringIntoViewRequestedEventArgs>) {
      subscribe(handler)
    }

    public operator
        fun invoke(handler: TypedEventHandler<UIElement, BringIntoViewRequestedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (UIElement, BringIntoViewRequestedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 209, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.UIElement"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml", "UIElement")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IUIElement"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private val statics: IUIElementStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IUIElementStatics, ::IUIElementStatics) }

    public val accessKeyProperty: DependencyProperty
      get() = statics.accessKeyProperty

    public val accessKeyScopeOwnerProperty: DependencyProperty
      get() = statics.accessKeyScopeOwnerProperty

    public val allowDropProperty: DependencyProperty
      get() = statics.allowDropProperty

    public val bringIntoViewRequestedEvent: RoutedEvent
      get() = statics.bringIntoViewRequestedEvent

    public val cacheModeProperty: DependencyProperty
      get() = statics.cacheModeProperty

    public val canBeScrollAnchorProperty: DependencyProperty
      get() = statics.canBeScrollAnchorProperty

    public val canDragProperty: DependencyProperty
      get() = statics.canDragProperty

    public val characterReceivedEvent: RoutedEvent
      get() = statics.characterReceivedEvent

    public val clipProperty: DependencyProperty
      get() = statics.clipProperty

    public val compositeModeProperty: DependencyProperty
      get() = statics.compositeModeProperty

    public val contextFlyoutProperty: DependencyProperty
      get() = statics.contextFlyoutProperty

    public val contextRequestedEvent: RoutedEvent
      get() = statics.contextRequestedEvent

    public val doubleTappedEvent: RoutedEvent
      get() = statics.doubleTappedEvent

    public val dragEnterEvent: RoutedEvent
      get() = statics.dragEnterEvent

    public val dragLeaveEvent: RoutedEvent
      get() = statics.dragLeaveEvent

    public val dragOverEvent: RoutedEvent
      get() = statics.dragOverEvent

    public val dropEvent: RoutedEvent
      get() = statics.dropEvent

    public val exitDisplayModeOnAccessKeyInvokedProperty: DependencyProperty
      get() = statics.exitDisplayModeOnAccessKeyInvokedProperty

    public val focusStateProperty: DependencyProperty
      get() = statics.focusStateProperty

    public val gettingFocusEvent: RoutedEvent
      get() = statics.gettingFocusEvent

    public val highContrastAdjustmentProperty: DependencyProperty
      get() = statics.highContrastAdjustmentProperty

    public val holdingEvent: RoutedEvent
      get() = statics.holdingEvent

    public val isAccessKeyScopeProperty: DependencyProperty
      get() = statics.isAccessKeyScopeProperty

    public val isDoubleTapEnabledProperty: DependencyProperty
      get() = statics.isDoubleTapEnabledProperty

    public val isHitTestVisibleProperty: DependencyProperty
      get() = statics.isHitTestVisibleProperty

    public val isHoldingEnabledProperty: DependencyProperty
      get() = statics.isHoldingEnabledProperty

    public val isRightTapEnabledProperty: DependencyProperty
      get() = statics.isRightTapEnabledProperty

    public val isTabStopProperty: DependencyProperty
      get() = statics.isTabStopProperty

    public val isTapEnabledProperty: DependencyProperty
      get() = statics.isTapEnabledProperty

    public val keyDownEvent: RoutedEvent
      get() = statics.keyDownEvent

    public val keyTipHorizontalOffsetProperty: DependencyProperty
      get() = statics.keyTipHorizontalOffsetProperty

    public val keyTipPlacementModeProperty: DependencyProperty
      get() = statics.keyTipPlacementModeProperty

    public val keyTipTargetProperty: DependencyProperty
      get() = statics.keyTipTargetProperty

    public val keyTipVerticalOffsetProperty: DependencyProperty
      get() = statics.keyTipVerticalOffsetProperty

    public val keyUpEvent: RoutedEvent
      get() = statics.keyUpEvent

    public val keyboardAcceleratorPlacementModeProperty: DependencyProperty
      get() = statics.keyboardAcceleratorPlacementModeProperty

    public val keyboardAcceleratorPlacementTargetProperty: DependencyProperty
      get() = statics.keyboardAcceleratorPlacementTargetProperty

    public val lightsProperty: DependencyProperty
      get() = statics.lightsProperty

    public val losingFocusEvent: RoutedEvent
      get() = statics.losingFocusEvent

    public val manipulationCompletedEvent: RoutedEvent
      get() = statics.manipulationCompletedEvent

    public val manipulationDeltaEvent: RoutedEvent
      get() = statics.manipulationDeltaEvent

    public val manipulationInertiaStartingEvent: RoutedEvent
      get() = statics.manipulationInertiaStartingEvent

    public val manipulationModeProperty: DependencyProperty
      get() = statics.manipulationModeProperty

    public val manipulationStartedEvent: RoutedEvent
      get() = statics.manipulationStartedEvent

    public val manipulationStartingEvent: RoutedEvent
      get() = statics.manipulationStartingEvent

    public val noFocusCandidateFoundEvent: RoutedEvent
      get() = statics.noFocusCandidateFoundEvent

    public val opacityProperty: DependencyProperty
      get() = statics.opacityProperty

    public val pointerCanceledEvent: RoutedEvent
      get() = statics.pointerCanceledEvent

    public val pointerCaptureLostEvent: RoutedEvent
      get() = statics.pointerCaptureLostEvent

    public val pointerCapturesProperty: DependencyProperty
      get() = statics.pointerCapturesProperty

    public val pointerEnteredEvent: RoutedEvent
      get() = statics.pointerEnteredEvent

    public val pointerExitedEvent: RoutedEvent
      get() = statics.pointerExitedEvent

    public val pointerMovedEvent: RoutedEvent
      get() = statics.pointerMovedEvent

    public val pointerPressedEvent: RoutedEvent
      get() = statics.pointerPressedEvent

    public val pointerReleasedEvent: RoutedEvent
      get() = statics.pointerReleasedEvent

    public val pointerWheelChangedEvent: RoutedEvent
      get() = statics.pointerWheelChangedEvent

    public val previewKeyDownEvent: RoutedEvent
      get() = statics.previewKeyDownEvent

    public val previewKeyUpEvent: RoutedEvent
      get() = statics.previewKeyUpEvent

    public val projectionProperty: DependencyProperty
      get() = statics.projectionProperty

    public val renderTransformOriginProperty: DependencyProperty
      get() = statics.renderTransformOriginProperty

    public val renderTransformProperty: DependencyProperty
      get() = statics.renderTransformProperty

    public val rightTappedEvent: RoutedEvent
      get() = statics.rightTappedEvent

    public val shadowProperty: DependencyProperty
      get() = statics.shadowProperty

    public val tabFocusNavigationProperty: DependencyProperty
      get() = statics.tabFocusNavigationProperty

    public val tabIndexProperty: DependencyProperty
      get() = statics.tabIndexProperty

    public val tappedEvent: RoutedEvent
      get() = statics.tappedEvent

    public val transform3DProperty: DependencyProperty
      get() = statics.transform3DProperty

    public val transitionsProperty: DependencyProperty
      get() = statics.transitionsProperty

    public val useLayoutRoundingProperty: DependencyProperty
      get() = statics.useLayoutRoundingProperty

    public val useSystemFocusVisualsProperty: DependencyProperty
      get() = statics.useSystemFocusVisualsProperty

    public val visibilityProperty: DependencyProperty
      get() = statics.visibilityProperty

    public val xYFocusDownNavigationStrategyProperty: DependencyProperty
      get() = statics.xYFocusDownNavigationStrategyProperty

    public val xYFocusDownProperty: DependencyProperty
      get() = statics.xYFocusDownProperty

    public val xYFocusKeyboardNavigationProperty: DependencyProperty
      get() = statics.xYFocusKeyboardNavigationProperty

    public val xYFocusLeftNavigationStrategyProperty: DependencyProperty
      get() = statics.xYFocusLeftNavigationStrategyProperty

    public val xYFocusLeftProperty: DependencyProperty
      get() = statics.xYFocusLeftProperty

    public val xYFocusRightNavigationStrategyProperty: DependencyProperty
      get() = statics.xYFocusRightNavigationStrategyProperty

    public val xYFocusRightProperty: DependencyProperty
      get() = statics.xYFocusRightProperty

    public val xYFocusUpNavigationStrategyProperty: DependencyProperty
      get() = statics.xYFocusUpNavigationStrategyProperty

    public val xYFocusUpProperty: DependencyProperty
      get() = statics.xYFocusUpProperty

    public fun tryStartDirectManipulation(value: Pointer): WinRtBoolean =
        statics.tryStartDirectManipulation(value)

    public fun registerAsScrollPort(element: UIElement) {
      statics.registerAsScrollPort(element)
    }
  }
}
