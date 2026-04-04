# Examples

Use these examples as routing guides, not as exhaustive recipes.

## Example: WinMD Reader Mismatch

Symptom:
The Kotlin side loads the wrong metadata set, misses SDK contracts, or classifies a type differently from `CsWinRT`.

Read:

- `references/cswinrt-pipeline.md`
- `references/kotlin-winrt-map.md`

Inspect locally:

- `winmd-parser-plugin/.../WinMdMetadataReader.kt`
- `winmd-parser-plugin/.../WinMdConfigurationResolver.kt`
- `winmd-parser-plugin/.../WinMdIr.kt`

Validate with:

- focused metadata-reader tests
- a generator test that proves the affected type set or metadata shape

## Example: Helper File Or ABI Initialization Missing

Symptom:
The projected Kotlin shape looks plausible, but runtime activation, event bridging, delegate registration, or generic support is broken because support code was never generated or registered.

Read:

- `references/cswinrt-pipeline.md`
- `references/review-checklists.md`

Inspect locally:

- `winmd-parser/...` renderers that emit helper/bootstrap code
- `winrt-core/...` registration and runtime helper entry points
- generated support files or generated companion/runtime bootstrap code

Validate with:

- targeted generator tests
- the smallest runtime test that crosses the missing helper path

## Example: Default Interface Bug On A Runtime Class

Symptom:
Generated runtime class projects the wrong interface or repeatedly queries the wrong IID.

Read:

- `references/implementation-topics.md`
- `references/kotlin-winrt-map.md`
- upstream runtime support and generated runtime-class behavior in `CsWinRT`

Inspect locally:

- `winrt-core/.../Inspectable.kt`
- `winmd-parser/.../RuntimeTypeRenderer.kt`
- generated class and interface wrappers for the affected type

Validate with:

- `RuntimePropertyTest`
- a focused generated binding parity or projection test

## Example: Event Subscription Leaks Or Forgets Delegate Handles

Symptom:
Event handlers stop firing, cannot unsubscribe, or leak handles.

Read:

- `references/implementation-topics.md`
- `references/review-checklists.md`

Inspect locally:

- `winmd-parser/.../RuntimeCompanionRenderer.kt`
- `winrt-core/.../WinRtDelegateBridge.kt`
- platform-specific delegate bridge code

Validate with:

- `WinRtDelegateBridgeTest`
- any generated binding tests covering event tokens

## Example: Parameterized IID Mismatch

Symptom:
Generic interfaces project or compare incorrectly, especially `IAsyncOperation<T>`-style shapes.

Read:

- `references/implementation-topics.md`
- `references/kotlin-winrt-map.md`

Inspect locally:

- `winrt-core/.../ParameterizedInterfaceIdSupport.kt`
- `winrt-core/.../WinRtTypeSignature.kt`
- `winmd-parser/.../WinRtSignatureMapper.kt`
- generated generic bindings

Validate with:

- `ParameterizedInterfaceIdTest`
- `WinRtTypeSignatureTest`
- focused generated binding tests

## Example: Wrong Activation Or Static Interface Path

Symptom:
A runtime class should activate through a factory or expose statics, but generated code chooses the wrong route.

Read:

- `references/implementation-topics.md`
- `references/upstream-map.md`

Inspect locally:

- `winmd-parser/.../RuntimeCompanionRenderer.kt`
- `winmd-parser-plugin/.../WinMdMetadataReader.kt`
- generated runtime class companion object

Validate with:

- a generator test
- a projection test using `getActivationFactory` or generated statics access
