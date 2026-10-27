# task-run-configurations Specification

## Purpose

Keeps run configurations shipped inside course task directories bound to the correct project module in
learner course projects, so learners can run them after the course project is created or its modules change.

## Requirements

### Requirement: Task run configuration module is fixed in learner projects
In a learner course project, the plugin SHALL set the module of every module-based run configuration stored
in a task directory to the module containing that task's source directory. This SHALL happen when such a
configuration is added and after project roots change. Non-course and course creator projects SHALL NOT be
affected.

#### Scenario: Run configuration added in a learner project
- **WHEN** a module-based run configuration without a module is added in a learner course project and is stored in a task directory
- **THEN** the run configuration's module eventually becomes the module containing the task's source directory

#### Scenario: Roots change in a learner project
- **WHEN** project roots change in a learner course project that has a module-based run configuration stored in a task directory, and the configuration's module is not set
- **THEN** the run configuration's module eventually becomes the module containing the task's source directory

#### Scenario: Non-course project
- **WHEN** project roots change in a project that is not a course project
- **THEN** no run configuration is modified

#### Scenario: Run configuration added in a course creator project
- **WHEN** a module-based run configuration without a module is added in a course creator project and is stored in a task directory
- **THEN** the run configuration's module stays unset

#### Scenario: Roots change in a course creator project
- **WHEN** project roots change in a course creator project that has a module-based run configuration without a module stored in a task directory
- **THEN** the run configuration's module stays unset

### Requirement: Module fix does not block the UI thread
Handling a project roots change or an added run configuration for task run configurations SHALL NOT perform
file system lookups, course loading or module resolution on the UI thread, in any project. Only applying an
already resolved module to a run configuration MAY happen on the UI thread.

#### Scenario: Roots change in a non-course project
- **WHEN** project roots change in a project that is not a course project
- **THEN** no slow operation is reported on the UI thread by the run configuration module fix

#### Scenario: Run configuration added
- **WHEN** a run configuration is added in any project
- **THEN** no file system lookup, course loading or module resolution is performed on the UI thread by the run configuration module fix

#### Scenario: First roots change before the course is loaded
- **WHEN** project roots change in a learner course project before the course model has been loaded
- **THEN** any course loading triggered by the module fix happens off the UI thread and outside a write action
