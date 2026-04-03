# Upstream Map

Use this file when you know the task area but need to decide where upstream evidence should come from.

## Start With Docs

- `README.md`
  Use for package roles, versioning expectations, and high-level support boundaries.

- `docs/usage.md`
  Use for projection generation workflow, project/package knobs, and consumption patterns.

- `docs/interop.md`
  Use for ABI-facing interop expectations and runtime bridging questions.

- `docs/nuget.md`
  Use for build properties that influence generation and runtime packaging.

- `docs/repo.md` or `docs/structure.md`
  Use for repository layout when you need to find the owning upstream source directory.

## Upstream Source Areas

The exact folder names can evolve, so use the repo map plus code search, but the stable categories are:

- projection generator code
  Owns how WinMD metadata becomes projected source.

- runtime support code, especially `WinRT.Runtime`
  Owns object identity, interface lookup, activation plumbing, delegates, marshaling helpers, and generic type support.

- authoring and samples
  Useful when you need end-to-end evidence of how generated code is expected to be consumed.

- build and packaging files
  Useful when a behavior comes from configuration rather than projection logic.

## Task To Upstream Area

- Runtime object semantics
  Start in runtime support code plus `docs/interop.md`.

- WinMD projection or generated API shape
  Start in generator source plus `docs/usage.md`, then inspect generated examples if available.

- Generic interface identity or type signatures
  Start in runtime support code and any helper that computes WinRT signatures or parameterized interfaces.

- Delegate, event, or callback behavior
  Start in runtime support code, delegate/event helper code, and `docs/interop.md`.

- Activation, factory, or static member access
  Start in runtime support code plus generated projection code for runtime classes.

- Build flags or package configuration
  Start in `docs/nuget.md` and package/build source.

## What Evidence To Extract

When reading `CsWinRT`, answer these concrete questions:

- Which layer owns the behavior: generator, runtime, or both?
- What metadata or type information drives the behavior?
- Which part is WinRT-intrinsic, and which part is .NET convenience?
- What must remain stable for user-observable behavior to match?

## Translation Heuristic For kotlin-winrt

Map upstream evidence into local modules like this:

- low-level ABI and pointer mechanics -> `kom`
- WinRT object model helpers -> `winrt-core`
- metadata reading and model building -> `winmd-parser-plugin`
- source emission strategy -> `winmd-parser`
- observable projection output -> `generated-winrt-bindings`

When multiple local modules participate in one behavior, preserve the full behavior across modules instead of copying only the most visible upstream snippet.
