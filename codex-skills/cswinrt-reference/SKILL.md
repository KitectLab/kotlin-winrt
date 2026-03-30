---
name: cswinrt-reference
description: Use CsWinRT as the reference projection when implementing, reviewing, or debugging WinRT behavior in kotlin-winrt. Trigger this skill for Kotlin/WinRT runtime work, WinMD-driven code generation, interface projection rules, activation/factory behavior, delegate and event bridging, parameterized IID logic, ABI marshaling decisions, or any task that should match the observable behavior of the official Microsoft CsWinRT projection.
---

# CsWinRT Reference

Use this skill to keep `kotlin-winrt` aligned with the official `microsoft/CsWinRT` projection model.

Start from the local repository contract in `README.md`: when local behavior and the Microsoft projections disagree, change `kotlin-winrt` to match the reference projection model instead of inventing project-specific rules.

## Workflow

1. Identify whether the task is runtime, generator, or app-facing API shape work.
2. Read the local Kotlin implementation first so the comparison is concrete.
3. Load the smallest relevant reference file from `references/`.
4. Compare semantics, not surface syntax.
5. Preserve Kotlin idioms only when they do not change WinRT-observable behavior.

## Comparison Rules

- Treat `CsWinRT` as a behavioral oracle, not a line-by-line code template.
- Match activation rules, default interface selection, implemented interface exposure, factory/static access, delegate lifetime, and generic type identity before considering naming differences.
- Compare ABI boundaries explicitly: ownership, `AddRef`/`Release`, boxing, string/HSTRING flow, HRESULT translation, and event token handling.
- For generator changes, compare emitted metadata intent and call paths, not just generated member names.
- When `CsWinRT` behavior is split between generated code and runtime helpers, reproduce the same total behavior across `winmd-parser`, `generated-winrt-bindings`, `winrt-core`, and `kom`.

## Reference Files

- `references/overview.md`: what `CsWinRT` is, what parts matter for this repository, and official upstream links.
- `references/upstream-map.md`: where to look in the `CsWinRT` repo and docs for a given task.
- `references/kotlin-winrt-map.md`: which local Kotlin modules and files own which WinRT behaviors.
- `references/implementation-topics.md`: implementation-oriented rules for interface projection, activation, delegates, generic IIDs, and marshaling.
- `references/review-checklists.md`: concrete checks to run before finalizing a projection/runtime change.
- `references/examples.md`: task-shaped examples that map common `kotlin-winrt` changes to the right local files and upstream evidence.

Load only the file that matches the current task.

## Working Conventions

- Prefer official `microsoft/CsWinRT` docs, source, and repo paths over secondary explanations.
- If the current task depends on a specific upstream implementation detail, inspect the live GitHub repository before finalizing the change.
- State explicitly when a conclusion is inferred from multiple upstream sources rather than spelled out in one place.
- If `CsWinRT` and `C++/WinRT` appear to disagree, treat that as a projection-model question and check both before changing Kotlin behavior.
- When touching generated bindings, inspect both the generator input path and the generated Kotlin output. Projection bugs often come from the split between metadata modeling and rendering.
- Keep changes evidence-driven: note which upstream doc or source area justified the local behavior.
