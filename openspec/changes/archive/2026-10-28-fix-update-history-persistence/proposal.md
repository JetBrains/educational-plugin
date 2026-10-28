# Proposal

## Why

`UpdateHistoryService` (EDU-9056) loses the course update history on project reopen and logs a SEVERE
`IllegalArgumentException` from `KotlinAwareBeanBinding.createUsingKotlin` every time a project with a non-empty
history is opened. `UpdateItem` is a data class with `val` properties and no default values, so the XML serializer
writes it as an empty `<UpdateItem />` and cannot instantiate it back. The history exists to be attached to
YouTrack reports created via `CreateNewYouTrackIssue` (EDU-8421), and today it only survives until the IDE restarts.

## What Changes

- Persist `UpdateHistoryService.State` with kotlinx.serialization: the state is stored as JSON inside a CDATA
  section of the `EduUpdateHistory` component in `workspace.xml`, so every `UpdateItem` field is written and read
  back.
- State saved in the old XML format is read as an empty history without an exception. Nothing is lost: the old
  format never contained item values.
- Add serialization tests for the service: empty state, state after an update, round trip, legacy XML.
- Verify the behavior manually in a real IDE, including a project with state saved by the old implementation. A
  temporary internal action that adds an update entry may be used for that and is removed before merge.

## Capabilities

### New Capabilities
- `course-update-history`: per-project history of course updates applied to a student project, its persistence
  across project reopening, and its use in diagnostics.

### Modified Capabilities

## Impact

- `intellij-plugin/educational-core/src/com/jetbrains/edu/learning/update/UpdateHistoryService.kt`: persisted
  state format changes from xmlb XML to JSON. The component name and storage (`EduUpdateHistory`,
  `workspace.xml`, roaming disabled) stay the same.
- New test `intellij-plugin/educational-core/testSrc/com/jetbrains/edu/learning/update/UpdateHistoryServiceSerializationTest.kt`.
- A temporary internal action for manual verification may be added to `educational-core`; it is removed before
  merge and is not part of the final diff.
- No new dependencies: kotlinx.serialization is already used for persistent state in `educational-core`
  (e.g. `CourseCertificateBannerManager`).
