# CsWinRT Overview

## Purpose

`CsWinRT` is Microsoft's C# projection for WinRT. For `kotlin-winrt`, it is a reference source for projection semantics rather than a source-language template.

Use it to answer questions such as:

- How should a runtime class expose its default interface and additional interfaces?
- When should activation go through factories, statics, or constructors?
- How are delegates, events, and tokens represented at the ABI boundary?
- Where does behavior live: generated projection code, runtime helpers, or both?

## What Matters For kotlin-winrt

- Runtime/object identity semantics
- Interface projection and casting rules
- Generic WinRT interface identity and parameterized IID behavior
- Delegate marshaling and event registration lifetime
- Activation, factory, and static member access patterns
- Projection-time code generation boundaries versus runtime support boundaries

## Official Upstream Entry Points

- Repository: `https://github.com/microsoft/CsWinRT`
- Top-level README: `https://github.com/microsoft/CsWinRT/blob/master/README.md`
- Usage guide: `https://github.com/microsoft/CsWinRT/blob/master/docs/usage.md`
- Interop guide: `https://github.com/microsoft/CsWinRT/blob/master/docs/interop.md`
- Repo map: `https://github.com/microsoft/CsWinRT/blob/master/docs/repo.md`
- NuGet/build properties: `https://github.com/microsoft/CsWinRT/blob/master/docs/nuget.md`

## How To Apply It

1. Read the local Kotlin code and name the exact behavior under discussion.
2. Choose the smallest upstream document or source area that answers that behavior.
3. Translate the WinRT semantics into Kotlin terms.
4. Keep the Kotlin API shape idiomatic only if the ABI and projection behavior remain equivalent.

## Scope Boundary

Do not clone `CsWinRT` surface syntax blindly:

- C# helpers may map to different Kotlin wrappers.
- .NET-specific conveniences may have no direct Kotlin equivalent.
- What must match is the externally visible WinRT behavior and the runtime invariants behind it.
