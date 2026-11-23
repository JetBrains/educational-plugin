# Design

## Context

See proposal.md - Why. Current course creator generation flow (`CourseProjectGenerator`):

```
createProject
  LocalFileSystem.refreshAndFindFileByIoFile(location)
  VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)      sync refresh of the course dir
  createCourseStructure -> generateCourseContent
    CCUtils.initializeCCPlaceholders(holder)                    only for !course.isStudy
      initializeTaskPlaceholders: LightVirtualFile + document per task file, in memory
      VirtualFileManager.refreshWithoutFileWatcher(true)        async refresh of ALL file systems
    GeneratorUtils.createCourse(holder, indicator)              writes files via VFS (VfsUtil.saveText, ...)
    createAdditionalFiles(holder)
```

History of the refresh (`git log -L` on `initializeCCPlaceholders`):
- `38a542eba6` (EDU-1411, 2018) moved placeholder initialization from `CCNewCourseActionBase` into
  `GeneratorUtils.initializeCCPlaceholders`, together with `refreshWithoutFileWatcher(true)` and
  `ProjectView.refresh()`. At that time placeholders were initialized after the course structure was created:
  task files were found in the course dir on disk, modified via their documents and saved with `saveDocumentAsIs`,
  so the refresh and the project view update after writing to disk made sense.
- `c66ae997fd` (EDU-1411, 2018) moved the function into `CCUtils` unchanged; `26d3cce574` (EDU-1411, 2018) dropped
  `ProjectView.refresh()` from it.
- `1ea1fa6f37` (EDU-2706, 2019) switched to in-memory `LightVirtualFile` documents and moved the call before course
  structure creation ("Note, it doesn't affect files in file system" KDoc). The refresh was kept by inertia.
- `a5a924c6a5` (EDU-5332, 2022) replaced `project` with `CourseInfoHolder`; the refresh stayed.

So the refresh now runs before any course file is written, is asynchronous, and refreshes nothing the method changed.
Files generated afterwards are written through VFS, so VFS already knows about them, and the course dir itself is
refreshed synchronously in `createProject` before generation.

The refresh's EDT part (applying events) runs at a random later moment, e.g. when `XmlTagNameSynchronizer` (present
with XML/JS plugins) calls `UIUtil.dispatchAllInvocationEvents()` at the start of a write command:
- `JsAddAnswerPlaceholderActionTest`: the deletion of the temp dir of a previous light test
  (`JsTaskDescriptionHighlightingTest`) lands inside the "Add Answer Placeholder" command as a global
  `NonUndoableAction`, so the undo in `CCAnswerPlaceholderTestBase.doTest` fails.
- `RsCreateLessonTest`: the refresh produces a stale update event for `/src/Cargo.toml`. `RsCourseBuilder.updateCargoToml`
  changes PSI inside a write command and saves the document right away in `saveDocument()`, but a postponed PSI change
  (the added whitespace) is applied to the document after the save, at the end of the command, so the document becomes
  unsaved again. When `checkResult` pumps EDT, the stale event meets an unsaved document: "Unexpected memory-disk
  conflict in tests for /src/Cargo.toml".

## Goals / Non-Goals

**Goals:**
- Course creator project generation starts no asynchronous refresh outside the course dir (see specs).
- Rust `Cargo.toml` is fully saved after the plugin updates workspace members, if a deterministic test shows it is
  not today (decisions 2 and 3).

**Non-Goals:**
- Other `refreshWithoutFileWatcher(true)` calls: `EduUtilsKt.synchronize` and `CourseArchiveCreator.synchronize` are
  not on the course generation path and are not involved in the failures.
- Changing test infrastructure (e.g. pumping EDT in setUp) to hide such races.

## Decisions

1. **Remove the refresh from `CCUtils.initializeCCPlaceholders`, do not replace it.** The method does not touch disk,
   and the course dir is already refreshed synchronously (`VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)`)
   in `CourseProjectGenerator.createProject` before generation; all generated files are written through VFS.
   The unused `VirtualFileManager` import is removed with it.
   Alternative: replace with `VfsUtil.markDirtyAndRefresh(false, true, true, holder.courseDir)`. Rejected: it would
   duplicate the refresh already done in `createProject`, and at this point the course dir contains nothing the plugin
   wrote outside VFS. In light tests it would also only rescan the in-memory test source root for no reason.

2. **Conditionally apply postponed PSI operations before saving in `RsCourseBuilder`.** Done only if the assertion
   from decision 3 fails without it, with decision 1 already applied. The private `PsiElement.saveDocument()`
   helper calls `PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)` before
   `FileDocumentManager.saveDocument(document)`. The helper is shared by `updateCargoToml` and `removeLessonMembers`, so
   both paths save the complete text.
   This is independent of decision 1: even without the stale event, the document was left unsaved in the real IDE and
   the file on disk lacked the last PSI change until the next save. Cargo reads the manifest from disk, so saving the
   complete text matters for project refresh.
   Alternative: `PsiDocumentManager.commitDocument` + save. Rejected: committing does not apply postponed formatting
   operations; `doPostponedOperationsAndUnblockDocument` is the documented way to flush them before working with the
   document.

3. **Check the saved state in Rust action tests.** `RsActionTestBase.checkCargoToml` additionally asserts that the
   `Cargo.toml` document has no unsaved changes before opening it in the editor (`FileDocumentManager.isFileModified`).
   It covers both `RsCreateLessonTest` and `RsDeleteActionTest`. Unlike the original flaky failure, this check is
   deterministic, so it is the gate for decision 2: it is written first and run with decision 1 applied but without
   decision 2.
   - It fails (expected for `test add the first lesson in course`): decision 2 is justified; add it and commit the
     fix together with the assertion.
   - It passes: the postponed PSI change is not a real problem on the current platform. Stop: neither the assertion
     (it would pass anyway) nor the `RsCourseBuilder` fix is committed, the result is reported and discussed with the
     user. The `rust-workspace-manifest` spec delta is then revisited together with that decision.
   No deterministic test is added for decision 1: the failure depends on when the async refresh is applied. It is
   verified by repeated runs of the flaky tests together with the test that precedes them on CI.

4. **Up to two implementation commits**: decision 1, and decisions 2 + 3 if decision 2 is kept, so each can be
   reverted separately.

## Risks / Trade-offs

- [Some real IDE scenario relied on the global refresh as a side effect, e.g. picking up files written outside VFS
  by a language course builder] -> The refresh was started before such files could be written and was asynchronous,
  so it could not reliably pick them up. Before removal, check that course generation code
  (`createAdditionalFiles`, `courseBuilder.createInitialLesson`, `GeneratorUtils.createCourse`) writes through VFS,
  and run the full test suite, which includes the course generation tests of all language modules. The user checks
  course creation manually in `runIde` after the implementation is committed.
- [Removing the refresh does not remove all flakiness of `JsAddAnswerPlaceholderActionTest`] -> Repeated runs on 262
  and 263 (see tasks) are the acceptance criterion; if failures remain, investigate the remaining event source
  before finishing.
