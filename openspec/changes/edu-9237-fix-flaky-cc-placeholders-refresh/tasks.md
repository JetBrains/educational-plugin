# Tasks

Test commands below run from the worktree root; add `-PenvironmentName=262` for platform 262 (default is 263).
Gradle build cache is enabled (`org.gradle.caching=true`), so repeated runs pass `--rerun` to the test task
(`cleanTest` alone restores results from the cache). Retry a failed non-target test up to 2
times before treating it as a regression.

## 1. Remove the global VFS refresh from course creator placeholder initialization

- [x] 1.1 Record the baseline: run `./gradlew :intellij-plugin:Edu-JavaScript:test --rerun --continue -PexcludeTests=**/slow/**` 5 times on 263 and note how many runs fail `JsAddAnswerPlaceholderActionTest.test placeholder text` with "Cannot perform 'Undo last operation'" (the whole module is run, so `JsTaskDescriptionHighlightingTest` precedes the test as on CI); a zero baseline is not a blocker, but note it in the report
- [x] 1.2 Confirm that course generation writes files only through VFS: read `GeneratorUtils.createCourse`, `CourseProjectGenerator.createAdditionalFiles` and `createInitialLesson` / `createAdditionalFiles` overrides of course builders and project generators in language modules (look for `java.io.File`, `java.nio.file.Files`, `FileUtil` writes into the course dir); verify by listing any found non-VFS writes in the report, and if a write into the course dir outside VFS exists, stop and revisit design decision 1 before changing code
- [x] 1.3 Remove `VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)` and the then unused import from `CCUtils.initializeCCPlaceholders` in `intellij-plugin/educational-core/src/com/jetbrains/edu/coursecreator/CCUtils.kt`; verify with `./gradlew :intellij-plugin:educational-core:testClasses` and `get_file_problems` on the file
- [x] 1.4 Run `./gradlew :intellij-plugin:educational-core:test --continue -PexcludeTests=**/slow/**` on 263 and verify no new failures (it covers `CourseGenerationTest`, `CreateCourseFromZipTest`, course creator placeholder and course update tests that rely on `initializeCCPlaceholders` / `initializeTaskPlaceholders`)
- [x] 1.5 Run the Edu-JavaScript module tests as in 1.1 10 times on 263 and 5 times on 262 and verify `JsAddAnswerPlaceholderActionTest` never fails
- [x] 1.6 Commit as `EDU-9237: <subject>` (only `CCUtils.kt`) and verify with `git show --stat HEAD`

## 2. Save the Rust workspace manifest after postponed PSI operations (conditional)

Group 2 starts only after group 1 is committed. Task 2.1 is the gate: tasks 2.2-2.4 are done only if the
assertion fails without the fix.

- [x] 2.1 In `intellij-plugin/Edu-Rust/testSrc/com/jetbrains/edu/rust/actions/RsActionTestBase.kt`, make `checkCargoToml` assert that the `Cargo.toml` document has no unsaved changes (`FileDocumentManager.isFileModified(manifest)` is false) before opening it in the editor; with the group 1 change in place and without any `RsCourseBuilder` change, run `./gradlew :intellij-plugin:Edu-Rust:test --tests "com.jetbrains.edu.rust.actions.RsCreateLessonTest" --tests "com.jetbrains.edu.rust.actions.RsDeleteActionTest" --continue -PexcludeTests=**/slow/**` on 263; if the new assertion fails (expected for `test add the first lesson in course`), go on with 2.2; if it passes, STOP: revert the test change, do not commit anything from group 2, skip 2.2-2.4 and report the result to the user for discussion (group 3 waits for that decision)
- [x] 2.2 In `intellij-plugin/Edu-Rust/src/com/jetbrains/edu/rust/RsCourseBuilder.kt`, call `PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)` in the private `PsiElement.saveDocument()` helper before `FileDocumentManager.getInstance().saveDocument(document)`; verify the tests from 2.1 pass on 263 and 262, and `get_file_problems` on the changed files shows no new problems
- [x] 2.3 Run `./gradlew :intellij-plugin:Edu-Rust:test --continue -PexcludeTests=**/slow/**` on 263 and 262 and verify no new failures; run `RsCreateLessonTest` 10 times on 263 (`test --rerun --tests`) and verify it never fails
- [x] 2.4 Commit as `EDU-9237: <subject>` (`RsCourseBuilder.kt` and `RsActionTestBase.kt`) and verify with `git show --stat HEAD`

## 3. Integration checks

- [x] 3.1 Run `./gradlew test --continue -PexcludeTests=**/slow/**` on 263 and verify no new failures (course generation tests of all language modules, e.g. `*CourseBuilderTest`, `JCourseGeneratorTest`, `EduCreateCourseCommandTest`, use the changed code); retry failing tests up to 2 times, and for failures that remain, check whether they also fail on master or are known flaky ones before treating them as regressions
- [ ] 3.2 Run `./gradlew :intellij-plugin:educational-core:test --continue -PexcludeTests=**/slow/**` on 262 and verify no new failures

## 4. Document rerunning tests

- [x] 4.1 In `AGENTS.md`, section "Run tests", after the single test class example, document that the Gradle build cache may restore test results (`FROM-CACHE`) even after `cleanTest` and that `--rerun` really runs tests again; commit separately as `EDU-9237: Document --rerun for tests in AGENTS.md` and verify with `git show --stat HEAD` that only `AGENTS.md` is in the commit

## Workflow follow-up

- No push without explicit approval.
- Manual check by the user in `runIde` after the implementation is committed: create a new course in course creator
  mode and a course creator project from a course archive with answer placeholders; placeholders show possible
  answers, all course files are visible in the Project view, no errors are logged. In a Rust course creator project
  add a lesson with a task and delete a lesson; `Cargo.toml` on disk contains the updated members and the editor shows
  no unsaved changes.
- Archive the change with `/openspec-archive-change` after review, syncing `course-project-generation` and
  `rust-workspace-manifest` into `openspec/specs/`.
