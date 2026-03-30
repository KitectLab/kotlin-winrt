# kotlin-winrt Map

Use this file to decide where a `CsWinRT`-derived behavior belongs in this repository.

## Module Ownership

- `kom`
  Owns COM ABI primitives and platform interop. Put pointer types, HRESULT handling, GUID types, HSTRING bridges, raw vtable calls, and platform-specific COM operations here.

- `winrt-core`
  Owns WinRT object semantics on top of COM primitives. Put `Inspectable`, interface projection helpers, runtime metadata helpers, type signatures, delegate bridges, and parameterized IID support here.

- `winmd-parser-plugin`
  Owns WinMD reading, metadata tables, configuration resolution, and the intermediate model. Put metadata discovery and classification logic here.

- `winmd-parser`
  Owns Kotlin emission policy. Put type-name mapping, interface/runtime-class rendering, delegate/event code generation, async shaping, and collection projection generation here.

- `generated-winrt-bindings`
  Is the observable output surface. Use it to verify whether the generator plus runtime helpers produce the expected API shape and call paths.

## File-Level Landing Zones

- Interface projection helpers
  `winrt-core/src/commonMain/kotlin/dev/winrt/core/Inspectable.kt`
  `winrt-core/src/commonMain/kotlin/dev/winrt/core/InterfaceProjection.kt`
  `winrt-core/src/commonMain/kotlin/dev/winrt/core/BindingMetadata.kt`

- Parameterized IID and type signatures
  `winrt-core/src/commonMain/kotlin/dev/winrt/core/ParameterizedInterfaceIdSupport.kt`
  `winrt-core/src/commonMain/kotlin/dev/winrt/core/ParameterizedInterfaceId.kt`
  `winrt-core/src/commonMain/kotlin/dev/winrt/core/WinRtTypeSignature.kt`
  platform variants:
  `winrt-core/src/jvmMain/kotlin/dev/winrt/core/JvmParameterizedInterfaceId.kt`
  `winrt-core/src/mingwX64Main/kotlin/dev/winrt/core/MingwParameterizedInterfaceId.kt`

- Delegate and callback bridges
  `winrt-core/src/commonMain/kotlin/dev/winrt/core/WinRtDelegateBridge.kt`
  `winrt-core/src/jvmMain/kotlin/dev/winrt/core/JvmWinRtDelegateBridge.kt`
  `winrt-core/src/jvmMain/kotlin/dev/winrt/core/JvmWinRtUnitResultDelegates.kt`
  `winrt-core/src/jvmMain/kotlin/dev/winrt/core/JvmWinRtBooleanResultDelegates.kt`
  `winrt-core/src/mingwX64Main/kotlin/dev/winrt/core/MingwWinRtDelegateBridge.kt`

- COM ABI and marshaling
  `kom/src/commonMain/kotlin/dev/winrt/kom/ComInterop.kt`
  `kom/src/commonMain/kotlin/dev/winrt/kom/ComPtr.kt`
  `kom/src/commonMain/kotlin/dev/winrt/kom/HResult.kt`
  `kom/src/commonMain/kotlin/dev/winrt/kom/HString.kt`
  `kom/src/jvmMain/kotlin/dev/winrt/kom/JvmPlatform.kt`
  `kom/src/jvmMain/kotlin/dev/winrt/kom/Jdk22Foreign.kt`
  `kom/src/mingwX64Main/kotlin/dev/winrt/kom/MingwPlatform.kt`

- Metadata reading and classification
  `winmd-parser-plugin/src/main/kotlin/dev/winrt/winmd/plugin/WinMdMetadataReader.kt`
  `winmd-parser-plugin/src/main/kotlin/dev/winrt/winmd/plugin/WinMdIr.kt`
  `winmd-parser-plugin/src/main/kotlin/dev/winrt/winmd/plugin/InterfaceVtableResolver.kt`
  `winmd-parser-plugin/src/main/kotlin/dev/winrt/winmd/plugin/WinMdConfigurationResolver.kt`

- Kotlin emission
  `winmd-parser/src/main/kotlin/dev/winrt/winmd/parser/RuntimeTypeRenderer.kt`
  `winmd-parser/src/main/kotlin/dev/winrt/winmd/parser/RuntimeCompanionRenderer.kt`
  `winmd-parser/src/main/kotlin/dev/winrt/winmd/parser/RuntimeMethodRenderer.kt`
  `winmd-parser/src/main/kotlin/dev/winrt/winmd/parser/RuntimePropertyRenderer.kt`
  `winmd-parser/src/main/kotlin/dev/winrt/winmd/parser/DelegateTypeRenderer.kt`
  `winmd-parser/src/main/kotlin/dev/winrt/winmd/parser/DelegateLambdaPlan.kt`
  `winmd-parser/src/main/kotlin/dev/winrt/winmd/parser/EventSlotDelegateSupport.kt`
  `winmd-parser/src/main/kotlin/dev/winrt/winmd/parser/TypeNameMapper.kt`
  `winmd-parser/src/main/kotlin/dev/winrt/winmd/parser/WinRtSignatureMapper.kt`
  `winmd-parser/src/main/kotlin/dev/winrt/winmd/parser/KotlinCollectionProjectionMapper.kt`

## How To Route A Change

- If the bug is "wrong metadata interpretation", start in `winmd-parser-plugin`.
- If the bug is "metadata was understood, but emitted Kotlin is wrong", start in `winmd-parser`.
- If the bug is "generated Kotlin shape looks right, but runtime behavior is wrong", start in `winrt-core`.
- If the bug is "pointer, HRESULT, HSTRING, or raw call mechanics are wrong", start in `kom`.
- If the bug spans more than one layer, trace the behavior from generated binding call site back through `winrt-core` to `kom`.
