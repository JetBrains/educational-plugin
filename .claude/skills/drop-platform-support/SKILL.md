---
name: drop-platform-support
description: Drops support for an old IntelliJ platform version in the JetBrains Academy plugin — removes `branches/<old>` code, moves shared platform-specific code to the main source set, and fixes `BACKCOMPAT` comments and `isAtLeast<version>` checks. Use when asked to "drop platform 261", "drop 2026.1 support", or "remove support for an old IntelliJ platform version".
argument-hint: <platform version to drop, e.g. 261>
---

# Drop Platform Support Skill

This skill runs the whole process of removing support for an old IntelliJ platform version in the JetBrains Academy plugin.

This skill implements the "Dropping the old platform version" section of
[`documentation/different-platform-versions.md`](../../../documentation/different-platform-versions.md)
and adds more detail. If the two directly contradict each other, report the contradiction to the user.

## Overview

Dropping platform support has three phases:
1. Remove platform-specific code for the old platform (steps 1–3)
2. Move remaining platform-specific code (that's identical across all supported platforms) to the main source set (steps 1–3)
3. Fix `BACKCOMPAT` comments and version checks that reference the old platform version (steps 4–6)

`helpers/drop_platform_support_local.py` automates most of phases 1 and 2. It does **not** move a file if a file
with the same path already exists in the main source set. Those files stay in `branches/<remaining>/` and have to be
merged by hand. Phase 3 is done by the agent.

## Arguments

The platform version to drop is the one the user asked for, referred to below as `{platform_version}`
(e.g. `261`). Its dotted form is `{dotted_version}` (e.g. `2026.1`, see below). If the user did not give a version, ask. Do not guess.

## Version naming

Platform versions appear in two forms. BACKCOMPAT comments in this repository use the **dotted** form.
To convert: `NNN` → `20` + first two digits + `.` + last digit.

| Numeric (`environmentName`, `branches/<v>`, `isAtLeast<v>`) | Dotted (`BACKCOMPAT` comments) |
|---|---|
| `253` | `2025.3` |
| `261` | `2026.1` |
| `262` | `2026.2` |

The same conversion is `Project.platformVersion` in `buildSrc/src/main/kotlin/intellijUtils.kt`.

## Prerequisites

- Python 3 (`python3`), run from the repository root
- No nested worktrees or checkouts inside the repository (e.g. `.claude/worktrees/*`). The script walks the whole
  tree with `os.walk(".")` and would change their `branches/` directories too.
- No staged or unstaged changes to tracked files (untracked files are fine). The script's changes are committed
  with `git add -u`, so any unrelated change to a tracked file would end up in the commit.
- A YouTrack ticket ID for the commits (ask the user if it isn't known — do not commit `EDU-XXXX`)

## Instructions

### 1. Prepare the branch

- Check the current branch: `git rev-parse --abbrev-ref HEAD`
- If it is already `drop-{platform_version}`, continue on it. If that branch exists locally, switch to it with
  `git checkout drop-{platform_version}`. Otherwise create it: `git checkout -b drop-{platform_version}`.
- `drop-{platform_version}` is the same branch name the CI script (`helpers/drop_platform_support.py`) pushes.
  Check for a remote branch with `git ls-remote --heads origin drop-{platform_version}`. If one already exists,
  tell the user and ask whether to continue on it rather than creating a separate local one.
- Make sure there are no changes to tracked files before continuing.

### 2. Run the script

- From the repository root:
  `python3 helpers/drop_platform_support_local.py --platform_version={platform_version}`
- It deletes `branches/{platform_version}/`, moves files that are identical in every remaining `branches/<v>/`
  directory into the main source set, deletes `gradle-{platform_version}.properties`, and updates the
  `supported values` line in `gradle.properties`.
- **If the script fails partway**, the tree is left half-applied. Stop, show the user `git status`, and ask how to
  proceed. Don't try to "finish" the changes by hand.
- Review the result with `git status`, then commit it with the ticket ID:
  `git add -u && git commit -m "<ticket>: Drop {platform_version} support"`.

### 3. Check what the script did not do

- **`gradle.properties`**: The script only rewrites the `supported values` line when `{platform_version}` is the
  *first* entry, and it never touches `environmentName`. Check that:
  - `{platform_version}` no longer appears in `# supported values: ...`
  - `environmentName` is not `{platform_version}` (if it is, set it to a remaining version)
- **Leftover platform-specific files**: List everything still under `*/branches/*/`
  (e.g. `find . -path '*/branches/*' -type f -not -path '*/build/*' -not -path './.git/*' -not -path './.claude/*'`).
  For each file that exists in every remaining version but was not moved (because the main source set already has a
  file at that path), merge it by hand. The script removes only the immediate parent directory of deleted files, so
  review and then delete empty directories left behind:
  `find . -path '*/branches/*' -type d -empty -not -path './.git/*' -not -path './.claude/*' -delete`.
- **Always platform-specific modules**: the script deliberately leaves the remaining `branches/<v>/` of the modules in
  `ALWAYS_PLATFORM_SPECIFIC_MODULES` (`helpers/platform_support_common.py`, e.g. `intellij-plugin/integration-tests`)
  untouched. Don't move, merge, inline, or simplify those files, even if only one platform is left.
- **Only one platform left**: This is the usual case, since the plugin normally supports two versions. With one
  version left, the script moves every file from its `branches/<v>/` whose path doesn't already exist in main.
  Then also (except for the always platform-specific modules above):
  - inline `*platform*.xml` files pulled in with `xi:include` into the main XML, if any remain
    (e.g. `educational-core-platform.xml` included from `educational-core.xml`). Put the content exactly in place
    of the `xi:include` line and don't reorder the other entries
  - simplify compat shims that now have a single implementation (`compatibilityUtils.kt` functions, `*Base`
    classes, `typealias`es) — see "Tips and tricks" in the platform document
  - keep the `branches/$environmentName/...` source dirs in `buildSrc/src/main/kotlin/intellij-plugin-common-conventions.gradle.kts`:
    they are needed again when the next platform is added
- Commit any fixes from this step separately, or fold them into the phase 3 commit.

### 4. Determine the next oldest version

- Read the `supported values` line in `gradle.properties` after step 3.
- The **next oldest version** is the smallest remaining one.
  Example: when dropping `261` and `supported values: 262, 263` remain, the next oldest is `262`.

### 5. Find BACKCOMPAT comments and version checks

Prefer JetBrains MCP tools (`search_regex`, `search_text`, `search_symbol`). Fall back to `grep`.

- **Primary**: BACKCOMPAT comments with the dotted version. The colon is optional in this repo
  (both `BACKCOMPAT: 2026.1` and `BACKCOMPAT 2026.1` exist):
  `grep -rnE "BACKCOMPAT:? *{dotted_version}" --exclude-dir=build --exclude-dir=.git --exclude-dir=localization --exclude-dir=.claude .`
  (escape the dot, e.g. `2026\.1`).
  Don't restrict file types — BACKCOMPAT notes appear in `.kt`, `.java`, `.kts`, `.xml`, `.properties`, `.py`, `.md`, etc.
  `.claude/` is excluded on purpose. The examples in this skill use real versions, so don't edit them as part of the drop.
- **Secondary**: the numeric form (`BACKCOMPAT.*{platform_version}`) and bare `BACKCOMPAT` comments with no version
  that mention the dropped platform.
- **Older versions**: BACKCOMPAT comments for versions *older* than `{platform_version}` can be left over from past
  drops. Fix them if they are simple. Otherwise list them in the summary for the user. Don't skip them silently.
- **Version checks for the next oldest version only** (these are now always `true`):
  - build scripts: `isAtLeast{NextOldestVersion}` (defined in `buildSrc/src/main/kotlin/intellijUtils.kt`, used in
    `intellij-plugin/**/build.gradle.kts` and `buildSrc`). Use `search_symbol` to find all usages.
- **Runtime and test checks for any version ≤ the next oldest version** (always `true` or always `false`):
  - runtime: search `BuildNumber\.fromString\("[0-9]{3}` — this covers named constants (`BUILD_242`) and inline
    checks (`build < BuildNumber.fromString("252")!!`) compared with `ApplicationInfo.getInstance().build`.
    Checks for versions older than `{NextOldestVersion}` are leftovers from past drops. Treat them the same way as
    older BACKCOMPAT comments: fix them if simple, otherwise report them.
  - tests: `@MinPlatformVersion("...")` whose version is ≤ the next oldest version
    (`intellij-plugin/educational-core/testSrc/com/jetbrains/edu/rules/ConditionalExecutionRule.kt`)
- **Leave checks for newer versions alone.** Example: when dropping 261 with 262 and 263 remaining,
  remove `isAtLeast262`, but leave `isAtLeast263` alone — it still distinguishes 262 from 263.
- **Final sweep**: plain-grep for `\b{platform_version}\b` and `{dotted_version}` (excluding `branches/`, `build`,
  `.git`, `.claude`, `localization`, and test data), and review the hits by hand. This catches hard-coded values
  like `environment("IDEA_BUILD_NUMBER", "261")`. Don't auto-fix anything here. Many hits are legitimate, e.g. dependency
  versions such as `psiViewerPlugin=PsiViewer:2026.1` in `gradle-262.properties` or release history in
  `documentation/PluginVersionsInfo.md`.

### 6. Fix each occurrence

For each BACKCOMPAT comment or version check:

1. **Read the surrounding code** and, if the intent isn't clear, look at `git log -S` / `git blame` for why it was added.
2. **Apply the fix** that the comment asks for:
   - `Inline it` → inline the declaration into all call sites (prefer an IDE inline refactoring if one is available), then remove it.
     If the body is only a delegation to a newer API, call that API directly.
   - `Merge it with X` → merge the class/file into `X`, update imports and references, delete the original.
   - `Drop it` → delete the code and anything that becomes unused.
   - `Update value to X` / `Check ...` → update the value, then either remove the comment or point it at the
     next oldest platform if the check has to be repeated at the next drop.
   - Any other instruction → follow it. General comment with no instruction → work out what the workaround was for
     and replace it with the straightforward code for the remaining platforms.
   - Version check → keep only the "new" code path (the `if (isAtLeastN)` branch, or the `else` of `if (!isAtLeastN)` /
     `if (build < BUILD_N)`), then remove the check definition once it has no usages. If a value was nullable only
     because of the check, make it non-null and simplify its callers.
3. **Remove the BACKCOMPAT comment** once the fix is applied (unless it was updated to point at the next platform).

See [Common patterns](#common-patterns) below for concrete examples from this repository.

### 7. Verify

- Compile production and test code against **every remaining** platform version:
  `./gradlew testClasses -PenvironmentName=<v>` for each `<v>` in `supported values`.
- If Gradle build logic (`buildSrc`, `build.gradle.kts`) changed, this also checks it.
- Optionally run tests for modules you changed in a non-trivial way:
  `./gradlew <gradle-module-path>:test --continue -PexcludeTests=**/slow/**`
- Fix failures before committing. Report any you can't fix.

### 8. Commit

- Use the project format `<ticket>: <subject>` with the real ticket ID, e.g. `EDU-1234: Fix BACKCOMPAT comments for 2026.1`.
- The script's changes are already committed separately in step 2. Commit the phase 3 fixes on top of that commit.

### 9. Summary

Report:
- the number of BACKCOMPAT comments and version checks fixed, grouped by fix type
- files that the script left under `branches/` and how they were merged
- stale older-version BACKCOMPAT comments left for the user
- which platform versions were compiled, and the results
- **manual follow-ups outside this repository/change**:
  - update `EducationalSupportedReleases` in the [`intellij-teamcity-config`](https://jetbrains.team/p/ij/code/intellij-teamcity-config) repository

## Common patterns

The examples assume dropping **261** (`2026.1`), with **262** as the next oldest version.

### Pattern 1: Version check property in `buildSrc`
```kotlin
// buildSrc/src/main/kotlin/intellijUtils.kt
// BACKCOMPAT: 2026.1. Drop it
val Project.isAtLeast262: Boolean get() = environmentName.toInt() >= 262
```
**Fix**: Simplify every usage (patterns 2–4), then delete the property.

### Pattern 2: Conditional dependencies in Gradle
```kotlin
// intellij-plugin/Edu-Python/build.gradle.kts
intellijPlugins(pythonPlugin)
testIntellijPlugins(tomlPlugin)
if (isAtLeast262) {
  intellijPlugins(testRunnerPlugin)
}
```
**Fix**: Remove the condition and keep the body unconditionally:
```kotlin
intellijPlugins(pythonPlugin)
testIntellijPlugins(tomlPlugin)
intellijPlugins(testRunnerPlugin)
```

### Pattern 3: Conditional list entries
```kotlin
val Project.commonTestPlugins: List<String> get() = listOfNotNull(
  imagesPlugin,
  if (isAtLeast262) "intellij.structureView.plugin" else null,
  if (isAtLeast262) testRunnerPlugin else null,
  jcefPlugin,
)
```
**Fix**: Replace each ternary with its value. If nothing nullable is left, `listOfNotNull` can become `listOf`:
```kotlin
val Project.commonTestPlugins: List<String> get() = listOf(
  imagesPlugin,
  "intellij.structureView.plugin",
  testRunnerPlugin,
  jcefPlugin,
)
```

### Pattern 4: Nullable only because of the version check
```kotlin
// BACKCOMPAT: 2026.1. Always use `jcefPlugin` property and make it non-null
val Project.jcefPlugin: String? get() = if (isAtLeast262) resolvePluginPlaceholders(prop("jcefPlugin")) else null
```
**Fix**: Make it non-null and update callers that handled `null`:
```kotlin
val Project.jcefPlugin: String get() = resolvePluginPlaceholders(prop("jcefPlugin"))
```

### Pattern 5: Runtime build check
```kotlin
val BUILD_262 = BuildNumber.fromString("262")!!
if (ApplicationInfo.getInstance().build < BUILD_262) {
  oldImplementation()
} else {
  newImplementation()
}
```
**Fix**: Keep only `newImplementation()`. Delete `BUILD_262` if it's no longer used.
These checks live in normal source files, not in `branches/`, so they are easy to miss — search for them explicitly.

### Pattern 6: Update the comment to the next version
```kotlin
// buildSrc/src/main/kotlin/common-conventions.gradle.kts
// BACKCOMPAT: 2026.1. Check the minimal required API version.
// Update this message and api version value if needed
apiVersion = KotlinVersion.KOTLIN_2_3
```
**Fix**: Set `apiVersion` to the Kotlin version bundled with the oldest remaining platform
(see https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#bundled-stdlib-versions). It must never be higher
than that version, because raising it is the risky direction. Keep it if unsure and report it.
Then update the comment to the next oldest platform:
```kotlin
// BACKCOMPAT: 2026.2. Check the minimal required API version.
// Update this message and api version value if needed
apiVersion = KotlinVersion.KOTLIN_2_3
```

### Pattern 7: Inline / merge / drop
```kotlin
// BACKCOMPAT: 2026.1. Inline it
fun oldHelper() = newApi()
```
**Fix**: Replace `oldHelper()` calls with `newApi()` and remove `oldHelper`.
`Merge it with X` and `Drop it` work the same way: move or delete the code, then fix references and imports.

## Tips

- Search for both the dotted (`2026.1`) and numeric (`261`) forms, and don't require a colon after `BACKCOMPAT`.
- Use `get_symbol_info` to understand a symbol before removing it.
- Gradle changes break the whole build — compile after changing `buildSrc`.
