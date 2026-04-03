# Review Checklists

Use these checklists before finalizing a `kotlin-winrt` change guided by `CsWinRT`.

## Runtime And Projection Changes

- Which WinRT behavior is changing, in one sentence?
- Which upstream source area or document justifies the change?
- Is the behavior owned by generator logic, runtime helpers, or low-level ABI code?
- Does the change preserve default interface behavior and activation semantics?
- Does it preserve pointer ownership and release behavior?
- Does it preserve HRESULT and exception translation?

## Generator Changes

- Did the metadata model change, the emitted Kotlin change, or both?
- Does generated output now route through the correct runtime helper?
- Did a generated runtime class companion get the correct activation kind?
- Did delegate/event emission preserve token handling and handle lifetime?
- Did generic interface emission preserve signature and IID semantics?

## Runtime Helper Changes

- Are repeated projections of the same pointer stable?
- Are cached results safe across platforms?
- Are platform-specific implementations still semantically aligned?
- Are there tests covering both positive behavior and ownership/lifetime edge cases?

## Validation Targets In This Repo

Prefer the smallest relevant tests first:

- `winrt-core/src/jvmTest/kotlin/dev/winrt/core/ParameterizedInterfaceIdTest.kt`
- `winrt-core/src/jvmTest/kotlin/dev/winrt/core/WinRtDelegateBridgeTest.kt`
- `winrt-core/src/commonTest/kotlin/dev/winrt/core/RuntimePropertyTest.kt`
- `winmd-parser/src/test/kotlin/dev/winrt/winmd/parser/KotlinBindingGeneratorTest.kt`
- `winmd-parser/src/test/kotlin/dev/winrt/winmd/parser/DelegateLambdaPlanResolverTest.kt`
- `winmd-parser/src/test/kotlin/dev/winrt/winmd/parser/WinRtSignatureMapperTest.kt`
- `winmd-parser/src/test/kotlin/dev/winrt/winmd/parser/CheckedInBindingsParityTest.kt`
- projection tests under `generated-winrt-bindings/src/jvmTest`

On WSL, invoke Gradle through `./.agent_scripts/run_windows_gradle.sh`.
