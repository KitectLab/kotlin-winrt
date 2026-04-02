# CsWinRT Gap Plan

This document tracks the current gap between `kotlin-winrt` and the observable projection behavior of `CsWinRT`.

The goal is not to copy C# surface syntax. The goal is to match WinRT behavior across:

- `kom` for ABI mechanics
- `winrt-core` for projection/runtime semantics
- `winmd-parser-plugin` for metadata interpretation
- `winmd-parser` for emitted Kotlin shape
- `generated-winrt-bindings` for the observable output surface

## Current Assessment

`kotlin-winrt` already has the basic architecture for a WinRT projection stack, but it is still far from a full `CsWinRT`-level projection. The largest gap is breadth of coverage, followed by semantic completeness in activation, interface projection, and generic behavior.

## Completed Or Mostly In Place

- COM and WinRT pointer plumbing exists in `kom`.
- `IInspectable`-style projection, `QueryInterface`, and projection caching exist in `winrt-core`.
- Parameterized interface IID support exists.
- Delegate, event token, and async projection support exists for the implemented subset.
- Collection projection to Kotlin collection types exists for common collection interfaces.
- The generator can emit checked-in bindings from WinMD input.

## Partial Coverage

- Default interface behavior exists, but only as far as the current generated/runtime subset needs it.
- Activation exists through a provider model, but the activation model is narrow.
- Static member access and factory access are supported for the currently generated runtime classes, but not as a fully generalized projection policy.
- Generic WinRT support exists for some interfaces and delegates, but not as a complete identity/signature system across the full WinRT type space.
- Event and delegate bridging exists, but lifecycle and ownership behavior are not yet validated as a fully generalized rule set.

## Major Gaps

### 1. Projection Coverage

The checked-in bindings cover only a small subset of the Windows SDK surface.

Observed coverage is concentrated in:

- `Windows.Foundation`
- `Windows.Data.Json`
- `Windows.Globalization`
- `Windows.System.UserProfile`
- a small `Microsoft.UI.Xaml` subset

This is the biggest gap relative to `CsWinRT`.

### 2. Activation and Static Semantics

`CsWinRT` treats runtime class activation, factories, statics, constructors, and default interface exposure as one coherent model.

Current `kotlin-winrt` support is still fragmented:

- `WinRtActivationKind` is minimal
- factory constructors are generated for known cases
- static access is present only where the generator has explicit rules

### 3. Generic Identity and Parameterized IID Behavior

`CsWinRT` has strong rules for generic type identity, parameterized IID computation, and projected type stability.

`kotlin-winrt` has the building blocks, but the system is not yet complete across all generic interfaces and delegates.

### 4. Event and Delegate Lifetime

Delegate wrappers and event token handling exist, but there is still work to do to make ownership and release behavior robust and uniform.

### 5. ABI Fidelity

The low-level ABI layer is usable, but it still needs more validation around:

- object ownership
- release behavior
- HSTRING flow
- HRESULT translation
- cross-platform consistency

## Gap Rating

This is a pragmatic estimate, not a formal benchmark.

- ABI foundation: about 50% to 70% in place
- Semantic projection model: about 50% to 70% in place
- Full projection coverage: about 10% to 20% in place
- Overall `CsWinRT` parity: early stage, not yet close to production parity

## Suggested Roadmap

### Phase 1: Semantic Alignment

- Tighten default interface behavior.
- Normalize activation and factory/static access semantics.
- Verify generic interface identity and parameterized IID behavior against reference projection rules.
- Harden delegate/event lifetime handling.

### Phase 2: Broaden Coverage

- Expand checked-in bindings to a larger WinMD surface.
- Prioritize common SDK namespaces and high-value WinRT APIs.
- Add parity tests for more generated types.

### Phase 3: ABI Hardening

- Add more ownership and release tests.
- Validate HSTRING and HRESULT translation paths.
- Exercise cross-platform behavior more systematically.

### Phase 4: Projection Consistency

- Reduce special-case logic in the generator where a generalized rule is possible.
- Make runtime helpers the canonical implementation of WinRT-observable behavior.
- Keep generated bindings as thin as possible while preserving projection rules.

## Execution Order

If the goal is to move toward `CsWinRT` parity efficiently, the practical order is:

1. Semantic alignment in `winrt-core`
2. Generator normalization in `winmd-parser`
3. Coverage expansion in `generated-winrt-bindings`
4. ABI hardening in `kom`

## Notes

- This plan is based on the current repository state and the existing `CsWinRT` reference guidance.
- Any future gap assessment should be updated against concrete generated output and runtime tests, not only source shape.
