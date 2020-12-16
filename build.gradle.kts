import de.undercouch.gradle.tasks.download.DownloadAction
import de.undercouch.gradle.tasks.download.DownloadSpec
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.internal.HasConvention
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

apply(from = "common.gradle.kts")

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

val baseVersion = when (baseIDE) {
  "idea" -> ideaVersion
  "clion" -> clionVersion
  "pycharm" -> pycharmVersion
  "studio" -> studioVersion
  else -> error("Unexpected IDE name = `$baseIDE`")
}

val studioPath: String
  get() {
    val androidStudioPath: String? by project
    return androidStudioPath ?: downloadStudioIfNeededAndGetPath()
  }

val jacksonVersion = "2.10.0"

val ideaSandbox = "${project.buildDir.absolutePath}/idea-sandbox"
val pycharmSandbox = "${project.buildDir.absolutePath}/pycharm-sandbox"
val studioSandbox = "${project.buildDir.absolutePath}/studio-sandbox"
val webStormSandbox = "${project.buildDir.absolutePath}/webstorm-sandbox"
val clionSandbox = "${project.buildDir.absolutePath}/clion-sandbox"
val goLandSandbox = "${project.buildDir.absolutePath}/goland-sandbox"

val isAtLeast203 = environmentName.toInt() >= 203

val pythonProPlugin = "Pythonid:${prop("pythonProPluginVersion")}"
val pythonCommunityPlugin = "PythonCore:${prop("pythonCommunityPluginVersion")}"

val pythonPlugin = when (baseIDE) {
  "idea" -> pythonProPlugin
  "clion" -> "python-ce"
  "pycharm" -> "python-ce"
  "studio" -> pythonCommunityPlugin
  else -> error("Unexpected IDE name = `$baseIDE`")
}
val cPlugin = "c-plugin"
val scalaPlugin = "org.intellij.scala:${prop("scalaPluginVersion")}"
val rustPlugin = "org.rust.lang:${prop("rustPluginVersion")}"
val tomlPlugin = "org.toml.lang:${prop("tomlPluginVersion")}"
val goPlugin = "org.jetbrains.plugins.go:${prop("goPluginVersion")}"
val markdownPlugin = if (baseIDE == "studio") "org.intellij.plugins.markdown:${prop("markdownPluginVersion")}" else "org.intellij.plugins.markdown"

val jvmPlugins = arrayOf(
  "java",
  "junit",
  "gradle-java"
)

plugins {
  idea
  kotlin("jvm") version "1.4.10"
  id("org.jetbrains.intellij") version "0.6.3"
  id("de.undercouch.download") version "4.0.4"
  id("net.saliman.properties") version "1.5.1"
}

idea {
  project {
    jdkName = "11"
    languageLevel = IdeaLanguageLevel("1.8")
    vcs = "Git"
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
    maven("https://dl.bintray.com/jetbrains/markdown")
    maven("https://dl.bintray.com/kotlin/kotlin-js-wrappers/")
  }

  configure<JavaPluginConvention> {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
  }

  intellij {
    if (baseIDE == "studio") {
      localPath = studioPath
    } else {
      version = baseVersion
    }
  }

  tasks {
    withProp("customJbr") {
      if (it.isNotBlank()) {
        runIde {
          setJbrVersion(it)
        }
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
        languageVersion = "1.4"
        apiVersion = "1.3"
        freeCompilerArgs = listOf("-Xjvm-default=enable")
      }
    }
  }

  dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
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
    implementation("com.squareup.retrofit2:converter-gson:2.4.0")
    implementation("com.squareup.okhttp3:logging-interceptor:3.14.0")
    implementation("org.jetbrains:kotlin-css-jvm:1.0.0-pre.58-kotlin-1.3.0") {
      excludeKotlinDeps()
    }

    testImplementation("com.squareup.okhttp3:mockwebserver:3.14.0")
  }
}

subprojects {
  sourceSets {
    main {
      java.srcDirs("src", "branches/$environmentName/src")
      resources.srcDirs("resources", "branches/$environmentName/resources")
      kotlin.srcDirs("src", "branches/$environmentName/src")
    }

    test {
      java.srcDirs("testSrc", "branches/$environmentName/testSrc")
      resources.srcDirs("testResources", "branches/$environmentName/testResources")
      kotlin.srcDirs("testSrc", "branches/$environmentName/testSrc")
    }
  }

  tasks {
    runIde { enabled = false }
    prepareSandbox { enabled = false }
    buildSearchableOptions { enabled = false }
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

  sourceSets {
    main {
      resources.srcDirs("resources")
    }
  }

  intellij {
    pluginName = "EduTools"
    updateSinceUntilBuild = true
    downloadSources = false

    tasks.withType<PatchPluginXmlTask> {
      changeNotes(file("changes.html").readText())
      pluginDescription(file("description.html").readText())
      sinceBuild(prop("customSinceBuild"))
      untilBuild(prop("customUntilBuild"))
    }

    val pluginsList = mutableListOf(
      rustPlugin,
      tomlPlugin,
      "yaml",
      markdownPlugin
    )
    pluginsList += listOfNotNull(pythonPlugin)
    if (isJvmCenteredIDE) {
      pluginsList += listOf("java", "junit", "Kotlin", scalaPlugin)
    }
    if (baseIDE == "idea") {
      pluginsList += listOf("NodeJS", "JavaScriptLanguage", goPlugin)
    }

    setPlugins(*pluginsList.toTypedArray())
  }

  dependencies {
    implementation(project(":educational-core"))
    implementation(project(":code-insight"))
    implementation(project(":code-insight:html"))
    implementation(project(":code-insight:markdown"))
    implementation(project(":code-insight:yaml"))
    implementation(project(":jvm-core"))
    implementation(project(":Edu-Java"))
    implementation(project(":Edu-Kotlin"))
    implementation(project(":Edu-Python"))
    implementation(project(":Edu-Python:Idea"))
    implementation(project(":Edu-Python:PyCharm"))
    implementation(project(":Edu-Scala"))
    implementation(project(":Edu-Android"))
    implementation(project(":Edu-JavaScript"))
    implementation(project(":Edu-Rust"))
    implementation(project(":Edu-Cpp"))
    implementation(project(":Edu-Go"))
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
    withType<PrepareSandboxTask> {
      from("$rootDir/twitter") {
        into("${intellij.pluginName}/twitter")
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
    }
    buildSearchableOptions {
      enabled = findProperty("enableBuildSearchableOptions") != "false"
    }
  }

  task("configureIdea") {
    doLast {
      intellij.sandboxDirectory = ideaSandbox
      withProp("ideaPath") {
        intellij.alternativeIdePath = it
      }
    }
  }

  task("configurePyCharm") {
    doLast {
      intellij.sandboxDirectory = pycharmSandbox
      withProp("pycharmPath") {
        intellij.alternativeIdePath = it
      }
    }
  }

  task("configureWebStorm") {
    doLast {
      if (!hasProp("webStormPath")) {
        throw InvalidUserDataException("Path to WebStorm installed locally is needed\nDefine \"webStormPath\" property")
      }

      intellij.sandboxDirectory = webStormSandbox
      intellij.alternativeIdePath = prop("webStormPath")
    }
  }

  task("configureCLion") {
    doLast {
      intellij.sandboxDirectory = clionSandbox
      withProp("clionPath") {
        intellij.alternativeIdePath = it
      }
    }
  }

  task("configureAndroidStudio") {
    doLast {
      intellij.sandboxDirectory = studioSandbox
      withProp("androidStudioPath") {
        intellij.alternativeIdePath = it
      }
    }
  }

  task("configureGoLand") {
    doLast {
      if (!hasProp("goLandPath")) {
        throw InvalidUserDataException("Path to GoLand installed locally is needed\nDefine \"goLandPath\" property")
      }

      intellij.sandboxDirectory = goLandSandbox
      intellij.alternativeIdePath = prop("goLandPath")
    }
  }
}

project(":educational-core") {

  val testOutput = configurations.create("testOutput")

  dependencies {
    testOutput(sourceSets.getByName("test").output.classesDirs)
  }
}

project(":code-insight") {

  val testOutput = configurations.create("testOutput")

  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))

    testOutput(sourceSets.getByName("test").output.classesDirs)
  }
}

project(":code-insight:html") {
  intellij {
    if (baseIDE == "clion" && isAtLeast203) {
      /**
       * TODO: Remove it after CLion plugin will be fixed
       * id from ProductivityFeaturesRegistry.xml automatically looking at FeatureStatisticsBundle,
       * and seems like ProductivityFeaturesRegistry can't exist in a plugin, only in product
       */
      setPlugins(cPlugin)
    }
  }

  dependencies {
    implementation(project(":educational-core"))
    implementation(project(":code-insight"))

    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":code-insight", "testOutput"))
  }
}

project(":code-insight:markdown") {

  intellij {
    val plugins = mutableListOf(markdownPlugin)
    if (baseIDE == "clion" && isAtLeast203) {
      /**
       * TODO: Remove it after CLion plugin will be fixed
       * id from ProductivityFeaturesRegistry.xml automatically looking at FeatureStatisticsBundle,
       * and seems like ProductivityFeaturesRegistry can't exist in a plugin, only in product
       */
      plugins += cPlugin
    }
    setPlugins(*plugins.toTypedArray())
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
    val pluginsList = mutableListOf("yaml")
    if (baseIDE == "clion" && isAtLeast203) {
      /**
       * TODO: Remove it after CLion plugin will be fixed
       * id from ProductivityFeaturesRegistry.xml automatically looking at FeatureStatisticsBundle,
       * and seems like ProductivityFeaturesRegistry can't exist in a plugin, only in product
       */
      pluginsList += cPlugin
    }
    setPlugins(*pluginsList.toTypedArray())
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
      localPath = null
      version = ideaVersion
    }
    setPlugins(*jvmPlugins)
  }

  val testOutput = configurations.create("testOutput")

  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))

    testOutput(sourceSets.getByName("test").output.classesDirs)
  }
}

project(":Edu-Java") {
  intellij {
    localPath = null
    version = ideaVersion
    setPlugins(*jvmPlugins)
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
      localPath = null
      version = ideaVersion
    }
    val plugins = listOf(
      "Kotlin",
      *jvmPlugins
    )
    setPlugins(*plugins.toTypedArray())
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
    localPath = null
    version = ideaVersion
    val plugins = listOf(
      scalaPlugin,
      *jvmPlugins
    )
    setPlugins(*plugins.toTypedArray())
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
    localPath = studioPath
    val plugins = listOf(
      "android",
      // Looks like `android-layoutlib` is semantically mandatory dependency of android plugin
      // because we get `NoClassDefFoundError` without it.
      // But it's marked as optional one so gradle-intellij-plugin doesn't load it automatically.
      // So we have to add it manually
      "android-layoutlib",
      *jvmPlugins
    )
    setPlugins(*plugins.toTypedArray())
  }

  dependencies {
    implementation(project(":educational-core"))
    implementation(project(":jvm-core"))
    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":jvm-core", "testOutput"))
  }

  // BACKCOMPAT: enable when 203 studio is available
  tasks.withType<Test> {
    enabled = environmentName.toInt() < 203
  }
}

project(":Edu-Python") {
  intellij {
    val plugins = listOfNotNull(
      pythonPlugin,
      if (isJvmCenteredIDE) "java" else null
    )
    setPlugins(*plugins.toTypedArray())
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
    if (!isJvmCenteredIDE || baseIDE == "studio") {
      localPath = null
      version = ideaVersion
    }

    val plugins = listOfNotNull(
      if (!isJvmCenteredIDE) pythonProPlugin else pythonPlugin,
      "java"
    )
    setPlugins(*plugins.toTypedArray())
  }

  dependencies {
    implementation(project(":educational-core"))
    compileOnly(project(":Edu-Python"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Python:PyCharm") {
  intellij {
    if (baseIDE == "studio") {
      localPath = null
      version = ideaVersion
    }
    setPlugins(pythonPlugin)
  }

  dependencies {
    implementation(project(":educational-core"))
    compileOnly(project(":Edu-Python"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-JavaScript") {
  intellij {
    localPath = null
    version = ideaVersion
    val plugins = listOf(
      "NodeJS",
      "JavaScriptLanguage"
    )
    setPlugins(*plugins.toTypedArray())
  }
  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Rust") {
  intellij {
    setPlugins(rustPlugin, tomlPlugin)
  }

  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Cpp") {
  intellij {
    localPath = null
    version = clionVersion
    val pluginsList = mutableListOf("clion-test-google", "clion-test-catch")
    if (isAtLeast203) {
      pluginsList += listOf("clion", cPlugin)
    }
    setPlugins(*pluginsList.toTypedArray())
  }

  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Go") {
  intellij {
    localPath = null
    version = ideaVersion
    setPlugins(goPlugin)
  }

  dependencies {
    implementation(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

fun downloadStudioIfNeededAndGetPath(): String {
  if (!rootProject.hasProperty("studioVersion")) error("studioVersion is unspecified")
  if (!rootProject.hasProperty("studioBuildVersion")) error("studioBuildVersion is unspecified")

  val osFamily = osFamily
  val (archiveType, fileTreeMethod) = if (osFamily == "linux") "tar.gz" to this::tarTree else "zip" to this::zipTree
  val studioArchive = file("${rootProject.projectDir}/dependencies/studio-$studioVersion-$studioBuildVersion-${osFamily}.$archiveType")
  if (!studioArchive.exists()) {
    download {
      src(studioArtifactDownloadPath(archiveType))
      retries(2)
      readTimeout(3 * 60 * 1000) // 3 min
      dest(studioArchive)
    }
  }

  val studioFolder = file("${rootProject.projectDir}/dependencies/studio-$studioVersion-$studioBuildVersion")
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
    "https://repo.labs.intellij.net/edu-tools/android-studio-ide-${studioBuildVersion}-${osFamily}.$archiveType"
  } else {
    println("Downloading studio from google's website...")
    "http://dl.google.com/dl/android/studio/ide-zips/${studioVersion}/android-studio-ide-${studioBuildVersion}-${osFamily}.$archiveType"
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
