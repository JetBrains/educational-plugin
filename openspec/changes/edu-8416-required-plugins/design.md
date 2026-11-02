# Design

## Context

`CourseValidationTestBase.doTest()` creates an IntelliJ Starter context (`Starter.newContext(...)`), installs the
built plugin zip through the `protected open fun installPlugin(configurator: PluginConfigurator, pluginPath: Path)`
hook (`configurator.installPluginFromPath(pluginPath)`), patches VM options and runs the IDE. Nothing overrides the hook
on master.

Per-test configuration already exists in one place: `@RequiredProperty`
(`intellij-plugin/integration-tests/testSrc/com/jetbrains/edu/slow/checkerTests/RequiredProperty.kt`). It is a
`@Repeatable` runtime annotation on `FUNCTION`/`CLASS`. `requiredProperties()` collects it from `javaClass` and from
the test method into a `linkedSetOf`.

IntelliJ Starter (`ide-starter-squashed` 263.7307.75) provides
`PluginConfigurator.installPluginFromPluginManager(pluginId: String, ide: InstalledIde, channel: String? = null, pluginFileName: String? = null)`.
It downloads the latest plugin version compatible with `ide.build`, caches it under the starter cache dir per IDE
build, and throws `PluginNotFoundException` (the message contains the plugin id) when nothing can be downloaded.

## Goals / Non-Goals

**Goals:**
- One general, declarative way for any course validation test to get extra plugins into the tested IDE.
- Same look and feel as `@RequiredProperty`, so test authors see a single pattern.

**Non-Goals:**
- Version or channel pinning (`channel`, `pluginFileName`, `PluginWithExactVersion`). We can add them later as
  optional annotation parameters without changing callers.
- Installing plugins from local paths or custom repositories.
- Changing the lookup rules of `@RequiredProperty`.

## Decisions

### Annotation `RequiredPlugin(val pluginId: String)`

A new file `RequiredPlugin.kt` next to `RequiredProperty.kt`:
`@Repeatable @Retention(AnnotationRetention.RUNTIME) @Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)`,
with KDoc in the same style and `@see CourseValidationTestBase`.

Alternative: an overridable `requiredPlugins(): List<String>` in the base class. Rejected because it can't be declared
per test method, and it repeats the "subclass hooks into setup" pattern that this change replaces.

### Collect from the whole class hierarchy plus the test method

`requiredPlugins()` builds a `linkedSetOf<String>()`: first from `RequiredPlugin` annotations of the test class and
all its superclasses (walk `generateSequence(javaClass) { it.superclass }`), then from the test method. The ordered
set keeps declaration order and removes duplicates.

Why we walk the hierarchy explicitly: `Class.getAnnotationsByType` ignores annotations on superclasses unless the
annotation is `@java.lang.annotation.Inherited`. For a Kotlin `@Repeatable` annotation, `@Inherited` would also have
to apply to the compiler-generated container, so explicit traversal is simpler and obviously correct. A language
plugin is naturally declared once on a shared abstract base of that language's test classes.

Alternative: read only `javaClass` and the method, like `@RequiredProperty`. Rejected because every concrete class
would then have to repeat the language plugin annotation.

### Install from the Plugin Manager before the built plugin

In `doTest()`, inside the `Starter.newContext(...).apply { }` block:

```kotlin
val pluginConfigurator = PluginConfigurator(this)
for (pluginId in requiredPlugins()) {
  pluginConfigurator.installPluginFromPluginManager(pluginId, ide)
}
pluginConfigurator.installPluginFromPath(pluginPath)
```

Dependencies go in first, and the built plugin goes last. Order doesn't affect the IDE (all plugins are loaded on
startup), but this order makes the setup log read naturally. A missing plugin fails the test in setup with the
starter's `PluginNotFoundException`, before the IDE is launched, as the spec requires. No extra wrapping is needed.

### Remove the `installPlugin` hook

The `protected open fun installPlugin` hook is replaced by the inline code above. It has no overrides, and keeping it
would leave a second, imperative way to change the plugin set. Removing it makes the annotation the single extension
point.

## Risks / Trade-offs

- [Network dependency on the Marketplace for annotated tests] → starter caches downloads per IDE build, so only the
  first run per build downloads. Tests without the annotation are not affected.
- [The latest compatible version can change between runs, so results can drift] → acceptable for integration tests,
  which aim to reproduce what a learner gets. Version pinning can be added later (see Non-Goals).
- [Superclass traversal differs from `@RequiredProperty` lookup] → documented in the KDoc of both the annotation and
  `CourseValidationTestBase`. We can align `@RequiredProperty` in a separate change if needed.
