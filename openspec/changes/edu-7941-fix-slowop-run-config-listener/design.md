# Design

## Context

See proposal.md - Why. Current flow in `FixModuleForRunConfigurationsListener.rootsChanged`:

```
rootsChanged (inside write action)
  -> project.invokeLater { }                       EDT
       learnerMode() -> isEduProject()
         -> StudyTaskManager.getInstance()
              needToLoadCourse -> isEduYamlProject  VFS lookup
              runReadAction { loadCourse(project) } YAML deep load
       RunManager.allSettings.forEach { updateConfiguration(it) }
         findFileByPath, getTaskDir, getContainingTask,
         findSourceDir, ModuleUtilCore.findModuleForFile, setModule
```

Constraint: `invokeLater` was introduced in EDU-7737 (commit `6b4ff81b70`, "avoid deadlock on project
opening") to leave the write action, because `StudyTaskManager`
initialization takes a read action and then `courseLoadingLock`; calling it inside a write action can
deadlock. Any new code path must also call `learnerMode()` outside a write action.

## Goals / Non-Goals

**Goals:**
- Nothing slow runs on EDT in the roots-change and configuration-added paths (see specs).
- Keep the deadlock protection.

**Non-Goals:**
- Changing `StudyTaskManager` lazy initialization or `isEduProject()` semantics.

## Decisions

1. **Move the work into a project service with an injected `CoroutineScope`.** The new light service
   `FixModuleForRunConfigurationsService(project: Project, scope: CoroutineScope)` holds the logic; the platform
   supplies a project-lifetime scope, cancelled on project close, so no work runs on a disposed project.
   `FixModuleForRunConfigurationsListener(project)` stays registered for both topics and only delegates to the
   service. `fixConfigurationModules` returns the launched `Job`. The service exists so that tests can mock it and wait for
   these jobs (see 5): there is no public API to replace a listener registered in `plugin.xml`.
   Alternatives: inject the scope into the listener itself - simpler, but then tests can't reach the scope and
   have to call listener methods directly, losing real events and the write action context of `rootsChanged`;
   `EduCoroutineScopeService.getInstance(project).scope` - shared with other code, so waiting for its jobs in
   tests would also wait for unrelated work.

2. **Both handlers launch the same coroutine on `Dispatchers.Default`.** Inside the coroutine:
   - `if (!learnerMode()) return` - runs on a BGT, never inside a write action, so the lock ordering in
     `StudyTaskManager.initializeCourse` (read lock first, then `courseLoadingLock`) is preserved.
   - Take the settings to fix: `RunManager.allSettings` (thread-safe snapshot) for `rootsChanged`, only the
     added settings for `runConfigurationAdded`; in `readAction { }` resolve the target module for each of them.
   - `withContext(Dispatchers.EDT)` apply `configuration.setModule(module)` for resolved pairs.
   `runConfigurationAdded` is not in the reports, but it does the same slow checks (`learnerMode()`, VFS and
   module lookups) on EDT when `RunManager` adds a configuration, so it is handled the same way.
   Alternative: keep `invokeLater` and only add a cheap guard (skip until course is loaded). Rejected: VFS
   lookups in `updateConfiguration` and `guessCourseDir` would still run on EDT.

3. **Split `updateConfiguration` into resolve + apply.** A private function resolves a private data class
   holding the module-based configuration and the module it should use (logging `LOG.error` on `Err` as
   today), and the apply step calls `setModule`. The data class only names the pair for
   readability; no other new types besides the service from decision 1. Both handlers resolve in a read action in background and apply on EDT.

4. **Coalescing is not added.** Roots changes come in bursts; each launches a coroutine. The work per launch
   is cheap once the course is loaded, and the previous code had the same per-event cost on EDT. If needed
   later, a `MutableSharedFlow`/conflation can be introduced without changing behavior.

5. **Cover the listener with a heavy test in `educational-core`.** No existing test exercises it: tests with
   task run configurations either build educator courses (the listener is a no-op there) or are slow checker
   tests that need a real JDK/Gradle. The new `FixModuleForRunConfigurationsListenerTest` is based on
   `CourseGenerationTestBase`:
   - Heavy, because the listener resolves the `.run.xml` file via `LocalFileSystem`, while light tests keep the
     course in the in-memory `temp://` file system, so the listener would never find the file there.
   - `educational-core` has no Java plugin, so the test registers its own minimal `ModuleBasedConfiguration`
     type in `ConfigurationType.CONFIGURATION_TYPE_EP` for the test duration.
   - A plain text course is used: its source dir is the task dir, so the expected module is the module of the
     task dir created by the platform for the generated project.
   - Both handlers are asynchronous, and in a course creator project they change nothing, so there is no
     result to wait for. After the course project is created, tests mock `FixModuleForRunConfigurationsService`
     with `mockService` (a spy calling the real `fixConfigurationModules`), collect the jobs it returns, and wait with
     `PlatformTestUtil.waitWithEventsDispatching` until they all complete. Job completion is the signal, so
     "nothing changed" cases are verifiable.
   - Events come from the IDE: `RunManager.addConfiguration` and `ProjectRootManagerEx.makeRootsChange` in a
     write action, so the `plugin.xml` registrations and the write action context of `rootsChanged` are covered.
     Real `Dispatchers.Default` and `Dispatchers.EDT` are used, no dispatcher injection is needed.
   - Fallback if waiting on child jobs proves unreliable: inject background and UI dispatchers and use
     `runTest` with `StandardTestDispatcher` + `advanceUntilIdle()` (like `UserAgreementManagerTest`).
   Alternative: a light `EduTestCase` test. Rejected: it would require changing how the listener looks up the
   file only for testability.

## Risks / Trade-offs

- [Module is set slightly later than before, asynchronously] -> Acceptable: on roots change it was already
  deferred via `invokeLater`; on configuration added the module is set shortly after the configuration is
  registered, before a user can run it in practice. Tests wait for the listener jobs instead of relying on the
  EDT queue order.
- [Run configuration removed or changed between resolve and apply] -> Applying `setModule` to a configuration
  no longer registered is harmless; `module` may be null as in the current code.
- [Read action cancelled by a write action] -> Use the cancellable coroutine `readAction` so it restarts
  automatically.
