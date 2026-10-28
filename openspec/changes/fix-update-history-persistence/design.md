# Design

## Context

`UpdateHistoryService` is a `SerializablePersistentStateComponent<State>` stored in `workspace.xml` under the
`EduUpdateHistory` component. `State` holds `List<UpdateItem>`, and `UpdateItem` is an immutable data class
(`versionBefore`, `versionAfter`, `updater`) without default values. See proposal.md - Why for the failure.

What the platform does today (observed in `UpdateHistoryServiceSerializationTest` and in a real IDE):
- On save, xmlb skips `val` properties, so each item is written as an empty `<UpdateItem />`.
- On load, `KotlinAwareBeanBinding` instantiates `UpdateItem` through `callBy` with no arguments, which throws
  `IllegalArgumentException`. The component falls back to the default empty state, and the platform logs SEVERE.

`SerializablePersistentStateComponent` switches to kotlinx.serialization when the state class is annotated with
`@kotlinx.serialization.Serializable`. The state is then stored as JSON inside a CDATA section. This is already
used in `educational-core`, e.g. `CourseCertificateBannerManager.BannerState`.

## Goals / Non-Goals

**Goals:**
- Entries are written and read back with all their values.
- Old stored state does not produce an error report.

**Non-Goals:**
- Recovering the history saved in the old format. It never contained item values, so there is nothing to recover.
- Changing how and when entries are added (`MarketplaceCourseUpdater`) or how the history is used
  (`CreateNewYouTrackIssue`, `EduProjectActivity` log line).

## Decisions

### Use kotlinx.serialization for `State` and `UpdateItem`

Annotate `UpdateHistoryService.State` and `UpdateItem` with `@Serializable`. `UpdaterImplementation` is an enum and
is serialized by name without an annotation.

Alternatives considered:
- **Make `UpdateItem` fields `var` with defaults** so xmlb can create and fill it. Works, but turns an immutable
  value into a mutable bean only to satisfy the serializer, and adds meaningless defaults (`""` versions).
- **Store entries as strings (`"2->3"`)**. Simplest XML, but loses structure and needs parsing to restore
  `UpdaterImplementation`.

kotlinx.serialization keeps the data class immutable, requires no defaults and matches an existing pattern in the
module.

### No explicit migration of the legacy format

With `@Serializable`, the old XML (`<option name="updates"><list><UpdateItem /></list></option>`) is not valid
JSON state. The serialization test shows it is read as an empty state without an exception. Since the old format has
no item values, an empty history is the correct result, and no migration code is needed.

### Test through `EduSettingsServiceTestBase`

`UpdateHistoryServiceSerializationTest` covers: empty state, the exact JSON written after an update, round trip
with several entries and both updater implementations, and legacy XML read as an empty state. The existing
`MarketplaceCourseUpdateTest` checks only the in-memory state, which is why the bug was not caught.

## Risks / Trade-offs

- [The real project store may handle legacy state differently from the test helper, e.g. log a warning] ->
  Verified manually in a real IDE (tasks.md, group 2): a project with a legacy `workspace.xml` must open without
  an error report, and new entries must survive reopening.
- [Users of older plugin versions opening a project saved by the new version would fail to read JSON state] ->
  Acceptable: the state is in `workspace.xml`, is not roamed, and older versions already lose this history on every
  reopen.

## Migration Plan

No migration step. Rollback is reverting the annotations; the JSON state is then ignored the same way.
