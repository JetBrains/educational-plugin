# Proposal

## Why

Two tests fail flakily on TeamCity (EDU-9237): `JsAddAnswerPlaceholderActionTest.test placeholder text`
(21/70 runs on 262, 13/67 on 263) and `RsCreateLessonTest.test add the first lesson in course` (1/67 on 263).
Both are caused by an asynchronous refresh of all file systems started by `CCUtils.initializeCCPlaceholders` on every
course creator project generation: its EDT part is applied at a random later moment and injects unrelated VFS events
into later commands (a non-undoable deletion of another test's temp dir inside the "Add Answer Placeholder" command,
a stale update event for `Cargo.toml` that ends in "Unexpected memory-disk conflict"). The refresh is a leftover from
2018, when placeholder initialization modified files on disk; since EDU-2706 (2019) it works on in-memory files only.

## What Changes

- Course creator project generation no longer starts a refresh of all file systems while initializing placeholders.
  Placeholder initialization stays in-memory, and the course directory is still refreshed synchronously before course
  structure creation, as today.
- Conditional: when the first task of a new lesson is created in a Rust course, `Cargo.toml` is saved only after all
  pending PSI changes are applied to the document, so the file on disk contains the complete updated `members` array
  and the document is not left unsaved. This part is done only if a deterministic test confirms that the document is
  left unsaved today (see design.md, decisions 2 and 3); otherwise it is dropped from this change and discussed
  separately.
- Tests (together with the conditional part): Rust action tests check that `Cargo.toml` has no unsaved changes after
  the plugin updates it.
- No user-visible change besides the Rust `Cargo.toml` being saved completely; no API or course format changes.

## Capabilities

### New Capabilities
- `course-project-generation`: side effects of generating a course project on the file system and VFS (what is
  refreshed, what placeholder initialization touches).
- `rust-workspace-manifest`: how the plugin keeps the `members` list of the Rust workspace `Cargo.toml` in sync with
  course lessons in course creator projects (only if the conditional Rust part is kept).

### Modified Capabilities

## Impact

- Code: `intellij-plugin/educational-core/src/com/jetbrains/edu/coursecreator/CCUtils.kt` (`initializeCCPlaceholders`);
  conditionally `intellij-plugin/Edu-Rust/src/com/jetbrains/edu/rust/RsCourseBuilder.kt` (`saveDocument` helper used
  by `updateCargoToml` and `removeLessonMembers`).
- Tests (conditionally): `intellij-plugin/Edu-Rust/testSrc/com/jetbrains/edu/rust/actions/RsActionTestBase.kt`
  (used by `RsCreateLessonTest` and `RsDeleteActionTest`).
- Affects all course creator course generation paths (new course, course from archive, course from Marketplace in
  educator mode) in every language module.
- Out of scope: other `refreshWithoutFileWatcher(true)` calls (`EduUtilsKt.synchronize`,
  `CourseArchiveCreator.synchronize`), which are not on the course generation path.
