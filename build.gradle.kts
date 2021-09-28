import de.undercouch.gradle.tasks.download.DownloadAction
import de.undercouch.gradle.tasks.download.DownloadSpec
import groovy.util.Node
import groovy.xml.XmlParser
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.internal.HasConvention
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.intellij.tasks.IntelliJInstrumentCodeTask
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

apply(from = "common.gradle.kts")
apply(from = "changes.gradle.kts")

val environmentName: String by project
val pluginVersion: String by project
val platformVersion: String = "20${StringBuilder(environmentName).insert(environmentName.length - 1, '.')}"
val baseIDE: String by project
val isJvmCenteredIDE = baseIDE in listOf("idea", "studio")

val ideaVersion: String by project
val clionVersion: String by project
val pycharmVersion: String by project
val studioVersion: String by project

val studioBuildVersion: String by project

val secretProperties: String by extra
val inJetBrainsNetwork: () -> Boolean by extra
val generateNotes: (String) -> String by extra

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

val studioPath: String
  get() {
    val androidStudioPath: String? by project
    return androidStudioPath ?: downloadStudioIfNeededAndGetPath()
  }

val jacksonVersion = "2.10.0"
val okhttpVersion = "3.14.0"

val ideaSandbox = "${project.buildDir.absolutePath}/idea-sandbox"
val pycharmSandbox = "${project.buildDir.absolutePath}/pycharm-sandbox"
val studioSandbox = "${project.buildDir.absolutePath}/studio-sandbox"
val webStormSandbox = "${project.buildDir.absolutePath}/webstorm-sandbox"
val clionSandbox = "${project.buildDir.absolutePath}/clion-sandbox"
val goLandSandbox = "${project.buildDir.absolutePath}/goland-sandbox"

// BACKCOMPAT: 2020.3
val isAtLeast211 = environmentName.toInt() >= 211
// BACKCOMPAT: 2021.1
val isAtLeast212 = environmentName.toInt() >= 212

val pythonProPlugin = "Pythonid:${prop("pythonProPluginVersion")}"
val pythonCommunityPlugin = "PythonCore:${prop("pythonCommunityPluginVersion")}"

val pythonPlugin = when {
  isIdeaIDE -> pythonProPlugin
  isClionIDE -> "python-ce"
  isPycharmIDE -> "python-ce"
  isStudioIDE -> pythonCommunityPlugin
  else -> error("Unexpected IDE name = `$baseIDE`")
}
val scalaPlugin = "org.intellij.scala:${prop("scalaPluginVersion")}"
val rustPlugin = "org.rust.lang:${prop("rustPluginVersion")}"
val tomlPlugin = "org.toml.lang:${prop("tomlPluginVersion")}"
val goPlugin = "org.jetbrains.plugins.go:${prop("goPluginVersion")}"
val markdownPlugin = if (isStudioIDE) "org.intellij.plugins.markdown:${prop("markdownPluginVersion")}" else "org.intellij.plugins.markdown"
val psiViewerPlugin = "PsiViewer:${prop("psiViewerPluginVersion")}"

val jvmPlugins = listOf(
  "java",
  "junit",
  "gradle-java"
)

val changesFile = "changes.html"

plugins {
  idea
  kotlin("jvm") version "1.5.0"
  id("org.jetbrains.intellij") version "1.1.4"
  id("de.undercouch.download") version "4.0.4"
  id("net.saliman.properties") version "1.5.1"
}

idea {
  project {
    jdkName = "11"
    languageLevel = IdeaLanguageLevel("1.8")
    vcs = "Git"
  }
  module {
    // https://github.com/gradle/gradle/issues/8749
    // `.add` can be used since Gradle 7.1
    excludeDirs = excludeDirs + file("dependencies")
  }
}

allprojects {
  apply {
    plugin("org.jetbrains.intellij")
    plugin("java")
    plugin("kotlin")
    plugin("net.saliman.properties")
  }

  repositories {
    mavenCentral()
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
  }

  configure<JavaPluginConvention> {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
  }
  sourceSets {
    main {
      if (project != rootProject) {
        java.srcDirs("src", "branches/$environmentName/src")
        kotlin.srcDirs("src", "branches/$environmentName/src")
      }
      resources.srcDirs("resources", "branches/$environmentName/resources")
    }

    test {
      if (project != rootProject) {
        java.srcDirs("testSrc", "branches/$environmentName/testSrc")
        kotlin.srcDirs("testSrc", "branches/$environmentName/testSrc")
        resources.srcDirs("testResources", "branches/$environmentName/testResources")
      }
    }
  }


  configurations {
    all {
      // Allows using project dependencies instead of IDE dependencies during compilation and test running
      resolutionStrategy.sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
    }
  }

  intellij {
    if (isStudioIDE) {
      localPath.set(studioPath)
    } else {
      version.set(baseVersion)
    }
  }

  tasks {
    withProp("customJbr") {
      if (it.isNotBlank()) {
        runIde {
          jbrVersion.set(it)
        }
      }
    }

    if (isStudioIDE) {
      withType<IntelliJInstrumentCodeTask> {
        compilerVersion.set(studioBuildVersion)
      }
    }

    withType<Test> {
      withProp(secretProperties, "stepikTestClientSecret") { environment("STEPIK_TEST_CLIENT_SECRET", it) }
      withProp(secretProperties, "stepikTestClientId") { environment("STEPIK_TEST_CLIENT_ID", it) }
      withProp("excludeTests") { exclude(it) }

      ignoreFailures = true
      filter {
        isFailOnNoMatchingTests = false
      }
    }

    withType<JavaCompile> { options.encoding = "UTF-8" }
    withType<KotlinCompile> {
      kotlinOptions {
        jvmTarget = "1.8"
        languageVersion = "1.5"
        apiVersion = "1.4"
        freeCompilerArgs = listOf("-Xjvm-default=enable")
      }
    }

    val verifyClasses = task("verifyClasses") {
      dependsOn(jar)
      doLast {
        if (!isAtLeast212) return@doLast
        verifyClasses(project)
      }
    }
    // Fail plugin build if there are errors in module packages
    rootProject.tasks.buildPlugin {
      dependsOn(verifyClasses)
    }
  }

  dependencies {
    implementation(group = "org.twitter4j", name = "twitter4j-core", version = "4.0.1")
    implementation("org.jsoup:jsoup:1.12.1")
    implementation("org.jetbrains:markdown:0.1.41") {
      excludeKotlinDeps()
    }
    implementation(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = jacksonVersion) {
      exclude(module = "snakeyaml")
    }
    implementation(group = "com.fasterxml.jackson.datatype", name = "jackson-datatype-jsr310", version = jacksonVersion)

    //transitive dependency is specified explicitly to avoid conflict with lib bundled since idea 181
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = jacksonVersion)

    //transitive dependency is specified explicitly because of the issue https://github.com/FasterXML/jackson-dataformats-text/issues/81
    //intellij platform uses affected snakeyaml version inside
    implementation(group = "org.yaml", name = "snakeyaml", version = "1.21")
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jacksonVersion) {
      excludeKotlinDeps()
    }

    implementation("com.squareup.retrofit2:retrofit:2.4.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.3.0")
    implementation("com.squareup.retrofit2:converter-gson:2.4.0") {
      exclude(module = "gson")
    }
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation("org.jetbrains:kotlin-css-jvm:1.0.0-pre.58-kotlin-1.3.0") {
      excludeKotlinDeps()
    }

    testImplementation("com.squareup.okhttp3:mockwebserver:$okhttpVersion")
    testImplementation("io.mockk:mockk:1.12.0") {
      excludeKotlinDeps()
    }
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

project(":") {
  val buildNumber = System.getenv("BUILD_NUMBER") ?: "SNAPSHOT"

  if (hasProp("setTCBuildNumber")) {
    // Specify build number at building plugin running configuration on TC
    // with heading plugin version: e.g. `3.8.BUILD_NUMBER` instead of `BUILD_NUMBER`
    println("##teamcity[buildNumber '$pluginVersion.$buildNumber']")
  }

  version = "$pluginVersion-$platformVersion-$buildNumber"

  intellij {
    pluginName.set("EduTools")
    updateSinceUntilBuild.set(true)
    downloadSources.set(false)

    tasks.withType<PatchPluginXmlTask> {
      changeNotes.set(provider { file(changesFile).readText() })
      pluginDescription.set(provider { file("description.html").readText() })
      sinceBuild.set(prop("customSinceBuild"))
      untilBuild.set(prop("customUntilBuild"))
    }

    val pluginsList = mutableListOf(
      rustPlugin,
      tomlPlugin,
      "yaml",
      markdownPlugin,
      // PsiViewer plugin is not a runtime dependency
      // but it helps a lot while developing features related to PSI
      psiViewerPlugin
    )
    pluginsList += listOfNotNull(pythonPlugin)
    if (isJvmCenteredIDE) {
      pluginsList += listOf("java", "junit", "Kotlin", scalaPlugin)
    }
    if (isIdeaIDE) {
      pluginsList += listOf("JavaScriptLanguage", "NodeJS", goPlugin)
    }

    plugins.set(pluginsList)
  }

  dependencies {
    compileOnly(project(":educational-core"))
    compileOnly(project(":code-insight"))
    compileOnly(project(":code-insight:html"))
    compileOnly(project(":code-insight:markdown"))
    compileOnly(project(":code-insight:yaml"))
    compileOnly(project(":jvm-core"))
    compileOnly(project(":Edu-Java"))
    compileOnly(project(":Edu-Kotlin"))
    compileOnly(project(":Edu-Python"))
    compileOnly(project(":Edu-Python:Idea"))
    compileOnly(project(":Edu-Python:PyCharm"))
    compileOnly(project(":Edu-Scala"))
    compileOnly(project(":Edu-Android"))
    compileOnly(project(":Edu-JavaScript"))
    compileOnly(project(":Edu-Rust"))
    compileOnly(project(":Edu-Cpp"))
    compileOnly(project(":Edu-Go"))
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

  tasks {
    jar {
      from({
        // Collect all `compileOnly` project dependencies and add their compilation output to jar.
        // We need to put all plugin manifest files into single jar to make new plugin model work
        val projectDependencies = project.configurations.compileOnly.get().allDependencies.withType<ProjectDependency>()
        projectDependencies.map { it.dependencyProject.sourceSets.main.get().output }
      })
    }
    withType<PrepareSandboxTask> {
      from("$rootDir/twitter") {
        into("${pluginName.get()}/twitter")
        include("**/*.gif")
      }
      finalizedBy(removeIncompatiblePlugins)
    }
    withType<RunIdeTask> {
      // Disable auto plugin reloading. See `com.intellij.ide.plugins.DynamicPluginVfsListener`
      // To enable dynamic reloading, change value to `true` and disable `EduDynamicPluginListener`
      jvmArgs("-Didea.auto.reload.plugins=false")
      jvmArgs("-Xmx2g")

      // Uncomment to show localized messages
      // jvmArgs("-Didea.l10n=true")

      // Uncomment to enable memory dump creation if plugin cannot be unloaded by the platform
      // jvmArgs("-Dide.plugins.snapshot.on.unload.fail=true")

      // Uncomment to enable FUS testing mode
      // jvmArgs("-Dfus.internal.test.mode=true")
    }
    buildSearchableOptions {
      enabled = findProperty("enableBuildSearchableOptions") != "false"
    }
  }

  task("configureIdea") {
    doLast {
      intellij.sandboxDir.set(ideaSandbox)
      withProp("ideaPath") { path ->
        tasks.runIde {
          ideDir.set(file(path))
        }
      }
    }
  }

  task("configurePyCharm") {
    doLast {
      intellij.sandboxDir.set(pycharmSandbox)
      withProp("pycharmPath") { path ->
        tasks.runIde {
          ideDir.set(file(path))
        }
      }
    }
  }

  task("configureWebStorm") {
    doLast {
      if (!hasProp("webStormPath")) {
        throw InvalidUserDataException("Path to WebStorm installed locally is needed\nDefine \"webStormPath\" property")
      }

      intellij.sandboxDir.set(webStormSandbox)
      tasks.runIde {
        ideDir.set(file(prop("webStormPath")))
      }
    }
  }

  task("configureCLion") {
    doLast {
      intellij.sandboxDir.set(clionSandbox)
      withProp("clionPath") { path ->
        tasks.runIde {
          ideDir.set(file(path))
        }
      }
    }
  }

  task("configureAndroidStudio") {
    doLast {
      intellij.sandboxDir.set(studioSandbox)
      withProp("androidStudioPath") { path ->
        tasks.runIde {
          ideDir.set(file(path))
        }
      }
    }
  }

  task("configureGoLand") {
    doLast {
      if (!hasProp("goLandPath")) {
        throw InvalidUserDataException("Path to GoLand installed locally is needed\nDefine \"goLandPath\" property")
      }

      intellij.sandboxDir.set(goLandSandbox)
      tasks.runIde {
        ideDir.set(file(prop("goLandPath")))
      }
    }
  }
}

project(":educational-core")

project(":code-insight") {
  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":code-insight:html") {
  dependencies {
    implementation(project(":educational-core"))
    implementation(project(":code-insight"))

    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":code-insight", "testOutput"))
  }
}

project(":code-insight:markdown") {
  val pluginList = mutableListOf(markdownPlugin)
  if (isStudioIDE) {
    pluginList += "platform-images"
  }
  intellij {
    plugins.set(pluginList)
  }

  tasks {
    prepareTestingSandbox {
      // Set custom plugin directory name for tests.
      // Otherwise, `prepareTestingSandbox` merge directories of `markdown` plugin and `markdown` modules
      // into single one
      pluginName.set("edu-markdown")
    }
  }

  dependencies {
    implementation(project(":educational-core"))
    implementation(project(":code-insight"))

    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":code-insight", "testOutput"))
  }
}

project(":code-insight:yaml") {
  intellij {
    plugins.set(listOf("yaml"))
  }

  dependencies {
    implementation(project(":educational-core"))
    implementation(project(":code-insight"))

    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":code-insight", "testOutput"))
  }
}

project(":jvm-core") {
  intellij {
    if (!isJvmCenteredIDE) {
      localPath.set(null as String?)
      version.set(ideaVersion)
    }
    plugins.set(jvmPlugins)
  }

  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Java") {
  intellij {
    localPath.set(null as String?)
    version.set(ideaVersion)
    plugins.set(jvmPlugins)
  }

  dependencies {
    implementation(project(":educational-core"))
    implementation(project(":jvm-core"))
    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":jvm-core", "testOutput"))
  }
}

project(":Edu-Kotlin") {
  intellij {
    if (!isJvmCenteredIDE) {
      localPath.set(null as String?)
      version.set(ideaVersion)
    }
    val pluginList = jvmPlugins + "Kotlin"
    plugins.set(pluginList)
  }

  dependencies {
    implementation(project(":educational-core"))
    implementation(project(":jvm-core"))
    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":jvm-core", "testOutput"))
  }
}

project(":Edu-Scala") {
  intellij {
    localPath.set(null as String?)
    version.set(ideaVersion)
    val pluginsList = jvmPlugins + scalaPlugin
    plugins.set(pluginsList)
  }

  dependencies {
    implementation(project(":educational-core"))
    implementation(project(":jvm-core"))
    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":jvm-core", "testOutput"))
  }
}

project(":Edu-Android") {
  intellij {
    localPath.set(studioPath)
    val pluginsList = listOf(
      "android",
      // Looks like `android-layoutlib` is semantically mandatory dependency of android plugin
      // because we get `NoClassDefFoundError` without it.
      // But it's marked as optional one so gradle-intellij-plugin doesn't load it automatically.
      // So we have to add it manually
      "android-layoutlib"
    ) + jvmPlugins
    plugins.set(pluginsList)
  }

  tasks {
    withType<IntelliJInstrumentCodeTask> {
      compilerVersion.set(studioBuildVersion)
    }
  }

  dependencies {
    implementation(project(":educational-core"))
    implementation(project(":jvm-core"))
    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":jvm-core", "testOutput"))
  }

  // BACKCOMPAT: enable when 211 studio is available
  tasks.withType<Test> {
    enabled = environmentName.toInt() < 211
  }
}

project(":Edu-Python") {
  intellij {
    val pluginList = listOfNotNull(
      pythonPlugin,
      if (isJvmCenteredIDE) "java" else null
    )
    plugins.set(pluginList)
  }

  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":Edu-Python:Idea"))
    testImplementation(project(":Edu-Python:PyCharm"))
  }
}

project(":Edu-Python:Idea") {
  intellij {
    if (!isJvmCenteredIDE || isStudioIDE) {
      localPath.set(null as String?)
      version.set(ideaVersion)
    }

    val pluginList = listOfNotNull(
      if (!isJvmCenteredIDE) pythonProPlugin else pythonPlugin,
      "java"
    )
    plugins.set(pluginList)
  }

  dependencies {
    implementation(project(":educational-core"))
    compileOnly(project(":Edu-Python"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Python:PyCharm") {
  intellij {
    if (isStudioIDE) {
      localPath.set(null as String?)
      version.set(ideaVersion)
    }
    plugins.set(listOf(pythonPlugin))
  }

  dependencies {
    implementation(project(":educational-core"))
    compileOnly(project(":Edu-Python"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-JavaScript") {
  intellij {
    localPath.set(null as String?)
    version.set(ideaVersion)
    val pluginList = mutableListOf("NodeJS", "JavaScriptLanguage")
    if (isAtLeast211) {
      pluginList += "com.intellij.css"
    }

    plugins.set(pluginList)
  }
  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Rust") {
  intellij {
    plugins.set(listOf(rustPlugin, tomlPlugin))
  }

  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))
  }

  tasks {
    withType<KotlinCompile> {
      doFirst {
        // Since 142 version of Rust & toml plugins contain conflicted markdown library with 0.2.0 version
        // TODO: find out a better way to avoid wrong dependency during compilation
        classpath = classpath.filter { it.name != "markdown-jvm-0.2.0.jar" }
      }
    }

    withType<Test> {
      doFirst {
        // Since 142 version of Rust & toml plugins contain conflicted markdown library with 0.2.0 version
        // TODO: find out a better way to avoid wrong dependency during compilation
        classpath = classpath.filter { it.name != "markdown-jvm-0.2.0.jar" }
      }
    }
  }
}

project(":Edu-Cpp") {
  intellij {
    localPath.set(null as String?)
    version.set(clionVersion)
    plugins.set(listOf("clion-test-google", "clion-test-catch", "clion", "c-plugin"))
  }

  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Go") {
  intellij {
    localPath.set(null as String?)
    version.set(ideaVersion)
    plugins.set(listOf(goPlugin))
  }

  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

fun downloadStudioIfNeededAndGetPath(): String {
  if (!rootProject.hasProperty("studioVersion")) error("studioVersion is unspecified")

  val osFamily = osFamily
  val (archiveType, fileTreeMethod) = if (osFamily == "linux") "tar.gz" to this::tarTree else "zip" to this::zipTree
  val studioArchive = file("${rootProject.projectDir}/dependencies/studio-$studioVersion-${osFamily}.$archiveType")
  if (!studioArchive.exists()) {
    download {
      src(studioArtifactDownloadPath(archiveType))
      retries(2)
      readTimeout(3 * 60 * 1000) // 3 min
      dest(studioArchive)
    }
  }

  val studioFolder = file("${rootProject.projectDir}/dependencies/studio-$studioVersion")
  if (!studioFolder.exists()) {
    copy {
      from(fileTreeMethod(studioArchive))
      into(studioFolder)
    }
  }

  return studioPath(studioFolder)
}

fun studioArtifactDownloadPath(archiveType: String): String {
  return if (inJetBrainsNetwork()) {
    println("Downloading studio from JB repo...")
    "https://repo.labs.intellij.net/edu-tools/android-studio-${studioVersion}-${osFamily}.$archiveType"
  } else {
    "http://dl.google.com/dl/android/studio/ide-zips/${studioVersion}/android-studio-${studioVersion}-${osFamily}.$archiveType"
  }
}

fun studioPath(studioFolder: File): String {
  return if (osFamily == "mac") {
    val candidates = studioFolder.listFiles()
      .filter { it.isDirectory && it.name.matches(Regex("Android Studio.*\\.app")) }
    when (candidates.size) {
      0 -> error("Can't find any folder matching `Android Studio*.app` in `$studioFolder`")
      1 -> return "${candidates[0]}/Contents"
      else -> error("More than one folder matching `Android Studio*.app` found in `$studioFolder`")
    }
  } else {
    "$studioFolder/android-studio"
  }
}

val osFamily: String get() {
  return when {
    Os.isFamily(Os.FAMILY_WINDOWS) -> "windows"
    Os.isFamily(Os.FAMILY_MAC) -> "mac"
    Os.isFamily(Os.FAMILY_UNIX) && !Os.isFamily(Os.FAMILY_MAC) -> "linux"
    else -> error("current os family is unsupported")
  }
}

val SourceSet.kotlin: SourceDirectorySet
  get() = (this as HasConvention)
      .convention
      .getPlugin(KotlinSourceSet::class.java)
      .kotlin


fun SourceSet.kotlin(action: SourceDirectorySet.() -> Unit) = kotlin.action()

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

fun <T : ModuleDependency> T.excludeKotlinDeps() {
  exclude(module = "kotlin-runtime")
  exclude(module = "kotlin-reflect")
  exclude(module = "kotlin-stdlib")
  exclude(module = "kotlin-stdlib-common")
  exclude(module = "kotlin-stdlib-jdk8")
}

// TODO: find way how to use existing functionality
fun download(configure: DownloadSpec.() -> Unit) {
  with(DownloadAction(project)) {
    configure()
    execute()
    println("Download completed")
  }
}

fun loadProperties(path: String): Properties {
  val properties = Properties()
  file(path).bufferedReader().use { properties.load(it) }
  return properties
}

/*
Workflow is the following:

1. Launch the task.
2. Review changes in changes.html and edit if needed.
3. Push.
 */
task("generate-release-notes") {
  doLast {
    val notes = generateNotes(pluginVersion)
    val changesFile = file(changesFile)
    val oldChangeNotes = changesFile.readText()
    changesFile.writeText("$notes\n$oldChangeNotes")
  }
}

fun parseManifest(file: File): Node {
  val node = XmlParser().parse(file)
  check(node.name() == "idea-plugin") {
    "Manifest file `$file` doesn't contain top-level `idea-plugin` attribute"
  }
  return node
}

fun manifestFile(project: Project, filePath: String? = null): File {
  val mainOutput = project.sourceSets.main.get().output
  val resourcesDir = mainOutput.resourcesDir ?: error("Failed to find resources dir for ${project.name}")

  if (filePath != null) {
    return resourcesDir.resolve(filePath).takeIf { it.exists() } ?: error("Failed to find manifest file for ${project.name} module")
  }
  val rootManifest = parseManifest(manifestFile(rootProject, "META-INF/plugin.xml"))
  val children = ((rootManifest["content"] as? List<*>)?.single() as? Node)?.children()
                 ?: error("Failed to find module declarations in root manifest")
  return children.filterIsInstance<Node>()
    .flatMap { node ->
      if (node.name() != "module") return@flatMap emptyList()
      val name = node.attribute("name") as? String ?: return@flatMap emptyList()
      listOfNotNull(resourcesDir.resolve ("$name.xml").takeIf { it.exists() })
    }.firstOrNull() ?: error("Failed to find manifest file for ${project.name} module")
}

fun findModulePackage(project: Project): String {
  // Some gradle projects are not modules from IDEA plugin point of view
  // because we use `include` for them inside manifests, i.e. they just a part of another module.
  // That's why we delegate manifest search to other projects in some cases
  val moduleManifest = when (project.path) {
    ":", ":educational-core", ":code-insight" -> manifestFile(rootProject, "META-INF/plugin.xml")
    ":Edu-Python:Idea", ":Edu-Python:PyCharm" -> manifestFile(project.parent!!)
    else -> manifestFile(project)
  }

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
