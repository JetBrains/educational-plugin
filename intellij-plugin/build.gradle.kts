import groovy.util.Node
import groovy.xml.XmlParser
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.RunIdeBase
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

val environmentName: String by project
val pluginVersion: String by project
val platformVersion: String = "20${StringBuilder(environmentName).insert(environmentName.length - 1, '.')}"
val baseIDE: String by project
val isJvmCenteredIDE = baseIDE in listOf("idea", "studio")

val ideaVersion: String by project
val clionVersion: String by project
val pycharmVersion: String by project
val studioVersion: String by project

val isIdeaIDE = baseIDE == "idea"
val isClionIDE = baseIDE == "clion"
val isPycharmIDE = baseIDE == "pycharm"
val isStudioIDE = baseIDE == "studio"

val baseVersion = when {
  isIdeaIDE -> ideaVersion
  isClionIDE -> clionVersion
  isPycharmIDE -> pycharmVersion
  isStudioIDE -> studioVersion
  else -> error("Unexpected IDE name = `$baseIDE`")
}

val pycharmSandbox = "${buildDir()}/pycharm-sandbox"
val clionSandbox = "${buildDir()}/clion-sandbox"
val remoteDevServerSandbox = "${buildDir()}/remote-dev-server-sandbox"

val pythonProPlugin: String by project
val pythonCommunityPlugin: String by project

val pythonPlugin = when {
  isIdeaIDE -> pythonProPlugin
  isClionIDE -> "python-ce"
  isPycharmIDE -> "python-ce"
  isStudioIDE -> pythonCommunityPlugin
  else -> error("Unexpected IDE name = `$baseIDE`")
}
val javaPlugin = "com.intellij.java"
val kotlinPlugin = "org.jetbrains.kotlin"
val scalaPlugin: String by project
val rustPlugin: String by project
val tomlPlugin = "org.toml.lang"
val goPlugin: String by project
val sqlPlugin = "com.intellij.database"
val shellScriptPlugin = "com.jetbrains.sh"
val markdownPlugin = "org.intellij.plugins.markdown"
val githubPlugin = "org.jetbrains.plugins.github"
val psiViewerPlugin: String by project
val phpPlugin: String by project
val intelliLangPlugin = "org.intellij.intelliLang"
val javaScriptPlugin = "JavaScript"
val nodeJsPlugin = "NodeJS"
val yamlPlugin = "org.jetbrains.plugins.yaml"
val androidPlugin = "org.jetbrains.android"
val platformImagesPlugin = "com.intellij.platform.images"
val gridImplPlugin = "intellij.grid.impl"
val codeWithMePlugin = "com.jetbrains.codeWithMe"

val jvmPlugins = listOf(
  javaPlugin,
  "JUnit",
  "org.jetbrains.plugins.gradle"
)

val kotlinPlugins = jvmPlugins + kotlinPlugin

val javaScriptPlugins = listOf(
  javaScriptPlugin,
  nodeJsPlugin
)

val rustPlugins = listOf(
  rustPlugin,
  tomlPlugin
)

val cppPlugins = listOf(
  "com.intellij.cidr.lang",
  "com.intellij.clion",
  "com.intellij.cidr.base",
  "com.intellij.nativeDebug",
  "org.jetbrains.plugins.clion.test.google",
  "org.jetbrains.plugins.clion.test.catch"
)

val pythonPlugins = listOfNotNull(
  pythonPlugin,
  // `intellij.grid.impl` is dependency only of pythonPro plugin
  if (pythonPlugin == pythonProPlugin) gridImplPlugin else null
)

val changesFile = "changes.html"

plugins {
  alias(libs.plugins.gradleIntelliJPlugin)
}

allprojects {
  apply {
    plugin("org.jetbrains.intellij")
    plugin("org.jetbrains.kotlin.plugin.serialization")
  }
  intellij {
    version = baseVersion
    instrumentCode = false
  }

  sourceSets {
    main {
      java.srcDirs("src", "branches/$environmentName/src")
      resources.srcDirs("resources", "branches/$environmentName/resources")
    }

    test {
      java.srcDirs("testSrc", "branches/$environmentName/testSrc")
      resources.srcDirs("testResources", "branches/$environmentName/testResources")
    }
  }

  kotlin {
    sourceSets {
      main {
        kotlin.srcDirs("src", "branches/$environmentName/src")
      }
      test {
        kotlin.srcDirs("testSrc", "branches/$environmentName/testSrc")
      }
    }
  }

  tasks {
    withProp("customJbr") {
      if (it.isNotBlank()) {
        runIde {
          jbrVersion = it
        }
      }
    }

    withType<Test> {
      withProp("../secret.properties", "stepikTestClientSecret") { environment("STEPIK_TEST_CLIENT_SECRET", it) }
      withProp("../secret.properties", "stepikTestClientId") { environment("STEPIK_TEST_CLIENT_ID", it) }
      systemProperty("java.awt.headless", "true")
    }

    withType<JavaCompile> {
      // Prevents unexpected incremental compilation errors after changing value of `environmentName` property
      inputs.property("environmentName", providers.gradleProperty("environmentName"))
    }
    withType<KotlinCompile> {
      // Prevents unexpected incremental compilation errors after changing value of `environmentName` property
      inputs.property("environmentName", providers.gradleProperty("environmentName"))
    }

    val verifyClasses = task("verifyClasses") {
      dependsOn(jar)
      doLast {
        verifyClasses(project)
      }
    }
    // Fail plugin build if there are errors in module packages
    project(":intellij-plugin").tasks.buildPlugin {
      dependsOn(verifyClasses)
    }
  }

  dependencies {
    implementationWithoutKotlin(rootProject.libs.twitter4j.core)
    implementationWithoutKotlin(rootProject.libs.twitter4j.v2)
    implementationWithoutKotlin(rootProject.libs.jsoup)
    implementationWithoutKotlin(rootProject.libs.jackson.dataformat.yaml)
    implementationWithoutKotlin(rootProject.libs.jackson.datatype.jsr310)
    implementationWithoutKotlin(rootProject.libs.jackson.module.kotlin)
    implementationWithoutKotlin(rootProject.libs.okhttp)
    implementationWithoutKotlin(rootProject.libs.logging.interceptor)
    implementationWithoutKotlin(rootProject.libs.retrofit)
    implementationWithoutKotlin(rootProject.libs.converter.jackson)
    implementationWithoutKotlin(rootProject.libs.kotlin.css.jvm)

    testImplementationWithoutKotlin(rootProject.libs.kotlin.test.junit)
    testImplementationWithoutKotlin(rootProject.libs.mockwebserver)
    testImplementationWithoutKotlin(rootProject.libs.mockk)
  }
}

subprojects {
  tasks {
    runIde { enabled = false }
    prepareSandbox { enabled = false }
    buildSearchableOptions { enabled = false }
  }

  val testOutput = configurations.create("testOutput")

  dependencies {
    testOutput(sourceSets.test.get().output.classesDirs)
  }
}

val buildNumber = System.getenv("BUILD_NUMBER") ?: "SNAPSHOT"

if (hasProp("setTCBuildNumber")) {
  // Specify build number at building plugin running configuration on TC
  // with heading plugin version: e.g. `3.8.BUILD_NUMBER` instead of `BUILD_NUMBER`
  println("##teamcity[buildNumber '$pluginVersion.$buildNumber']")
}

version = "$pluginVersion-$platformVersion-$buildNumber"

intellij {
  pluginName = "JetBrainsAcademy"
  updateSinceUntilBuild = true
  downloadSources = false

  tasks.withType<PatchPluginXmlTask> {
    changeNotes = provider { file(changesFile).readText() }
    pluginDescription = provider { file("description.html").readText() }
    sinceBuild = prop("customSinceBuild")
    untilBuild = prop("customUntilBuild")
  }

  val pluginsList = mutableListOf(
    yamlPlugin,
    markdownPlugin,
    // PsiViewer plugin is not a runtime dependency
    // but it helps a lot while developing features related to PSI
    psiViewerPlugin
  )
  if (isIdeaIDE || isClionIDE) {
    pluginsList += rustPlugins
  }
  pluginsList += pythonPlugins
  pluginsList += shellScriptPlugin
  if (isJvmCenteredIDE) {
    pluginsList += jvmPlugins
    pluginsList += listOf(kotlinPlugin, scalaPlugin)
  }
  if (isIdeaIDE) {
    pluginsList += javaScriptPlugins
    pluginsList += listOf(goPlugin, phpPlugin)
  }
  if (!(isStudioIDE || isPycharmIDE)) {
    pluginsList += sqlPlugin
  }

  plugins = pluginsList
}

dependencies {
  implementation(project("educational-core"))
  implementation(project("code-insight"))
  implementation(project("code-insight:html"))
  implementation(project("code-insight:markdown"))
  implementation(project("code-insight:yaml"))
  implementation(project("jvm-core"))
  implementation(project("Edu-Java"))
  implementation(project("Edu-Kotlin"))
  implementation(project("Edu-Python"))
  implementation(project("Edu-Python:Idea"))
  implementation(project("Edu-Python:PyCharm"))
  implementation(project("Edu-Scala"))
  implementation(project("Edu-Android"))
  implementation(project("Edu-JavaScript"))
  implementation(project("Edu-Rust"))
  implementation(project("Edu-Cpp"))
  implementation(project("Edu-Go"))
  implementation(project("Edu-Php"))
  implementation(project("Edu-Shell"))
  implementation(project("sql"))
  implementation(project("sql:sql-jvm"))
  implementation(project("github"))
  implementation(project("remote-env"))
}

val removeIncompatiblePlugins = task<Delete>("removeIncompatiblePlugins") {

  fun deletePlugin(sandboxPath: String, pluginName: String) {
    file("$sandboxPath/plugins/$pluginName").deleteRecursively()
  }

  doLast {
    deletePlugin(pycharmSandbox, "python-ce")
    deletePlugin(clionSandbox, "python-ce")
    deletePlugin(pycharmSandbox, "Scala")
  }
}

// Collects all jars produced by compilation of project modules and merges them into singe one.
// We need to put all plugin manifest files into single jar to make new plugin model work
val mergePluginJarTask = task<Jar>("mergePluginJars") {
  duplicatesStrategy = DuplicatesStrategy.FAIL

  // The name differs from all module names to avoid collision during new jar file creation
  archiveBaseName = "JetBrainsAcademy"

  exclude("META-INF/MANIFEST.MF")

  val pluginLibDir by lazy {
    val sandboxTask = tasks.prepareSandbox.get()
    sandboxTask.destinationDir.resolve("${sandboxTask.pluginName.get()}/lib")
  }
  val pluginJars by lazy {
    pluginLibDir.listFiles().orEmpty().filter { it.isPluginJar() }
  }

  destinationDirectory = project.layout.dir(provider { pluginLibDir })

  doFirst {
    for (file in pluginJars) {
      from(zipTree(file))
    }
  }

  doLast {
    delete(pluginJars)
  }
}

tasks {
  withType<PrepareSandboxTask> {
    from("twitter") {
      into("${pluginName.get()}/twitter")
      include("**/*.gif")
    }
    finalizedBy(removeIncompatiblePlugins)
    doLast {
      val kotlinJarRe = """kotlin-(stdlib|reflect|runtime).*\.jar""".toRegex()
      val libraryDir = destinationDir.resolve("${pluginName.get()}/lib")
      val kotlinStdlibJars = libraryDir.listFiles().orEmpty().filter { kotlinJarRe.matches(it.name) }
      check(kotlinStdlibJars.isEmpty()) {
        "Plugin shouldn't contain kotlin stdlib jars. Found:\n" + kotlinStdlibJars.joinToString(separator = ",\n") { it.absolutePath }
      }
    }
  }
  prepareSandbox {
    finalizedBy(mergePluginJarTask)
  }
  withType<RunIdeBase> {
    // Force `mergePluginJarTask` be executed before any task based on `RunIdeBase` (for example, `runIde` or `buildSearchableOptions`).
    // Otherwise, these tasks fail because of implicit dependency.
    // Should be dropped when jar merging is implemented in `gradle-intellij-plugin` itself
    mustRunAfter(mergePluginJarTask)
    // Disable auto plugin reloading. See `com.intellij.ide.plugins.DynamicPluginVfsListener`
    // To enable dynamic reloading, change value to `true` and disable `EduDynamicPluginListener`
    autoReloadPlugins = false
    jvmArgs("-Xmx2g")
    jvmArgs("-Dide.experimental.ui=true")

    // Uncomment to show localized messages
    // jvmArgs("-Didea.l10n=true")

    // Uncomment to enable memory dump creation if plugin cannot be unloaded by the platform
    // jvmArgs("-Dide.plugins.snapshot.on.unload.fail=true")

    // Uncomment to enable FUS testing mode
    // jvmArgs("-Dfus.internal.test.mode=true")
  }
  verifyPlugin {
    mustRunAfter(mergePluginJarTask)
  }
  buildSearchableOptions {
    enabled = findProperty("enableBuildSearchableOptions") != "false"
  }
  buildPlugin {
    dependsOn(":edu-format:jar")
    dependsOn(":edu-format:sourcesJar")
    doLast {
      copyFormatJars()
    }
  }
}

// Generates event scheme for JetBrains Academy plugin FUS events to `build/eventScheme.json`
task<RunIdeTask>("buildEventsScheme") {
  dependsOn(tasks.prepareSandbox)
  args("buildEventsScheme", "--outputFile=${buildDir()}/eventScheme.json", "--pluginId=com.jetbrains.edu")
  // Force headless mode to be able to run command on CI
  systemProperty("java.awt.headless", "true")
  // BACKCOMPAT: 2023.2. Update value to 232 and this comment
  // `IDEA_BUILD_NUMBER` variable is used by `buildEventsScheme` task to write `buildNumber` to output json.
  // It will be used by TeamCity automation to set minimal IDE version for new events
  environment("IDEA_BUILD_NUMBER", "232")
}

task("configureRemoteDevServer") {
  doLast {
    intellij.sandboxDir = remoteDevServerSandbox
  }
}

task<RunIdeTask>("runRemoteDevServer") {
  dependsOn(tasks.prepareSandbox)
  val remoteProjectPath = System.getenv("REMOTE_DEV_PROJECT") ?: rootProject.layout.projectDirectory.dir("example-course-project").asFile.absolutePath
  args("cwmHostNoLobby", remoteProjectPath)
  systemProperty("ide.browser.jcef.enabled", "false")
}

createTasksToRunIde("Idea", requiresLocalPath = false)
createTasksToRunIde("CLion", requiresLocalPath = false)
createTasksToRunIde("PyCharm", requiresLocalPath = false)
createTasksToRunIde("AndroidStudio", requiresLocalPath = false)
createTasksToRunIde("WebStorm")
createTasksToRunIde("GoLand")
createTasksToRunIde("PhpStorm")
createTasksToRunIde("RustRover")
createTasksToRunIde("DataSpell")

/**
 * Creates `configure$[ideName]` and `run$[ideName]` Gradle tasks based on given [ideName].
 *
 * - `configure$[ideName]` checks that all necessary properties are provided and specifies sandbox path
 * - `run$[ideName]` runs IDE itself via `runIde` task
 */
fun createTasksToRunIde(ideName: String, requiresLocalPath: Boolean = true) {
  // "GoLand" -> "goLandPath"
  val pathProperty = ideName.replaceFirstChar { it.lowercaseChar() } + "Path"
  // "GoLand" -> "$buildDir/goland-sandbox"
  val sandboxPath = "${buildDir()}/${ideName.lowercase()}-sandbox"

  task("configure$ideName") {
    doLast {
      if (requiresLocalPath && !hasProp(pathProperty)) {
        throw InvalidUserDataException("Path to $ideName installed locally is needed\nDefine \"$pathProperty\" property")
      }
      intellij.sandboxDir = sandboxPath
    }
  }

  task<RunIdeTask>("run$ideName") {
    dependsOn(tasks.prepareSandbox)

    if (hasProp(pathProperty)) {
      ideDir = provider {
        file(prop(pathProperty))
      }
    }
  }
}

project("educational-core") {
  dependencies {
    api(project(":edu-format"))
    // For some reason, kotlin serialization plugin doesn't see the corresponding library from IDE dependency
    // and fails Kotlin compilation.
    // Let's provide necessary dependency during compilation to make it work
    compileOnly(rootProject.libs.kotlinx.serialization) {
      excludeKotlinDeps()
    }
  }
}

project("code-insight") {
  dependencies {
    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("code-insight:html") {
  dependencies {
    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:code-insight"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:code-insight", "testOutput"))
  }
}

project("code-insight:markdown") {
  val pluginList = mutableListOf(markdownPlugin)
  if (isStudioIDE) {
    pluginList += "platform-images"
  }
  intellij {
    plugins = pluginList
  }

  tasks {
    prepareTestingSandbox {
      // Set custom plugin directory name for tests.
      // Otherwise, `prepareTestingSandbox` merge directories of `markdown` plugin and `markdown` modules
      // into single one
      pluginName = "edu-markdown"
    }
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:code-insight"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:code-insight", "testOutput"))
  }
}

project("code-insight:yaml") {
  intellij {
    plugins = listOf(yamlPlugin)
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:code-insight"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:code-insight", "testOutput"))
  }
}

project("jvm-core") {
  intellij {
    if (!isJvmCenteredIDE) {
      version = ideaVersion
    }
    plugins = jvmPlugins
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("remote-env") {
  intellij {
    if (isStudioIDE) {
      version = ideaVersion
    }
    plugins = listOf(codeWithMePlugin)
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Java") {
  intellij {
    version = ideaVersion
    plugins = jvmPlugins
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:jvm-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
  }
}

project("Edu-Kotlin") {
  intellij {
    if (!isJvmCenteredIDE) {
      version = ideaVersion
    }
    plugins = kotlinPlugins
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:jvm-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
  }
}

project("Edu-Scala") {
  intellij {
    version = ideaVersion
    val pluginsList = jvmPlugins + scalaPlugin
    plugins = pluginsList
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:jvm-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
  }
}

project("Edu-Android") {
  intellij {
    version = studioVersion
    val pluginsList = jvmPlugins + androidPlugin
    plugins = pluginsList
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:jvm-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
  }

  // BACKCOMPAT: enable when 241 studio is available
  tasks.withType<Test> {
    enabled = environmentName.toInt() < 241
  }
}

project("Edu-Python") {
  intellij {
    val pluginList = pythonPlugins + listOfNotNull(
      if (isJvmCenteredIDE) javaPlugin else null,
      // needed only for tests, actually
      platformImagesPlugin,
      // needed to load `intellij.python.community.impl` module of Python plugin in tests
      tomlPlugin
    )
    plugins = pluginList
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:Edu-Python:Idea"))
    testImplementation(project(":intellij-plugin:Edu-Python:PyCharm"))
  }
}

project("Edu-Python:Idea") {
  intellij {
    if (!isJvmCenteredIDE || isStudioIDE) {
      version = ideaVersion
    }

    val pluginList = listOfNotNull(
      if (!isJvmCenteredIDE) pythonProPlugin else pythonPlugin,
      gridImplPlugin,
      javaPlugin
    )
    plugins = pluginList
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))
    compileOnly(project(":intellij-plugin:Edu-Python"))
    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Python:PyCharm") {
  intellij {
    if (isStudioIDE) {
      version = ideaVersion
    }
    plugins = pythonPlugins
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))
    compileOnly(project(":intellij-plugin:Edu-Python"))
    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-JavaScript") {
  intellij {
    version = ideaVersion
    plugins = javaScriptPlugins
  }
  dependencies {
    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Rust") {
  intellij {
    if (!isIdeaIDE && !isClionIDE) {
      version = ideaVersion
    }
    plugins = rustPlugins
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Cpp") {
  intellij {
    version = clionVersion
    plugins = cppPlugins
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Go") {
  intellij {
    version = ideaVersion
    plugins = listOf(goPlugin, intelliLangPlugin)
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Php") {
  intellij {
    version = ideaVersion
    plugins = listOf(phpPlugin)
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Shell") {
  intellij {
    plugins = listOf(shellScriptPlugin)
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("sql") {
  intellij {
    if (isStudioIDE || isPycharmIDE) {
      version = ideaVersion
    }
    plugins = listOf(sqlPlugin)
  }

  dependencies {
    api(project(":intellij-plugin:educational-core"))
    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("sql:sql-jvm") {
  intellij {
    version = ideaVersion
    plugins = listOf(sqlPlugin) + jvmPlugins
  }

  dependencies {
    api(project(":intellij-plugin:sql"))
    api(project(":intellij-plugin:jvm-core"))
    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:sql", "testOutput"))
    testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
  }
}

project("github") {
  intellij {
    plugins = listOf(githubPlugin)
  }

  dependencies {
    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

fun hasProp(name: String): Boolean = extra.has(name)

fun prop(name: String): String =
  extra.properties[name] as? String ?: error("Property `$name` is not defined in gradle.properties")

fun withProp(name: String, action: (String) -> Unit) {
  if (hasProp(name)) {
    action(prop(name))
  }
}

fun withProp(filePath: String, name: String, action: (String) -> Unit) {
  if (!file(filePath).exists()) {
    println("$filePath doesn't exist")
    return
  }
  val properties = loadProperties(filePath)
  val value = properties.getProperty(name) ?: return
  action(value)
}

fun buildDir(): String {
  return project.layout.buildDirectory.get().asFile.absolutePath
}

fun <T : ModuleDependency> T.excludeKotlinDeps() {
  exclude(module = "kotlin-runtime")
  exclude(module = "kotlin-reflect")
  exclude(module = "kotlin-stdlib")
  exclude(module = "kotlin-stdlib-common")
  exclude(module = "kotlin-stdlib-jdk8")
}

fun loadProperties(path: String): Properties {
  val properties = Properties()
  file(path).bufferedReader().use { properties.load(it) }
  return properties
}

fun File.isPluginJar(): Boolean {
  if (!isFile) return false
  if (extension != "jar") return false
  return zipTree(this).files.any {
    if (it.extension != "xml") return@any false
    val node = XmlParser().parse(it)
    return node.name() == "idea-plugin"
  }
}

fun parseManifest(file: File): Node {
  val node = XmlParser().parse(file)
  check(node.name() == "idea-plugin") {
    "Manifest file `$file` doesn't contain top-level `idea-plugin` attribute"
  }
  return node
}

fun manifestFile(project: Project): File {
  var filePath: String? = null
  // Some gradle projects are not modules from IDEA plugin point of view
  // because we use `include` for them inside manifests, i.e. they just a part of another module.
  // That's why we delegate manifest search to other projects in some cases
  when (project.path) {
    ":intellij-plugin" -> {
      filePath = "META-INF/plugin.xml"
    }
    ":intellij-plugin:educational-core", ":intellij-plugin:code-insight",
    ":intellij-plugin:Edu-Python:Idea", ":intellij-plugin:Edu-Python:PyCharm" -> return manifestFile(project.parent!!)
    // Special rules for `Edu-Python` module because it's added via `include`
    // BACKCOMPAT: 2023.3. Drop this branch
    ":intellij-plugin:Edu-Python" -> {
      filePath = if (environmentName.toInt() < 241) "Edu-Python-Community.xml" else "Edu-Python.xml"
    }
  }

  val mainOutput = project.sourceSets.main.get().output
  val resourcesDir = mainOutput.resourcesDir ?: error("Failed to find resources dir for ${project.name}")

  if (filePath != null) {
    return resourcesDir.resolve(filePath).takeIf { it.exists() } ?: error("Failed to find manifest file for ${project.name} module")
  }
  val rootManifest = parseManifest(manifestFile(project(":intellij-plugin")))
  val children = ((rootManifest["content"] as? List<*>)?.single() as? Node)?.children()
                 ?: error("Failed to find module declarations in root manifest")
  return children.filterIsInstance<Node>()
           .flatMap { node ->
             if (node.name() != "module") return@flatMap emptyList()
             val name = node.attribute("name") as? String ?: return@flatMap emptyList()
             listOfNotNull(resourcesDir.resolve("$name.xml").takeIf { it.exists() })
           }.firstOrNull() ?: error("Failed to find manifest file for ${project.name} module")
}

fun findModulePackage(project: Project): String {
  val moduleManifest = manifestFile(project)
  val node = parseManifest(moduleManifest)
  return node.attribute("package") as? String ?: error("Failed to find package for ${project.name}")
}

fun verifyClasses(project: Project) {
  val pkg = findModulePackage(project)
  val expectedDir = pkg.replace('.', '/')

  var hasErrors = false
  for (classesDir in project.sourceSets.main.get().output.classesDirs) {
    val basePath = classesDir.toPath()
    for (file in classesDir.walk()) {
      if (file.isFile && file.extension == "class") {
        val relativePath = basePath.relativize(file.toPath())
        if (!relativePath.startsWith(expectedDir)) {
          logger.error("Wrong package of `${relativePath.joinToString(".").removeSuffix(".class")}` class. Expected `$pkg`")
          hasErrors = true
        }
      }
    }
  }

  if (hasErrors) {
    throw GradleException("Classes with wrong package were found. See https://docs.google.com/document/d/1pOy-qNlGOJe6wftHVYHkH8sZOoAfav1fdGDPJgkQWJo")
  }
}

fun DependencyHandler.implementationWithoutKotlin(dependencyNotation: Provider<*>) {
  implementation(dependencyNotation) {
    excludeKotlinDeps()
  }
}

fun DependencyHandler.testImplementationWithoutKotlin(dependencyNotation: Provider<*>) {
  testImplementation(dependencyNotation) {
    excludeKotlinDeps()
  }
}

fun copyFormatJars() {
  copy {
    from("../edu-format/build/libs/")
    into("build/distributions")
    include("*.jar")
  }
}
