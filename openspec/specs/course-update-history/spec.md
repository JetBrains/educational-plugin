# course-update-history Specification

## Purpose

Keeps a per-project record of course updates applied to a student project, so that diagnostics such as issue
reports can show which course versions the project went through.

## Requirements

### Requirement: Update is recorded in history
The system SHALL append an entry to the project's update history every time a course update is applied. Each entry
SHALL contain the course version before the update, the version after the update and the updater implementation
that applied it.

#### Scenario: Course update is applied
- **WHEN** a course update from version 2 to version 3 is applied to a student project
- **THEN** the project's update history contains an entry `2->3`

### Requirement: Update history survives project reopening
The system SHALL persist the update history per project and restore it with all entry values when the project is
opened again. The history SHALL NOT be shared between projects or synced across machines.

#### Scenario: Project with update history is reopened
- **WHEN** a project's update history contains entries `1->2` and `2->3` and the project is closed and opened again
- **THEN** the update history contains entries `1->2` and `2->3` with the same updater implementations

#### Scenario: Project without updates is reopened
- **WHEN** no course update was applied to a project and the project is closed and opened again
- **THEN** the update history is empty

### Requirement: Unreadable stored history does not break project opening
When the stored update history cannot be read, including history saved by plugin versions before this change, the
system SHALL start with an empty history and SHALL NOT report an error. Opening the project SHALL proceed normally.

#### Scenario: History saved by an older plugin version
- **WHEN** a project is opened whose stored update history was saved by a plugin version before this change
- **THEN** the project opens without an error report and the update history is empty

#### Scenario: New updates after reading an old history
- **WHEN** a project with history saved by an older plugin version is opened and a course update from version 3 to
  version 4 is applied
- **THEN** after the project is reopened, the update history contains the entry `3->4`
