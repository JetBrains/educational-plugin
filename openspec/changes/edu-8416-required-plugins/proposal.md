# Proposal

## Why

Course validation integration tests (EDU-8416) run the `validateCourse` command in a real IDE that has only the built
JetBrains Academy plugin installed. Some technologies need more plugins in that IDE (for example, a language plugin
that is not bundled into the tested IDE). Right now a test has no declarative way to say so. Each test base would have
to hook into the IDE setup on its own, so every technology would solve the same problem in its own way.

## What Changes

- New repeatable test annotation that declares the id of a Marketplace plugin the test needs. It works on a test
  method and on a test class (including an abstract base class shared by several test classes).
- `CourseValidationTestBase` collects every declared plugin id for the current test, installs each one from the
  Plugin Manager (the latest version compatible with the tested IDE build), and then installs the built plugin.
- The `protected open fun installPlugin(configurator, pluginPath)` hook of `CourseValidationTestBase` is removed.
  The annotation becomes the only way to add plugins to the tested IDE. The hook has no overrides on master.
- `@RequiredProperty` on a test class now applies to its subclasses too: both annotations are collected by the same
  lookup (test class, its superclasses, test method).
- `intellij-plugin/integration-tests/README.md` describes the new annotation next to `@RequiredProperty`.

Out of scope:
- pinning a plugin version or release channel.

## Capabilities

### New Capabilities

- `course-validation-tests`: how course validation integration tests prepare the IDE they run in, starting with the
  plugins installed into it.

### Modified Capabilities

None.

## Impact

- `intellij-plugin/integration-tests/testSrc/com/jetbrains/edu/slow/checkerTests/CourseValidationTestBase.kt`: plugin
  installation logic.
- New file `intellij-plugin/integration-tests/testSrc/com/jetbrains/edu/slow/checkerTests/RequiredPlugin.kt`.
- `intellij-plugin/integration-tests/README.md`.
- Tests that declare required plugins need network access to the JetBrains Marketplace (downloads are cached by
  IntelliJ Starter per IDE build). Existing tests without the annotation behave exactly as before.
- No production code changes.
