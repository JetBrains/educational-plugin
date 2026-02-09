import org.gradle.api.Project
import org.gradle.process.JavaForkOptions
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformDependenciesExtension
import kotlin.reflect.KProperty

val Project.environmentName: String by Properties

// BACKCOMPAT: 2025.3. Drop it
val Project.isAtLeast261: Boolean get() = environmentName.toInt() >= 261

val Project.pluginVersion: String by Properties
val Project.platformVersion: String get() = "20${StringBuilder(environmentName).insert(environmentName.length - 1, '.')}"
val Project.baseIDE: String by Properties

val Project.ideaVersion: String by Properties
val Project.clionVersion: String by Properties
val Project.pycharmVersion: String by Properties
val Project.riderVersion: String by Properties

val Project.isIdeaIDE: Boolean get() = baseIDE == "idea"
val Project.isClionIDE: Boolean get() = baseIDE == "clion"
val Project.isPycharmIDE: Boolean get() = baseIDE == "pycharm"
val Project.isRiderIDE: Boolean get() = baseIDE == "rider"

val Project.baseVersion: String get() = when {
  isIdeaIDE -> ideaVersion
  isClionIDE -> clionVersion
  isPycharmIDE -> pycharmVersion
  isRiderIDE -> riderVersion
  else -> error("Unexpected IDE name = `$baseIDE`")
}

val Project.pythonProPlugin: String by Properties
val Project.pythonCommunityPlugin: String by Properties

val Project.pythonPlugin: String get() = when {
  // Since 2024.2 Python Community plugin is available in paid products (like IU) together with Python Pro as its base dependency.
  // But all necessary code that we need is inside Python Community plugin, so we need only it from compilation POV
  isIdeaIDE -> pythonCommunityPlugin
  isClionIDE -> "PythonCore"
  isPycharmIDE -> "PythonCore"
  isRiderIDE -> pythonCommunityPlugin
  else -> error("Unexpected IDE name = `$baseIDE`")
}
val Project.javaPlugin: String get() = "com.intellij.java"
val Project.kotlinPlugin: String get() = "org.jetbrains.kotlin"
val Project.scalaPlugin: String by Properties
val Project.rustPlugin: String by Properties
val Project.tomlPlugin: String get() = "org.toml.lang"
val Project.goPlugin: String by Properties
val Project.sqlPlugin: String get() = "com.intellij.database"
val Project.shellScriptPlugin: String get() = "com.jetbrains.sh"
val Project.markdownPlugin: String get() = "org.intellij.plugins.markdown"
val Project.githubPlugin: String get() = "org.jetbrains.plugins.github"
val Project.psiViewerPlugin: String by Properties
val Project.phpPlugin: String by Properties
val Project.intelliLangPlugin: String get() = "org.intellij.intelliLang"
val Project.javaScriptPlugin: String get() = "JavaScript"
val Project.nodeJsPlugin: String get() = "NodeJS"
val Project.cssPlugin: String get() = "com.intellij.css"
val Project.jsonPlugin: String get() = "com.intellij.modules.json"
val Project.yamlPlugin: String get() = "org.jetbrains.plugins.yaml"
val Project.radlerPlugin: String get() = "org.jetbrains.plugins.clion.radler"
val Project.imagesPlugin: String get() = "com.intellij.platform.images"

val Project.intellijTestFrameworkType: TestFrameworkType
  get() {
    return if (isAtLeast261) {
      TestFrameworkType.Platform
    }
    else {
      TestFrameworkType.Bundled
    }
  }

val Project.jvmPlugins: List<String> get() = listOf(
  javaPlugin,
  "JUnit",
  "org.jetbrains.plugins.gradle"
)

val Project.javaScriptPlugins: List<String> get() = listOf(
  javaScriptPlugin,
  nodeJsPlugin,
)

val Project.rustPlugins: List<String> get() = listOf(
  rustPlugin,
  tomlPlugin
)

val Project.cppPlugins: List<String> get() = listOfNotNull(
  "com.intellij.cidr.lang",
  "com.intellij.clion",
  "com.intellij.nativeDebug",
  "org.jetbrains.plugins.clion.test.google",
  "org.jetbrains.plugins.clion.test.catch",
  "com.intellij.cmake",
)

val Project.sqlPlugins: List<String> get() = listOfNotNull(
  sqlPlugin,
  "intellij.grid.plugin"
)

val Project.csharpPlugins: List<String> get() = listOf(
  "com.intellij.resharper.unity"
)

// Plugins which we add to tests for all modules.
// It's the most common plugins which affect the behavior of the plugin code
val Project.commonTestPlugins: List<String> get() = listOf(
  imagesPlugin, // adds `svg` file type and makes IDE consider .svg files as text ones
  yamlPlugin,   // makes IDE consider .yaml files as text ones and affects formatting of yaml files
  jsonPlugin,   // dependency of a lot of other bundled plugin
)


data class TypeWithVersion(val type: IntelliJPlatformType, val version: String)

fun String.toTypeWithVersion(): TypeWithVersion {
  val (code, version) = split("-", limit = 2)
  return TypeWithVersion(IntelliJPlatformType.fromCode(code, version), version)
}

fun IntelliJPlatformDependenciesExtension.intellijIde(versionWithCode: String) {
  val (type, version) = versionWithCode.toTypeWithVersion()
  create(type, version) {
    useInstaller.set(false)
    useCache.set(true)
  }
  // JetBrains runtime is necessary not only for running IDE but for tests as well
  jetbrainsRuntime()
}

fun IntelliJPlatformDependenciesExtension.intellijPlugins(vararg notations: String) {
  for (notation in notations) {
    if (notation.contains(":")) {
      plugin(notation)
    }
    else {
      bundledPlugin(notation)
    }
  }
}

fun IntelliJPlatformDependenciesExtension.intellijPlugins(notations: List<String>) {
  intellijPlugins(*notations.toTypedArray())
}

fun IntelliJPlatformDependenciesExtension.testIntellijPlugins(vararg notations: String) {
  for (notation in notations) {
    if (notation.contains(":")) {
      testPlugin(notation)
    }
    else {
      testBundledPlugin(notation)
    }
  }
}

fun IntelliJPlatformDependenciesExtension.testIntellijPlugins(notations: List<String>) {
  testIntellijPlugins(*notations.toTypedArray())
}

// Since 2024.1 CLion has two sets of incompatible plugins: based on classic language engine and new one (AKA Radler).
// Platform uses `idea.suppressed.plugins.set.selector` system property to choose which plugins should be disabled.
// But there aren't `idea.suppressed.plugins.set.selector`, `idea.suppressed.plugins.set.classic`
// and `idea.suppressed.plugins.set.radler` properties in tests,
// as a result, the platform tries to load all plugins and fails because of duplicate definitions.
// Here is a workaround to make test work with CLion by defining proper values for necessary properties
fun JavaForkOptions.setClionSystemProperties(project: Project, withRadler: Boolean = false) {
  val (mode, suppressedPlugins) = if (withRadler) {
    "radler" to project.clionRadlerSuppressedPlugins
  }
  else {
    "classic" to project.clionClassicSuppressedPlugins
  }
  systemProperty("idea.suppressed.plugins.set.selector", mode) // possible values: `classic` and `radler`
  systemProperty("idea.suppressed.plugins.set.$mode", suppressedPlugins.joinToString(","))
}

private val Project.clionRadlerSuppressedPlugins: List<String>
  get() = listOf("com.intellij.cidr.lang")

private val Project.clionClassicSuppressedPlugins: List<String>
  get() = listOf("org.jetbrains.plugins.clion.radler")

// There isn't an implicit `project` object here, so
// this is a minor workaround to use delegation for properties almost like in a regular plugin
// and not to duplicate property name twice: one time in Kotlin property and the second time in `prop` call
private object Properties {
  operator fun getValue(thisRef: Project, property: KProperty<*>): String = thisRef.prop(property.name)
}
