# Integration tests

This module contains end-to-end integration tests for the JetBrains Academy plugin.

Unlike the rest of the plugin's checker tests, these tests don't run the plugin in-process
inside the test JVM. Instead, each test:

1. downloads a real IDE distribution and installs the actual **built plugin** (the
   `buildPlugin` zip, not plugin classes on the test classpath) into it,
2. launches that IDE headlessly,
3. uses the plugin's `validateCourse` command-line command to open a course project and run the
   Check action on every task in it,
4. and compares the reported results against an expected result fixture.

## The purpose

The plugin already has a large suite of checker tests based on
`com.jetbrains.edu.learning.checker.CheckersTestBase`. Those tests run inside the test JVM with
`isUnitTestMode == true`, so they exercise `isUnitTestMode`-only branches and test-only service
implementations that exist throughout the plugin and platform, instead of the code path a real
user hits.

The tests in this module run the plugin in a genuinely separate, real (non-test) IDE process, so
`isUnitTestMode` is always `false` and the real, unmodified code path runs.

**Long-term goal:** eventually replace `CheckersTestBase`-based checker tests with this module,
since they suffer from exactly this problem.

## How it works

- The module's `test` Gradle task depends on `:intellij-plugin:buildPlugin` and passes the path
  to the resulting plugin zip to the test JVM via the `path.to.build.plugin` system property.
- Each test (see `CourseValidationTestBase.doTest()`) uses the
  [IntelliJ Starter](https://github.com/JetBrains/intellij-community/tree/master/tools/intellij.tools.ide.starter)
  framework to download a real IDE build, install that plugin zip into it, and configure it for a headless run.
- It then launches the IDE with a command line along the lines of:
  ```
  validateCourse <workspaceDir> --local <courseDir> --tests true --links false --output-format json --output <reportFile>
  ```
- Inside that real IDE process, the `validateCourse` command opens the given local course as a
  real educator-mode project, then walks every lesson and task: it invokes
  the real `CheckAction` for every course task, and records the outcome.
  The command dumps the whole result as a JSON tree into `reportFile`.
- Back on the Gradle/test side, `doTest()` reads both `reportFile` and the fixture's
  `expected.json`, flattens each into a list of `(path, case)` results, and reports one JUnit 5
  dynamic test per case - so a single course fixture expands into many individually-reported
  results, and a mismatch tells you exactly which task/case failed.

## Test data layout

Each test method looks for its fixture under:

```
testData/<technologyPrefix>/<test method display name>/course/...     - the course project to validate
testData/<technologyPrefix>/<test method display name>/expected.json  - the expected validation result tree
```

- `course/` is a full local educator-mode course project (course/lesson/task YAML, task
  sources and tests, etc) - this is what gets passed to `validateCourse --local`.
- `expected.json` is a serialized result tree in the same shape the command itself produces,
  e.g.:
  ```json
  {
    "name": "root_node",
    "children": [
      { "type": "suite", "name": "lesson1", "children": [
        { "type": "suite", "name": "task1", "children": [
          { "type": "case", "name": "Tests", "result": { "type": "success" } }
        ]}
      ]}
    ]
  }
  ```
  A case's `result.type` is one of `success`, `ignored`, or `failed`.

## Running the tests

```
./gradlew :intellij-plugin:integration-tests:test
```

The plugin build and IDE download/install happen automatically as part of the task. 

Some test classes require extra JVM system properties (enforced via `@RequiredProperty`) so the
launched IDE can resolve an SDK/interpreter:

- JVM tests need `-Dproject.jdk=<path to a JDK>` (or the `COM_JETBRAINS_EDU_PROJECT_JDK` environment variable).
- Python tests need `-Dproject.python.interpreter=<path to a Python interpreter>` (or `COM_JETBRAINS_EDU_PROJECT_PYTHON_INTERPRETER`).

These tests are intended to run on TeamCity with necessary environment.

[ide-tests](../../out/ide-tests) directory contains all artifacts of the corresponding tests

## Adding a new test case

1. Add a course fixture under `testData/<technologyPrefix>/<method name>/course`.
2. Add the matching `expected.json` next to it (hand-write it, or generate it by running
   `validateCourse` against the fixture and copying its output once you've confirmed it's
   correct).
3. Add a corresponding test method in the relevant test class (or create a new one) that calls `doTest(...)`.

