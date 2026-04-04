---
name: cswinrt-reference
description: Use CsWinRT as the behavioral reference when implementing, reviewing, or debugging WinRT metadata ingestion, projection generation, or runtime behavior in kotlin-winrt. Trigger this skill for WinMD parsing rules, metadata filters, mapped types, generated projection or ABI shape, helper/init file emission, activation/factory behavior, delegate and event bridging, parameterized IID logic, ABI marshaling decisions, or any task that should match the observable behavior of Microsoft CsWinRT even when the CsWinRT source tree is not available in the current repository.
---

# CsWinRT Reference

Use this skill to keep `kotlin-winrt` aligned with the official `microsoft/CsWinRT` projection model.

Start from the local repository contract in `README.md`: when local behavior and the Microsoft projections disagree, change `kotlin-winrt` to match the reference projection model instead of inventing project-specific rules.

## Workflow

1. Identify whether the task is WinMD ingestion, projection generation, runtime behavior, or app-facing API shape work.
2. If the task touches WinMD reading or generated projection shape, read `references/cswinrt-pipeline.md` first. That file is the distilled behavior model and must be usable even when the upstream `CsWinRT` source tree is absent.
3. Read the local Kotlin implementation so the comparison is concrete.
4. Load only the smallest additional reference file from `references/` that matches the task.
5. If a local `.cswinrt` or `CsWinRT` checkout exists, use it as extra evidence after loading the bundled behavior reference. Do not make the skill depend on that checkout.
6. Compare semantics, not surface syntax.
7. Preserve Kotlin idioms only when they do not change WinRT-observable behavior.

## Comparison Rules

- Treat `CsWinRT` as a behavioral oracle, not a line-by-line code template.
- Prefer the bundled behavior references when the upstream source tree is unavailable. If the bundled reference and a live checkout appear to disagree, trust the implementation and update the skill later.
- Match activation rules, default interface selection, implemented interface exposure, factory/static access, delegate lifetime, and generic type identity before considering naming differences.
- Compare ABI boundaries explicitly: ownership, `AddRef`/`Release`, boxing, string/HSTRING flow, HRESULT translation, and event token handling.
- For generator changes, compare metadata ingestion, filtering, emitted metadata intent, helper-file emission, and runtime call paths, not just generated member names.
- When `CsWinRT` behavior is split between generated code and runtime helpers, reproduce the same total behavior across `winmd-parser`, `generated-winrt-bindings`, `winrt-core`, and `kom`.

## Reference Files

- `references/overview.md`: what `CsWinRT` is, what parts matter for this repository, and official upstream links.
- `references/cswinrt-pipeline.md`: distilled end-to-end behavior for WinMD input expansion, metadata cache usage, namespace/type filtering, mapped types, projected/ABI emission, helper/init file generation, target-mode differences, and component-mode behavior. Read this first for metadata-reader or generator tasks.
- `references/upstream-map.md`: where to look in the `CsWinRT` repo and docs for a given task.
- `references/kotlin-winrt-map.md`: which local Kotlin modules and files own which WinRT behaviors.
- `references/implementation-topics.md`: implementation-oriented rules for interface projection, activation, delegates, generic IIDs, and marshaling.
- `references/review-checklists.md`: concrete checks to run before finalizing a projection/runtime change.
- `references/examples.md`: task-shaped examples that map common `kotlin-winrt` changes to the right local files and upstream evidence.

Load only the file that matches the current task.

## Working Conventions

- Prefer official `microsoft/CsWinRT` docs, source, and repo paths over secondary explanations.
- If the current task depends on a specific upstream implementation detail and the source tree is available locally or online, inspect it before finalizing the change.
- State explicitly when a conclusion is inferred from multiple upstream sources rather than spelled out in one place.
- If `CsWinRT` and `C++/WinRT` appear to disagree, treat that as a projection-model question and check both before changing Kotlin behavior.
- When touching generated bindings, inspect both the generator input path and the generated Kotlin output. Projection bugs often come from the split between metadata modeling and rendering.
- When the bug smells like "wrong thing got generated" or "wrong metadata got interpreted", explicitly decide which stage is wrong:
  `input expansion -> metadata cache/model -> type filtering/classification -> projected source emission -> ABI/helper/init emission -> runtime helper behavior`.
- Keep changes evidence-driven: note which upstream doc or source area justified the local behavior.
