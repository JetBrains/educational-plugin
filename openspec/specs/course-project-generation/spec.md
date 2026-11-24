# course-project-generation Specification

## Purpose

Defines the side effects of generating a course project on the file system and on the IDE's virtual file system,
so that course generation does not interfere with files and projects outside the generated course.

## Requirements

### Requirement: Course creator placeholder initialization stays in memory
When a course creator project is generated, the plugin SHALL replace the placeholder texts of task files with their
possible answers in memory only, before the course files are written to disk. Placeholder initialization SHALL NOT
modify, create or delete files on disk.

#### Scenario: Placeholders are initialized in a generated course creator project
- **WHEN** a course creator project is generated for a course with a task file containing an answer placeholder
- **THEN** the task file written to disk contains the placeholder's possible answer at the placeholder position

#### Scenario: No files are touched before the course structure is written
- **WHEN** placeholders are initialized during course creator project generation
- **THEN** no file on disk is created, modified or deleted by the placeholder initialization

### Requirement: Course generation does not refresh unrelated file systems
Generating a course project SHALL NOT start a refresh of the whole virtual file system or of locations outside the
course directory. Only the course directory MAY be refreshed, and such a refresh SHALL complete before course files are
generated, so no VFS events from it are delivered later into unrelated commands.

#### Scenario: Course creator project generation
- **WHEN** a course creator project is generated
- **THEN** no refresh of file systems or directories outside the course directory is started by the course generation

#### Scenario: Later command after course generation
- **WHEN** a command (for example, adding an answer placeholder) is executed in a course creator project after the project was generated
- **THEN** the command's undo group contains no VFS changes caused by a refresh started by the course generation, and the command can be undone
