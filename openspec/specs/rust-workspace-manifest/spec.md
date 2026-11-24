# rust-workspace-manifest Specification

## Purpose

Defines how the plugin keeps the `members` list of the workspace `Cargo.toml` in Rust course creator projects in sync
with the course lessons, so Cargo sees every lesson's task packages.

## Requirements

### Requirement: Lesson is added to workspace members
In a Rust course creator project with a workspace `Cargo.toml`, when the first task of a lesson is created, the plugin
SHALL add a `"<lesson path>/*/"` entry to the workspace `members` array, ordered by lesson position, unless an entry
for the lesson already exists.

#### Scenario: First lesson in a course
- **WHEN** the first task of the first lesson is created in a course whose `Cargo.toml` has an empty `members` array
- **THEN** `members` contains exactly one entry `"<lesson path>/*/"`, followed by a comma and a newline

#### Scenario: Lesson entry already exists
- **WHEN** the first task of a lesson is created and `members` already contains an entry starting with the lesson path
- **THEN** `Cargo.toml` is not changed

### Requirement: Updated workspace manifest is saved completely
After the plugin updates the workspace `Cargo.toml` (adding or removing lesson members), the file on disk SHALL contain
the complete updated text, including all pending PSI changes, and the document of `Cargo.toml` SHALL have no unsaved
changes.

#### Scenario: Manifest is saved after adding a lesson
- **WHEN** the first task of a new lesson is created in a Rust course creator project
- **THEN** the `Cargo.toml` document has no unsaved changes and the file content on disk equals the document text

#### Scenario: Manifest is saved after removing a lesson
- **WHEN** a lesson listed in the workspace `members` is deleted in a Rust course creator project
- **THEN** after the update is applied, the `Cargo.toml` document has no unsaved changes and the file content on disk equals the document text
