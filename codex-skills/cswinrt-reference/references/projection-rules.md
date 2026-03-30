# Projection Rules

## Runtime First

When a change touches COM pointers, interface wrappers, activation, delegates, or generic interface identity, compare runtime behavior first. A Kotlin projection that looks neat but changes ABI ownership or interface identity is wrong.

## Generated Code Versus Runtime Helpers

`CsWinRT` behavior is often split across generated projection code and runtime support types. Mirror the same split in Kotlin where it fits:

- `winmd-parser` and `generated-winrt-bindings` describe projected types and call shapes.
- `winrt-core` owns WinRT object semantics and projection helpers.
- `kom` owns low-level COM and ABI details.

Do not force all behavior into the generator if `CsWinRT` relies on a runtime helper, and do not push runtime invariants into ad hoc handwritten call sites.

## Interface Projection

- Preserve the default interface contract for runtime classes.
- Expose implemented interfaces consistently with the metadata model.
- Keep casting/projection rules stable for already-known interfaces versus newly queried interfaces.
- Treat static members and factory members as metadata-driven projection behavior, not ordinary instance methods.

## Generic Interfaces And Parameterized IIDs

- Treat generic WinRT identity as part of the ABI contract.
- Match parameterized IID construction and caching behavior at the semantic level.
- Keep the mapping from projected generic arguments to WinRT signatures deterministic.

For Kotlin work in this area, compare with local files such as `winrt-core/.../ParameterizedInterfaceIdSupport.kt` and the generated bindings that consume those IDs.

## Delegates And Events

- Keep delegate wrappers alive for as long as the ABI contract requires.
- Match event registration token semantics and revocation behavior.
- Handle HRESULT and exception conversion consistently at delegate boundaries.

## Activation

- Distinguish instance activation, factory activation, and static access.
- Keep activation-path lookup metadata-driven.
- When a runtime class requires special activation handling, treat it as a projection rule, not an app-specific workaround.

## Marshaling

Compare these explicitly whenever they appear:

- pointer ownership and release behavior
- HSTRING and string conversion
- GUID and type-signature flow
- boxing and unboxing
- HRESULT to exception or error-result conversion

## Review Heuristic

If a Kotlin change answers any of these with "because it is simpler", re-check it against `CsWinRT`:

- Which interface is considered canonical here?
- Who owns this pointer after the call?
- Is this identity stable across repeated projections?
- Does metadata, not convenience, decide this call path?
