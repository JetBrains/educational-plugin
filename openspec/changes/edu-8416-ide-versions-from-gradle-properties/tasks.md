# Tasks

## 1. Pass tested IDE versions from Gradle

- [x] 1.1 In `intellij-plugin/integration-tests/build.gradle.kts`, add a private function that turns the version part of `<code>-<version>` into `(version, buildType)`: if it fully matches `\d{4}(\.\d+)+`, return `(version, "RELEASE")`, otherwise `(platformVersion, "EAP")`. In the `test` task, next to `path.to.build.plugin`, iterate over `listOf(ideaVersion, pycharmVersion, clionVersion, riderVersion)` and set `edu.integration.tests.<code>.version` / `edu.integration.tests.<code>.build.type`, where `<code>` is the product code before `-` (`IU`, `PY`, `CL`, `RD`), with a short comment on the EAP simplification. Verify with `./gradlew :intellij-plugin:integration-tests:testClasses`
- [x] 1.2 Verify the computed values for both platforms without running the IDE: temporarily log the eight values from the `test` task configuration, then run `./gradlew :intellij-plugin:integration-tests:help` with default `environmentName` (expect idea `2026.3`, pycharm `2026.3`, clion `2026.3.0.1`, rider `2026.3`, all `RELEASE`) and with `-PenvironmentName=262` (expect idea `2026.2.0.1`, pycharm `2026.2`, clion `2026.2.0.1`, rider `2026.2`, all `RELEASE`). Temporarily set `ideaVersion=IU-263.7307-EAP-CANDIDATE-SNAPSHOT` and `riderVersion=RD-2026.3-EAP8-SNAPSHOT` in `gradle-263.properties` and expect `2026.3`/`EAP` for both. Revert the temporary logging and property edits

## 2. Read versions in tests and drop hand-written constants

- [x] 2.1 In `intellij-plugin/integration-tests/testSrc/com/jetbrains/edu/slow/checkerTests/ideInfoUtils.kt`, add a private helper that reads a required system property and fails with `System property '<name>' is not set`. Build `ideaUltimate()` / `pyCharm()` from `edu.integration.tests.<IdeInfo.productCode>.version` and `BuildType.valueOf(edu.integration.tests.<IdeInfo.productCode>.build.type).type`, keeping their public signatures, and add `clion()` / `rider()` built the same way from `IdeInfo.CLion` / `IdeInfo.Rider` (imports from `com.intellij.tools.ide.starter.product.clion` / `.rider`). Verify with `get_file_problems` on the file
- [x] 2.2 Delete `intellij-plugin/integration-tests/branches/262/testSrc/com/jetbrains/edu/slow/checkerTests/ideVersionConstants.kt` and `.../branches/263/...` (so the `integration-tests/branches` directory is gone). Verify with `./gradlew :intellij-plugin:integration-tests:testClasses` and with `./gradlew :intellij-plugin:integration-tests:testClasses -PenvironmentName=262`
- [x] 2.3 Update "How it works" in `intellij-plugin/integration-tests/README.md`: the tested IDE versions come from `ideaVersion` / `pycharmVersion` / `clionVersion` / `riderVersion` of the current platform properties, releases are used as is, and EAP versions are reduced to the latest EAP build of the `20XY.Z` major version. The `test` task passes them as `edu.integration.tests.*` system properties. Verify by reading the rendered section

## 3. Integration checks

- [x] 3.1 Run `./gradlew :intellij-plugin:integration-tests:test --tests "com.jetbrains.edu.slow.checkerTests.plain.PlainTextCourseValidationTest"` and verify that it passes and that the Starter log under `out/ide-tests` shows the IntelliJ IDEA Ultimate `2026.3` release being downloaded/used (retry flaky failures up to 2 times)
- [x] 3.2 Run any course validation test class directly (outside the Gradle task, without `edu.integration.tests.*` properties) or temporarily drop one `systemProperty` call, and verify that it fails before the IDE download with a message naming the missing property. Revert any temporary change and check that `git status` shows only the intended changes

## Workflow follow-up

- Commit as `EDU-8416: <subject>` on `arseniy/EDU-8416-ide-versions-from-properties` only on explicit request. No push without approval.
- Archive the change with `/openspec-archive-change` after review, syncing `course-validation-tests` into `openspec/specs/`.
