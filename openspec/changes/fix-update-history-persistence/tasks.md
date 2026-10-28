# Tasks

## 1. Persist update history with kotlinx.serialization

- [ ] 1.1 Annotate `UpdateHistoryService.State` and `UpdateItem` in `UpdateHistoryService.kt` with `@kotlinx.serialization.Serializable` and verify `./gradlew :intellij-plugin:educational-core:classes` succeeds
- [ ] 1.2 Add `UpdateHistoryServiceSerializationTest` (based on `EduSettingsServiceTestBase`) covering empty state, the JSON written after an update, round trip of several entries with both updater implementations, and legacy XML read as an empty state; verify with `./gradlew :intellij-plugin:educational-core:test --tests "com.jetbrains.edu.learning.update.UpdateHistoryServiceSerializationTest"`
- [ ] 1.3 Verify existing history users still pass: `./gradlew :intellij-plugin:educational-core:test --tests "com.jetbrains.edu.learning.marketplace.MarketplaceCourseUpdateTest" --tests "com.jetbrains.edu.learning.update.*" --continue -PexcludeTests=**/slow/**`

## 2. Manual verification in a real IDE

- [ ] 2.1 Optionally add a temporary internal action (`internal="true"`, marked `TODO: remove before merge`) that appends an `UpdateItem` to the current project's `UpdateHistoryService`, so the history can be filled without publishing a course update; verify it is available via Find Action in `runIde`
- [ ] 2.2 With the old implementation (without `@Serializable`), add an entry, save the project and reopen it; verify `.idea/workspace.xml` contains `EduUpdateHistory` with an empty `<UpdateItem />`, and `idea.log` has the SEVERE from EDU-9056 and an empty `Update history:`
- [ ] 2.3 With the new implementation, open the same project with the legacy `workspace.xml`; verify there is no error report and `Update history:` in `idea.log` is empty
- [ ] 2.4 With the new implementation, add entries, save and reopen the project; verify `workspace.xml` contains the JSON state and `Course opened: ... Update history:` lists the added entries
- [ ] 2.5 If the temporary action was added, remove it and its registration; verify `git diff master --stat` does not list them, `xmllint --noout` passes on `educational-core.xml` and `./gradlew :intellij-plugin:educational-core:testClasses` succeeds

## Workflow follow-up

- Archive the change and sync `course-update-history` into `openspec/specs/` after review.
