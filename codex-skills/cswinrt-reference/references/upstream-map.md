# Upstream Map

Use this file when you know the task area but need to decide where upstream evidence should come from.

## Start With Docs

- `README.md`: project scope, support matrix, package overview
- `docs/usage.md`: how projections are generated and consumed
- `docs/interop.md`: ABI and interop-focused guidance
- `docs/nuget.md`: build and package properties that affect generation/runtime behavior
- `docs/repo.md`: repository layout

## Task To Upstream Area

- Runtime object semantics
  Look for runtime support code and interop docs first.

- WinMD projection or generated API shape
  Look for usage docs, generator-related source, and generated sample output.

- Generic interface identity or type signatures
  Look for runtime support plus generated signatures that demonstrate the metadata contract.

- Delegate, event, or callback behavior
  Look for interop docs and runtime delegate support code.

- Build flags or package configuration
  Look for `docs/nuget.md` and package-related source files.

## Translation Heuristic For kotlin-winrt

Map upstream evidence into local modules like this:

- low-level ABI and pointer mechanics -> `kom`
- WinRT object model helpers -> `winrt-core`
- metadata reading and model building -> `winmd-parser-plugin`
- source emission strategy -> `winmd-parser`
- observable projection output -> `generated-winrt-bindings`

When multiple local modules participate in one behavior, preserve the full behavior across modules instead of copying only the most visible upstream snippet.
