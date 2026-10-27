# Tasks

## 1. Move task run configuration handling off EDT

- [x] 1.1 Add `FixModuleForRunConfigurationsListenerTest` (heavy, `CourseGenerationTestBase`, test-only `ModuleBasedConfiguration` type) before changing the listener, covering the spec scenarios: configuration added in a learner course, roots change in a learner course, configuration added in a course creator course; verify it passes with `./gradlew :intellij-plugin:educational-core:test --tests "com.jetbrains.edu.learning.checker.FixModuleForRunConfigurationsListenerTest"` on the current listener code, and that it fails when `setModule` in `rootsChanged` or the `learnerMode()` check in `runConfigurationAdded` is removed
- [x] 1.2 Move the listener logic into a new light project service `FixModuleForRunConfigurationsService(project: Project, scope: CoroutineScope)` and change `FixModuleForRunConfigurationsListener` constructor to `(project: Project)`, delegating both handlers to the service; keep both registrations in `intellij-plugin/educational-core/resources/META-INF/educational-core.xml` unchanged; verify with `./gradlew :intellij-plugin:educational-core:testClasses`
- [x] 1.3 Split `updateConfiguration` into a private resolve step returning a private data class with the configuration and its module (logs `LOG.error` on `Err` as today), and an apply step (`setModule`); verify it compiles by reading the diff
- [x] 1.4 Add `fixConfigurationModules(getSettings)` to the service: `scope.launch(Dispatchers.Default)` with the `learnerMode()` check, resolve the settings in coroutine `readAction { }`, apply results in `withContext(Dispatchers.EDT)`, return the launched `Job`; call it with `listOf(settings)` from `runConfigurationAdded` and with `RunManager.allSettings` from `rootsChanged`; update the deadlock comment to describe the background thread instead of `invokeLater`; verify with `./gradlew :intellij-plugin:educational-core:testClasses` and `get_file_problems` on the files
- [x] 1.5 Switch `FixModuleForRunConfigurationsListenerTest` to the asynchronous handlers: mock the service with `mockService` to collect the jobs returned by `fixConfigurationModules`, wait for them with `PlatformTestUtil.waitWithEventsDispatching` inside `addTaskRunConfiguration` / `makeRootsChange` (real `RunManager.addConfiguration` and `makeRootsChange` in a write action); add the roots change in a course creator course scenario; verify the test passes several times and fails when `learnerMode()`, `setModule` or the `ModuleRootListener` registration is removed
- [x] 1.6 Run `./gradlew :intellij-plugin:educational-core:test --continue -PexcludeTests=**/slow/**` and verify no new failures, including the test from 1.1 (retry flaky failures up to 2 times)

## 2. Integration checks

- [x] 2.1 Manual check in `runIde`: in a non-course project, add a source root and verify no SlowOp from `FixModuleForRunConfigurationsListener` in the log; open a learner course with a task `runConfigurations/*.run.xml` and verify the run configuration gets the module of the task source dir after open / Gradle sync

## Workflow follow-up

- Commit as `EDU-7941: <subject>` on `arseniy/EDU-7941-fix-SlowOp` only on explicit request; no push without approval.
- Archive the change with `/openspec-archive-change` after review, syncing `task-run-configurations` into `openspec/specs/`.
