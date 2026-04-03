# Implementation Topics

Use this file when you are actively designing or patching Kotlin code.

## Interface Projection

Primary local anchors:

- `winrt-core/.../Inspectable.kt`
- `winrt-core/.../InterfaceProjection.kt`
- generated interface wrappers such as `generated-winrt-bindings/.../IWindow.kt`

Implementation rules:

- Preserve one canonical default interface path for runtime classes.
- Query or reuse interfaces based on metadata and cached projection rules, not ad hoc casting.
- Keep wrapper identity stable enough that repeated projection of the same ABI pointer does not produce inconsistent behavior.

## Runtime Classes, Statics, And Factories

Primary local anchors:

- `winmd-parser/.../RuntimeCompanionRenderer.kt`
- generated runtime classes such as `generated-winrt-bindings/.../Window.kt`
- runtime metadata helpers in `winrt-core`

Implementation rules:

- Instance construction, factory activation, and static members are separate projection paths.
- Activation kind must come from metadata and be reflected in generated companion/runtime metadata.
- Static interface projection should be lazily resolved through the activation factory path, not hand-wired per type.

## Delegates And Events

Primary local anchors:

- `winmd-parser/.../DelegateTypeRenderer.kt`
- `winmd-parser/.../RuntimeCompanionRenderer.kt`
- `winmd-parser/.../RuntimeMethodRenderer.kt`
- `winrt-core/.../WinRtDelegateBridge.kt`

Implementation rules:

- A generated event subscription must preserve the delegate handle for at least the lifetime implied by the token.
- Unsubscription must release the stored delegate handle exactly once.
- Delegate argument decoding must match WinRT ABI types, especially strings, booleans, integers, and object pointers.

## Generic Interfaces And Parameterized IIDs

Primary local anchors:

- `winrt-core/.../ParameterizedInterfaceIdSupport.kt`
- `winrt-core/.../WinRtTypeSignature.kt`
- `winmd-parser/.../WinRtSignatureMapper.kt`
- generated generic interfaces such as `IAsyncOperation<T>`

Implementation rules:

- The WinRT type signature is part of interface identity.
- Keep signature building deterministic and platform-consistent.
- If a projected generic type changes the signature string or IID derivation, treat it as a breaking behavior change and validate consumers.

## Marshaling And ABI Calls

Primary local anchors:

- `kom/.../ComInterop.kt`
- `kom/.../JvmPlatform.kt`
- `kom/.../MingwPlatform.kt`
- `winmd-parser/.../AbiCallCatalog.kt`
- `winmd-parser/.../HStringSupport.kt`

Implementation rules:

- Do not hide ownership transitions. Make the caller/callee release contract explicit.
- HSTRING conversions must preserve both data and release behavior.
- HRESULT conversion policy must be consistent between direct calls and delegate callbacks.
- If generated code chooses a different ABI helper path, confirm that the helper preserves the same failure and ownership semantics.

## Collections And Async Projections

Primary local anchors:

- `winmd-parser/.../KotlinCollectionProjectionMapper.kt`
- `winmd-parser/.../AsyncMethodProjectionPlanner.kt`
- generated projections under `generated-winrt-bindings/src/commonMain/kotlin/windows/foundation`

Implementation rules:

- Preserve WinRT collection semantics before applying Kotlin collection conveniences.
- Preserve async status, completion, and error propagation semantics before adding coroutine helpers.
- Treat helper extension APIs as additive. They must not change the underlying projected ABI behavior.
