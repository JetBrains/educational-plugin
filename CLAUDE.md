---
project: JetBrains Academy plugin
languages: [Kotlin, Java]
framework: IntelliJ Platform SDK
build-system: Gradle
---

# JetBrains Academy plugin

## Module layout

- `edu-format` — shared module with course format: course/lesson/task classes, YAML/JSON/XML serialization, etc.
  Note, this module is used outside the plugin project for reading and parsing course archives.
- `intellij-plugin` — main plugin root, containing:
  - `educational-core/` — core code of the plugin.
  - `jvm-core/` — JVM-shared functionality.
  - `Edu-<Language>/` — language-specific modules.
  - `sql/` and `sql:sql-jvm` — SQL support.
  - `AI/` — core module for AI features
  - `features:*` — feature modules. Some important ones:
    - `command-line` - command line interface for the plugin. Used for communication with the plugin outside of IDE.
    - `lti` — Learning Tools Interoperability support
    - `social-media` - sharing courses achievement in social media.
  - `localization/` — translation resources (Crowdin-synced).

The main rule is to put code into a separate module (not `educational-core`), if it requires some additional dependencies (like language plugin)

## Localization

- Source message bundles live at `<module>/resources/messages/*Bundle.properties`.
- All user-visible strings should be in message bundles and used via `*Bundle.message` methods
- Translations are synced from Crowdin into `intellij-plugin/localization/resources/localization/<lang>/...`.
  **Do not edit** translated files — only the source bundles.

## Support for different IntelliJ Platforms

See [different-platform-versions.md](documentation/different-platform-versions.md)

## Common commands

### Compile

Check compilation of both production and test code:

For the whole project:
```shell
./gradlew testClasses
```

For the specific module:
```shell
./gradlew <gradle-module-path>:testClasses
```

### Run tests

Run all tests in a module:

```shell
./gradlew <gradle-module-path>:test --continue -PexcludeTests=**/slow/**
```

For example, to run tests in `intellij-plugin:educational-core` module:

```shell
./gradlew :intellij-plugin:educational-core:test --continue -PexcludeTests=**/slow/**
```

Run a single test class or some subset:

```shell
./gradlew <gradle-module-path>:test --tests "<FQN or wildcard>"
```

For example, to run all tests from `CreateCourseFromZipTest` class located in `intellij-plugin:educational-core` module:

```shell
./gradlew :intellij-plugin:educational-core:test --tests "com.jetbrains.edu.learning.CreateCourseFromZipTest"
```

## Tests
- Framework: JUnit 4.
- Tests live under `<module>/testSrc/` (not `src/test`).
- Base class:
  - `EduTestCase` for things within single course project scope
  - `EduHeavyTestCase` for tests that require interaction with project instances (like course project generation, opening or reopening projects, etc.)

## Commits

Use the following format for commit messages:
```
<YouTrack ticket ID>: <subject>
    
<detailed description>?
```

Description should describe shortly what the commit does and why. Omit obvious details.

## MCP Tools

Try to use JetBrains MCP tools whenever possible.

- Read: `read_file`
- **Default to `search_symbol` (if available) for classes/methods/fields; use `search_text`/`search_regex` mainly for strings, comments, and non-symbol matches.**
- Inspections & symbol info: `get_file_problems`, `get_symbol_info`
- Refactorings: `rename_refactoring`; use for renames and avoid manual search/replace.
- Formatting: `reformat_file`
- Concurrency checks: `find_threading_requirements_usages`, `find_lock_requirements_usages`
- Project structure & VCS: `get_project_modules`, `get_project_dependencies`, `get_repositories`, `git_status`
- Run configs: `get_run_configurations`, `execute_run_configuration`

## Project issues

Issues for this project are located in https://youtrack.jetbrains.com/issues/EDU YouTrack project