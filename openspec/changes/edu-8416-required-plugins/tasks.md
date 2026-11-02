# Tasks

## 1. Required plugins declaration and installation

- [ ] 1.1 Add `RequiredPlugin.kt` to `intellij-plugin/integration-tests/testSrc/com/jetbrains/edu/slow/checkerTests/`: `@Repeatable @Retention(RUNTIME) @Target(FUNCTION, CLASS) annotation class RequiredPlugin(val pluginId: String)`, with KDoc in the style of `RequiredProperty.kt` that says class-level declarations are also read from superclasses; verify with `get_file_problems` on the file
- [ ] 1.2 In `CourseValidationTestBase`: add private `requiredPlugins(): Set<String>` (a `linkedSetOf` over `RequiredPlugin` of `javaClass` and all its superclasses, then of `testMethod()`), install each id with `PluginConfigurator(this).installPluginFromPluginManager(pluginId, ide)` before `installPluginFromPath(pluginPath)` in `doTest()`, remove the `protected open fun installPlugin` hook, and mention `RequiredPlugin` in the class KDoc; verify with `./gradlew :intellij-plugin:integration-tests:testClasses`
- [ ] 1.3 Document `@RequiredPlugin` in `intellij-plugin/integration-tests/README.md` next to the `@RequiredProperty` paragraph in "Running the tests" (plugin id, method/class/base-class targets, latest compatible version from the Marketplace); verify by reading the rendered section

## 2. Integration checks

- [ ] 2.1 Regression: run `./gradlew :intellij-plugin:integration-tests:test --tests "com.jetbrains.edu.slow.checkerTests.plain.PlainTextCourseValidationTest"` without annotations and verify it passes as on master (retry flaky failures up to 2 times)
- [ ] 2.2 Positive case: temporarily annotate `PlainTextCourseValidationTest` with `@RequiredPlugin("org.intellij.scala")` on the class and on the method, run the same command, and verify that the starter log shows `Setting up plugin: org.intellij.scala` once, the plugin is present in the test IDE plugins dir under `out/ide-tests`, and the test passes; revert the temporary annotation
- [ ] 2.3 Negative case: temporarily annotate the test with a non-existent id (e.g. `@RequiredPlugin("com.jetbrains.edu.non.existent.plugin")`), run it, and verify it fails before the IDE launch with an error naming the id; revert the temporary annotation and check that `git status` shows only the intended changes

## Workflow follow-up

- Commit as `EDU-8416: <subject>` on `arseniy/EDU-8416-required-plugins` only on explicit request; no push without approval.
- Archive the change with `/openspec-archive-change` after review, syncing `course-validation-tests` into `openspec/specs/`.
