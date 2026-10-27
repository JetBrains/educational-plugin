# Proposal

## Why

`FixModuleForRunConfigurationsListener` runs its `rootsChanged` handler on EDT. The handler checks
`isEduProject()`, which may initialize `StudyTaskManager`: a VFS lookup of `course-info.yaml` in every
project (even non-course ones) on every roots change, and a full YAML course load in course projects.
This produces "Slow operations are prohibited on EDT" reports (EDU-7941: 1041 reports from 346 users in a
month across all IDEs) and can freeze the UI.

## What Changes

- The roots-change handler of `FixModuleForRunConfigurationsListener` stops doing slow work on EDT:
  the learner-mode check, file lookups and module resolution move to a background thread; only applying the
  resolved module to the run configuration happens on EDT.
- The handler still runs outside the write action, preserving the existing deadlock protection around
  `StudyTaskManager` initialization.
- The configuration-added handler (`runConfigurationAdded`) moves off EDT the same way: the check and module
  resolution run in background, only applying the module happens on EDT. So the module of an added task run
  configuration is set asynchronously.
- No user-visible behavior change besides removing UI freezes: task run configurations in learner course
  projects still get the module of the task source directory.

## Capabilities

### New Capabilities
- `task-run-configurations`: how the plugin keeps task run configurations (stored inside task directories)
  bound to the correct module in learner course projects.

### Modified Capabilities

## Impact

- Code: `intellij-plugin/educational-core/src/com/jetbrains/edu/learning/checker/FixModuleForRunConfigurationsListener.kt`
  (delegates to the new project service `FixModuleForRunConfigurationsService` with an injected `CoroutineScope`;
  listener registrations in `resources/META-INF/educational-core.xml` stay the same).
- Affects all language modules relying on task run configurations (e.g. JVM/Gradle-based courses).
- No API, dependency or course format changes.
