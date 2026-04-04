# CsWinRT Pipeline Reference

Use this file when the task is about how `CsWinRT` reads WinMD inputs or how it turns metadata into projected source. This file is intentionally written so the skill still works when the `CsWinRT` source tree is not present in the current repository.

Treat the pipeline below as the stable behavior model to compare against `kotlin-winrt`.

## Stage 1: Input Expansion

`CsWinRT` does not start from "all metadata everywhere". It first expands explicit CLI inputs into a concrete set of WinMD files.

Stable behavior:

- `-input` accepts direct files, directories, SDK aliases, and response files.
- A direct file is included as-is.
- A directory input is enumerated and filtered to metadata files. Current implementation uses a plain directory iterator, so the actual behavior is immediate-directory scanning rather than deep recursive traversal.
- `local` expands to the system WinMetadata directory under `%windir%`.
- `sdk` and `sdk+` resolve the active Windows SDK version, then load WinMDs listed by `Platform.xml`.
- `version` and `version+` behave the same way for a specific SDK version.
- The `+` suffix means "also include extension SDK metadata" by walking `Extension SDKs` manifests.
- `@response.rsp` is parsed line by line using Windows command-line quoting rules. A line beginning with `#` is treated as a comment.

Implication for `kotlin-winrt`:

- If a bug is "wrong metadata got loaded" or "an SDK contract unexpectedly appeared or disappeared", compare input expansion rules before touching projection code.
- Keep SDK/platform manifest behavior separate from raw `.winmd` parsing behavior.

## Stage 2: Metadata Cache Construction

After input expansion, `CsWinRT` builds a metadata cache over the selected WinMD files using the WinMD reader library.

Stable behavior:

- Metadata parsing is delegated to a cache/model layer, not interleaved with code emission.
- The cache exposes namespace-grouped members and resolved metadata entities such as `TypeDef`, attributes, signatures, interfaces, and generic information.
- Later stages ask the cache semantic questions such as:
  category of a type
  custom attributes
  default interface and implemented interfaces
  field/method/property signatures
  generic instantiations and type references

Implication for `kotlin-winrt`:

- Separate "how metadata is decoded and classified" from "how Kotlin is emitted".
- If multiple generated files are wrong in the same way, the bug is usually in the metadata model or classification layer, not in each renderer.

## Stage 3: Filter Setup

`CsWinRT` applies namespace/type filters after metadata is loaded but before emission.

Stable behavior:

- Include and exclude prefixes build a main projection filter.
- Additions use a separate filter: included namespaces are eligible by default unless explicitly excluded from additions.
- Component mode adds an extra pass that finds authored runtime classes with `ActivatableAttribute` or `StaticAttribute`.

Implication for `kotlin-winrt`:

- Treat filtering as a first-class stage. A missing type can be a filter problem, not a renderer problem.
- Keep "project the metadata type" distinct from "append namespace additions/helpers for that namespace".

## Stage 4: Per-Namespace Traversal

Projection generation runs per namespace, not per entire assembly blob.

Stable behavior:

- Each namespace gets its own generated projection file.
- Work is parallelized across namespaces, but the semantic unit is still a namespace.
- Cross-cutting data needed later is accumulated while namespaces are processed:
  event helper definitions
  base-type mappings
  authored-type-to-metadata-type mappings
  generic ABI delegate registrations

Implication for `kotlin-winrt`:

- If helper files are wrong, inspect the accumulation stage as well as the final writer.
- Namespace-local generation can still feed global helper outputs.

## Stage 5: Type Classification

Within each namespace, `CsWinRT` classifies each included type and emits different code paths by category.

Stable behavior:

- Classes, delegates, enums, interfaces, and structs are handled by different writers.
- Attribute classes are treated specially instead of using the normal runtime-class path.
- API contract structs are emitted as contracts, not ordinary structs.
- Struct ABI emission depends on blittability.
- Built-in mapped types short-circuit normal type emission. They are treated as custom/manual projections rather than generated from scratch.

Practical classification rules:

- `class`
  projected class emission
  optional factory-class emission in component mode
  base-type and authored-metadata mapping collection in the normal projection path
- `delegate`
  projected delegate emission plus ABI delegate support
- `enum`
  projected enum emission only; no ABI namespace body needed
- `interface`
  projected interface emission plus static ABI helper classes and ABI interface emission
- `struct`
  projected struct emission; non-blittable structs also get ABI structs

Implication for `kotlin-winrt`:

- Never fix a class/interface/delegate bug by adding ad hoc special cases before confirming the type category logic is correct.
- If a type is custom-mapped, compare against the mapped/manual projection behavior, not the normal generator path.

## Stage 6: Projected Source Emission

`CsWinRT` first emits the public projection surface for included types.

Stable behavior:

- Each namespace file starts with a generated-file header and a projected namespace body.
- Projected source is emitted before ABI declarations.
- Event helper subclasses are discovered during this phase.
- Generic references discovered from type shapes are collected for later ABI delegate or generic-instantiation helpers.

What this stage decides:

- public or internal projection accessibility depending on mode
- projected class/interface/delegate/enum/struct shape
- factory-class surface for component projections
- which later helper/init files will be necessary

Implication for `kotlin-winrt`:

- If the user-visible API shape is wrong but runtime helpers seem fine, the bug is in this stage.
- Keep user-facing projection shape decisions metadata-driven.

## Stage 7: ABI Source Emission

If any type in a namespace needs ABI support, `CsWinRT` emits a second namespace block under `ABI.<Namespace>`.

Stable behavior:

- ABI emission is conditional, not automatic for every type.
- Enums do not generate ABI bodies.
- Blittable structs skip ABI struct wrappers.
- Interfaces always emit static ABI helper classes plus ABI interfaces.
- Delegates emit ABI delegates and WinRT-exposed helper types.
- Runtime classes emit ABI classes; component mode may also emit WinRT-exposed factory helpers.
- Normal projection code and ABI code are separate outputs inside the same namespace file.

Implication for `kotlin-winrt`:

- If the projected API looks right but calls fail at runtime, compare the ABI layer next.
- Distinguish "projected type shape" from "ABI helper shape"; they are related but not interchangeable.

## Stage 8: Namespace Additions

After ordinary namespace emission, `CsWinRT` may append predefined additions.

Stable behavior:

- Additions are namespace-specific source fragments shipped with the generator.
- Additions are enabled by include filters and disabled by `addition_exclude`.
- Additions are not the same as mapped types:
  mapped types suppress normal generation for specific metadata types
  additions append extra source for a namespace

Implication for `kotlin-winrt`:

- If behavior exists in `CsWinRT` but not in raw metadata, check whether it comes from additions rather than metadata-driven rendering.

## Stage 9: Post-Pass Helper Files

`CsWinRT` emits several helper files after namespace generation. These files are part of the projection contract, not incidental scaffolding.

Stable behavior:

- `WinRTEventHelpers.cs`
  collects event helper definitions gathered during namespace processing.
- `WinRTBaseTypeMappingHelper.cs`
  registers a type-name-to-base-type map through a module initializer.
- `WinRTAbiDelegateInitializer.cs`
  netstandard-only helper that registers ABI delegate signatures gathered from generic usage.
- `WinRTGenericTypeInstantiations.cs`
  modern .NET helper for generic instantiation support when needed.
- `WinRT_Module.cs`
  component-mode helper for module activation factory behavior.
- `AuthoringMetadataTypeMappingHelper.cs`
  component-mode helper that registers authored runtime types to metadata types.
- Static string-based helpers such as `InitializeProjection.cs`
  register the projection assembly via module initializer.
- `ComInteropHelpers.cs`
  is emitted only when the projected filter includes `Windows`.

Implication for `kotlin-winrt`:

- Missing helper files can break behavior even when the main generated type file looks correct.
- Treat module initializers and helper registration as part of projection semantics.

## Stage 10: Target And Mode Differences

`CsWinRT` changes generation behavior based on target mode and authoring mode.

Stable behavior:

- `netstandard2.0`
  uses alternate class/interface ABI generation paths and emits ABI delegate registration helpers.
- modern .NET
  defaults to the newer projection path and may emit generic instantiation helpers instead.
- `component`
  enables authored-type behavior such as factory classes, module activation helpers, and metadata-type registration.
- `internal`, `embedded`, `public_enums`, `public_exclusiveto`, `idic_exclusiveto`, and `partial_factory`
  adjust accessibility or authored-projection behavior rather than changing the metadata model itself.

Implication for `kotlin-winrt`:

- Do not compare outputs across modes without normalizing for target and component settings.
- A generator bug may be mode-specific even when the metadata is identical.

## Ownership Split: Generator Versus Runtime

Use this split when translating `CsWinRT` behavior into `kotlin-winrt`.

- Metadata ingestion, filtering, type classification, and emitted source shape belong to the generator side.
- Object identity, interface lookup, activation plumbing, marshaling helpers, custom type mappings, and parameterized IID logic belong to runtime support.
- Some behaviors are intentionally split:
  projected source decides which helper to call
  runtime support decides what that helper actually does

If the generated Kotlin looks fine but runtime semantics differ, do not keep patching templates. Move to `winrt-core` or `kom`.

## Debugging Heuristics

When a task mentions `CsWinRT`, force the problem into one of these buckets first:

1. Wrong WinMD inputs selected
2. Metadata model/classification is wrong
3. Type filtering or mapped-type handling is wrong
4. Public projection emission is wrong
5. ABI emission is wrong
6. Helper/init file emission is wrong
7. Runtime helper semantics are wrong

That bucketing step is mandatory. It prevents vague "projection mismatch" diagnoses.
