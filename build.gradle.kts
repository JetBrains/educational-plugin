import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.DownloadAction
import de.undercouch.gradle.tasks.download.DownloadSpec
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.internal.HasConvention
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Properties

val secretProperties = "secret.properties"

val environmentName: String by project
val baseIDE: String by project
val isJvmCenteredIDE = baseIDE in listOf("idea", "studio")

val ideaVersion: String by project
val clionVersion: String by project
val pycharmVersion: String by project
val studioVersion: String by project

val studioBuildVersion: String by project

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

val isAtLeast193 = environmentName.toInt() >= 193
val isAtLeast201 = environmentName.toInt() >= 201
// TODO remove when 202 EAPs will be available
val is202 = environmentName.toInt() == 202

val pythonProPlugin = "Pythonid:${prop("pythonProPluginVersion")}"
val pythonCommunityPlugin = "PythonCore:${prop("pythonCommunityPluginVersion")}"
// python community plugin is not compatible with IDEA Ultimate since 2019.3
val pythonIDEAPlugin = if (isAtLeast193) pythonProPlugin else pythonCommunityPlugin

val pythonPlugin = when (baseIDE) {
  "idea" -> pythonIDEAPlugin
  "clion" -> if (isAtLeast193) "python-ce" else "python"
  // PyCharm has separate python plugin since 2019.3. Previously it was part of PyCharm core code
  "pycharm" -> if (isAtLeast193) "python-ce" else null
  "studio" -> pythonCommunityPlugin
  else -> error("Unexpected IDE name = `$baseIDE`")
}
val scalaPlugin = "org.intellij.scala:${prop("scalaPluginVersion")}"
val rustPlugin = "org.rust.lang:${prop("rustPluginVersion")}"
val tomlPlugin = "org.toml.lang:${prop("tomlPluginVersion")}"
val goPlugin = "org.jetbrains.plugins.go:${prop("goPluginVersion")}"

plugins {
  idea
  kotlin("jvm") version "1.3.72"
  id("org.jetbrains.intellij") version "0.4.20"
  id("de.undercouch.download") version "4.0.4"
  id("net.saliman.properties") version "1.5.1"
  id("org.openjfx.javafxplugin") version "0.0.8"
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
    plugin("org.openjfx.javafxplugin")
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

  javafx {
    version = "11.0.2"
    modules = listOf("javafx.web", "javafx.swing")
    configuration = "compileOnly"
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
        languageVersion = "1.3"
        apiVersion = "1.3"
        freeCompilerArgs = listOf("-Xjvm-default=enable")
      }
    }
  }

  dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile(group = "org.twitter4j", name = "twitter4j-core", version = "4.0.1")
    compile("org.jsoup:jsoup:1.11.2")
    compile("org.jetbrains:markdown:0.1.41") {
      excludeKotlinDeps()
    }

    compile(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = jacksonVersion) {
      exclude(module = "snakeyaml")
    }
    //transitive dependency is specified explicitly to avoid conflict with lib bundled since idea 181
    compile(group = "com.fasterxml.jackson.core", name = "jackson-core", version = jacksonVersion)

    //transitive dependency is specified explicitly because of the issue https://github.com/FasterXML/jackson-dataformats-text/issues/81
    //intellij platform uses affected snakeyaml version inside
    compile(group = "org.yaml", name = "snakeyaml", version = "1.21")
    compile(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jacksonVersion) {
      excludeKotlinDeps()
    }

    compile("com.squareup.retrofit2:retrofit:2.4.0")
    compile("com.squareup.retrofit2:converter-jackson:2.3.0")
    compile("com.squareup.retrofit2:converter-gson:2.4.0")
    compile("com.squareup.okhttp3:logging-interceptor:3.14.0")
    compile("org.jetbrains:kotlin-css-jvm:1.0.0-pre.58-kotlin-1.3.0") {
      excludeKotlinDeps()
    }

    testCompile("com.squareup.okhttp3:mockwebserver:3.14.0")
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
  version = "${prop("pluginVersion")}-$buildNumber"

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
      "yaml"
    )
    pluginsList += listOfNotNull(pythonPlugin)
    if (isJvmCenteredIDE) {
      pluginsList += listOf("java", "junit", "Kotlin", scalaPlugin)
    }
    if (baseIDE == "idea") {
      pluginsList += listOf("NodeJS", "JavaScriptLanguage", goPlugin)
    }

    if (is202) {
      setPlugins(*listOf("java", "junit", "Kotlin").toTypedArray())
    } else {
      setPlugins(*pluginsList.toTypedArray())
    }
  }

  dependencies {
    compile(project(":educational-core"))
    compile(project(":jvm-core"))
    compile(project(":Edu-YAML"))
    compile(project(":Edu-Java"))
    compile(project(":Edu-Kotlin"))

    if (!is202) {
      compile(project(":Edu-Python"))
      compile(project(":Edu-Python:Idea"))
      compile(project(":Edu-Python:PyCharm"))
      compile(project(":Edu-Scala"))
      compile(project(":Edu-Android"))
      compile(project(":Edu-JavaScript"))
      compile(project(":Edu-Rust"))
      compile(project(":Edu-Cpp"))
      compile(project(":Edu-Go"))
    }
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
      finalizedBy(removeIncompatiblePlugins)
    }
    buildSearchableOptions {
      enabled = findProperty("enableBuildSearchableOptions") != "false"
    }
  }

  afterEvaluate {
    configureSecretProperties()
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

project(":jvm-core") {
  intellij {
    if (!isJvmCenteredIDE) {
      localPath = null
      version = ideaVersion
    }
    val plugins = mutableListOf(
      "java",
      "junit",
      "properties",
      "gradle",
      "Groovy"
    )
    if (isAtLeast193) {
      plugins += "gradle-java"
    }
    setPlugins(*plugins.toTypedArray())
  }

  val testOutput = configurations.create("testOutput")

  dependencies {
    compile(project(":educational-core"))
    testCompile(project(":educational-core", "testOutput"))

    testOutput(sourceSets.getByName("test").output.classesDirs)
  }
}

project(":Edu-YAML") {
  intellij {
      setPlugins("yaml")
  }

  dependencies {
    compile(project(":educational-core"))
    testCompile(project(":educational-core", "testOutput"))
  }
}


project(":Edu-Java") {
  intellij {
    localPath = null
    version = ideaVersion
    val plugins = mutableListOf(
      "java",
      "junit",
      "properties",
      "gradle",
      "Groovy"
    )
    if (isAtLeast193) {
      plugins += "gradle-java"
    }
    setPlugins(*plugins.toTypedArray())
  }

  dependencies {
    compile(project(":educational-core"))
    compile(project(":jvm-core"))
    testCompile(project(":educational-core", "testOutput"))
    testCompile(project(":jvm-core", "testOutput"))
  }
}

project(":Edu-Kotlin") {
  intellij {
    if (!isJvmCenteredIDE) {
      localPath = null
      version = ideaVersion
    }
    val plugins = mutableListOf(
      "Kotlin",
      "java",
      "junit",
      "properties",
      "gradle",
      "Groovy"
    )
    if (isAtLeast193) {
      plugins += "gradle-java"
    }
    setPlugins(*plugins.toTypedArray())
  }

  dependencies {
    compile(project(":educational-core"))
    compile(project(":jvm-core"))
    testCompile(project(":educational-core", "testOutput"))
    testCompile(project(":jvm-core", "testOutput"))
  }
}

if (!is202) {
  project(":Edu-Scala") {
    intellij {
      localPath = null
      version = ideaVersion
      val plugins = mutableListOf(
        scalaPlugin,
        "java",
        "junit",
        "properties",
        "gradle",
        "Groovy"
      )
      if (isAtLeast193) {
        plugins += "gradle-java"
      }
      setPlugins(*plugins.toTypedArray())
    }

    dependencies {
      compile(project(":educational-core"))
      compile(project(":jvm-core"))
      testCompile(project(":educational-core", "testOutput"))
      testCompile(project(":jvm-core", "testOutput"))
    }
  }

  project(":Edu-Android") {
    intellij {
      localPath = studioPath
      val plugins = mutableListOf(
        "android",
        "junit",
        "properties",
        "gradle",
        "Groovy",
        "IntelliLang",
        "smali",
        "Kotlin",
        "java",
        "android-layoutlib"
      )
      if (isAtLeast193) {
        plugins += "gradle-java"
      }
      setPlugins(*plugins.toTypedArray())
    }

    dependencies {
      compile(project(":educational-core"))
      compile(project(":jvm-core"))
      testCompile(project(":educational-core", "testOutput"))
      testCompile(project(":jvm-core", "testOutput"))
    }

    //BACKCOMPAT: enable when 201 studio is available
    tasks.withType(Test::class.java) {
      enabled = environmentName.toInt() == 193
    }
  }

  project(":Edu-Python") {
    intellij {
      val plugins = listOfNotNull(
        pythonPlugin,
        // python pro plugin has mandatory dependency on yaml plugin
        "yaml",
        if (isJvmCenteredIDE) "java" else null
      )
      setPlugins(*plugins.toTypedArray())
    }

    dependencies {
      compile(project(":educational-core"))
      testCompile(project(":educational-core", "testOutput"))
      testCompile(project(":Edu-Python:Idea"))
      testCompile(project(":Edu-Python:PyCharm"))
    }
  }

  project(":Edu-Python:Idea") {
    intellij {
      if (!isJvmCenteredIDE || isAtLeast193 && baseIDE == "studio") {
        localPath = null
        version = ideaVersion
      }

      val plugins = listOfNotNull(
        if (!isJvmCenteredIDE) pythonIDEAPlugin else pythonPlugin,
        // python pro plugin has mandatory dependency on yaml plugin
        "yaml",
        "java"
      )
      setPlugins(*plugins.toTypedArray())
    }

    dependencies {
      compile(project(":educational-core"))
      compileOnly(project(":Edu-Python"))
      testCompile(project(":educational-core", "testOutput"))
    }
  }

  project(":Edu-Python:PyCharm") {
    intellij {
      if (isAtLeast193 && baseIDE == "studio") {
        localPath = null
        version = ideaVersion
      }
      // python pro plugin has mandatory dependency on yaml plugin
      val plugins = listOfNotNull(pythonPlugin, "yaml")
      setPlugins(*plugins.toTypedArray())
    }

    dependencies {
      compile(project(":educational-core"))
      compileOnly(project(":Edu-Python"))
      testCompile(project(":educational-core", "testOutput"))
    }
  }

  project(":Edu-JavaScript") {
    intellij {
      localPath = null
      version = ideaVersion
      val plugins = mutableListOf(
        "NodeJS",
        "JavaScriptLanguage",
        "CSS",
        "JavaScriptDebugger"
      )
      if (isAtLeast201) {
        // Internal CSS plugin dependency
        plugins += "platform-images"
      }
      setPlugins(*plugins.toTypedArray())
    }
    dependencies {
      compile(project(":educational-core"))
      testCompile(project(":educational-core", "testOutput"))
    }
  }

  project(":Edu-Rust") {
    intellij {
      setPlugins(rustPlugin, tomlPlugin)
    }

    dependencies {
      compile(project(":educational-core"))
      testCompile(project(":educational-core", "testOutput"))
    }
  }

  project(":Edu-Cpp") {
    intellij {
      localPath = null
      version = clionVersion
    if (isAtLeast193) {
      setPlugins("clion-test-google", "clion-test-catch")
    }
  }

    dependencies {
      compile(project(":educational-core"))
      testCompile(project(":educational-core", "testOutput"))
    }
  }

  project(":Edu-Go") {
    intellij {
      localPath = null
      version = ideaVersion
      setPlugins(goPlugin)
    }

    dependencies {
      compile(project(":educational-core"))
      testCompile(project(":educational-core", "testOutput"))
    }
  }
}

fun configureSecretProperties() {
  if (inJetBrainsNetwork()) {
    download {
      src("https://repo.labs.intellij.net/edu-tools/secret.properties")
      dest(secretProperties)
    }
  }
  else {
    val secretProperties = file(secretProperties)
    if (!secretProperties.exists()) {
      secretProperties.createNewFile()
    }
  }

  val secretProperties = loadProperties(secretProperties)

  secretProperties.extractAndStore("educational-core/resources/stepik/stepik.properties", "stepikClientId", "stepikClientSecret")
  secretProperties.extractAndStore("educational-core/resources/hyperskill/hyperskill-oauth.properties", "hyperskillClientId", "hyperskillClientSecret")
  secretProperties.extractAndStore("educational-core/resources/twitter/oauth_twitter.properties", "twitterConsumerKey", "twitterConsumerSecret")
  secretProperties.extractAndStore("Edu-Python/resources/checkio/py-checkio-oauth.properties", "pyCheckioClientId", "pyCheckioClientSecret")
  secretProperties.extractAndStore("Edu-JavaScript/resources/checkio/js-checkio-oauth.properties", "jsCheckioClientId", "jsCheckioClientSecret")
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
    "https://repo.labs.intellij.net/edu-tools/android-studio-ide-${studioBuildVersion}-${osFamily}.$archiveType"
  } else {
    "http://dl.google.com/dl/android/studio/ide-zips/${studioVersion}/android-studio-ide-${studioBuildVersion}-${osFamily}.$archiveType"
  }
}

fun inJetBrainsNetwork(): Boolean {
  var inJetBrainsNetwork = false
  try {
    inJetBrainsNetwork = InetAddress.getByName("repo.labs.intellij.net").isReachable(1000)
    if (!inJetBrainsNetwork && org.gradle.internal.os.OperatingSystem.current().isWindows) {
      inJetBrainsNetwork = Runtime.getRuntime().exec("ping -n 1 repo.labs.intellij.net").waitFor() == 0
    }
  } catch (ignored: UnknownHostException) {}
  return inJetBrainsNetwork
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
  if (!file(filePath).exists()) return
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
  }
}

fun loadProperties(path: String): Properties {
  val properties = Properties()
  file(path).bufferedReader().use { properties.load(it) }
  return properties
}

fun Properties.extractAndStore(path: String, vararg keys: String) {
  val properties = Properties()
  for (key in keys) {
    properties[key] = getProperty(key) ?: ""
  }
  val file = file(path)
  file.parentFile?.mkdirs()
  file.bufferedWriter().use { properties.store(it, "") }
}
