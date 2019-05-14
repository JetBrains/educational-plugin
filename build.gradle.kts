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

val environmentName: String by project
val baseIDE: String by project
val isJvmCenteredIDE = baseIDE in listOf("idea", "studio")

val ideaVersion: String by project
val clionVersion: String by project
val studioVersion: String by project

val studioBuildVersion: String by project

val baseVersion = when (baseIDE) {
  "idea" -> ideaVersion
  "clion" -> clionVersion
  "studio" -> studioVersion
  else -> error("Unexpected IDE name = `$baseIDE`")
}

val studioPath: String
  get() {
    val androidStudioPath: String? by project
    return androidStudioPath ?: downloadStudioIfNeededAndGetPath()
  }

val jacksonVersion = "2.9.5"

val pycharmSandbox = "${project.buildDir.absolutePath}/pycharm-sandbox"
val studioSandbox = "${project.buildDir.absolutePath}/studio-sandbox"
val webStormSandbox = "${project.buildDir.absolutePath}/webstorm-sandbox"
val clionSandbox = "${project.buildDir.absolutePath}/clion-sandbox"

plugins {
  idea
  kotlin("jvm") version "1.3.11"
  id("org.jetbrains.intellij") version "0.4.5"
  id("de.undercouch.download") version "3.4.3"
  id("net.saliman.properties") version "1.4.6"
}

idea {
  project {
    jdkName = "1.8"
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

    tasks {
      buildSearchableOptions {
        enabled = findProperty("enableBuildSearchableOptions") != "false"
      }
    }
  }

  tasks {
    withType<Test> {
      withProp("stepikTestClientSecret") { environment("STEPIK_TEST_CLIENT_SECRET", it) }
      withProp("stepikTestClientId") { environment("STEPIK_TEST_CLIENT_ID", it) }
      withProp("excludeTests") { exclude(it) }

      ignoreFailures = true
    }

    withType<JavaCompile> { options.encoding = "UTF-8" }
    withType<KotlinCompile> {
      kotlinOptions {
        jvmTarget = "1.8"
        languageVersion = "1.2"
        apiVersion = "1.2"
      }
    }
  }

  dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile(group = "org.twitter4j", name = "twitter4j-core", version = "4.0.1")
    compile("org.jsoup:jsoup:1.11.2")
    compile("org.jetbrains:markdown:0.1.28") {
      excludeKotlinDeps()
    }

    compile(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = jacksonVersion) {
      exclude(module = "snakeyaml")
    }
    //transitive dependency is specified explicitly to avoid conflict with lib bundled since idea 181
    compile(group = "com.fasterxml.jackson.core", name = "jackson-core", version = jacksonVersion)

    //transitive dependency is specified explicitly because of the issue https://github.com/FasterXML/jackson-dataformats-text/issues/81
    //intellij platform uses affected snakeyaml version inside
    compile(group = "org.yaml", name = "snakeyaml", version = "1.19")
    compile(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jacksonVersion) {
      excludeKotlinDeps()
    }

    compile("com.squareup.retrofit2:retrofit:2.4.0")
    compile("com.squareup.retrofit2:converter-jackson:2.3.0")
    compile("com.squareup.retrofit2:converter-gson:2.4.0")
    compile("com.squareup.okhttp3:logging-interceptor:3.13.0")

    compileOnly(fileTree("${rootProject.buildDir}/javafx/jre/lib/ext"))

    compile("org.jetbrains:kotlin-css-jvm:1.0.0-pre.58-kotlin-1.3.0") {
      excludeKotlinDeps()
    }
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

    val pluginsList = mutableListOf("PythonCore:${prop("pythonPluginVersion")}", "org.rust.lang:${prop("rustPluginVersion")}")
    if (isJvmCenteredIDE) {
      pluginsList += listOf("junit", "Kotlin", "org.intellij.scala:${prop("scalaPluginVersion")}")
    }
    if (baseIDE == "idea") {
      pluginsList += listOf("NodeJS:${prop("nodeJsPluginVersion")}", "JavaScriptLanguage")
    }

    setPlugins(*pluginsList.toTypedArray())
  }

  dependencies {
    compile(project(":educational-core"))
    compile(project(":jvm-core"))
    compile(project(":Edu-Python"))
    compile(project(":Edu-Kotlin"))
    compile(project(":Edu-Java"))
    compile(project(":Edu-Scala"))
    compile(project(":Edu-Android"))
    compile(project(":Edu-JavaScript"))
    compile(project(":Edu-Rust"))
    compile(project(":Edu-Cpp"))
  }

  val downloadJavaFx = task<Download>("downloadJavaFx") {
    overwrite(true)
    src("http://download.jetbrains.com/idea/open-jfx/javafx-sdk-overlay.zip")
    dest("${project.buildDir}/javafx.zip")
  }

  val prepareJavaFx = task<Copy>("prepareJavaFx") {
    val javafxFile = file("${project.buildDir}/javafx.zip")
    onlyIf { javafxFile.exists() }
    from(zipTree(javafxFile))
    into(file("${project.buildDir}/javafx"))
  }

  prepareJavaFx.dependsOn(downloadJavaFx)

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

  tasks.withType<PrepareSandboxTask> {
    finalizedBy(removeIncompatiblePlugins)
  }

  task("configurePyCharm") {
    doLast {
      if (!hasProp("pycharmPath")) {
        throw InvalidUserDataException("Path to PyCharm installed locally is needed\nDefine \"pycharmPath\" property")
      }

      intellij.sandboxDirectory = pycharmSandbox
      intellij.alternativeIdePath = prop("pycharmPath")
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
}

project(":educational-core") {

  task<Download>("downloadColorFile") {
    overwrite(false)
    src("https://raw.githubusercontent.com/ozh/github-colors/master/colors.json")
    dest("${projectDir}/resources/languageColors/colors.json")
  }

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
    setPlugins("junit", "properties", "gradle", "Groovy")
  }

  val testOutput = configurations.create("testOutput")

  dependencies {
    compile(project(":educational-core"))
    testCompile(project(":educational-core", "testOutput"))

    testOutput(sourceSets.getByName("test").output.classesDirs)
  }
}

project(":Edu-Java") {
  intellij {
    localPath = null
    version = ideaVersion
    setPlugins("junit", "properties", "gradle", "Groovy")
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
    setPlugins("Kotlin", "junit", "properties", "gradle", "Groovy")
  }

  dependencies {
    compile(project(":educational-core"))
    compile(project(":jvm-core"))
    testCompile(project(":educational-core", "testOutput"))
    testCompile(project(":jvm-core", "testOutput"))
  }
}

project(":Edu-Scala") {
  intellij {
    localPath = null
    version = ideaVersion
      setPlugins("org.intellij.scala:${prop("scalaPluginVersion")}", "junit", "properties", "gradle", "Groovy")
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
    setPlugins("android", "junit", "properties", "gradle", "Groovy", "IntelliLang", "smali")
  }

  dependencies {
    compile(project(":educational-core"))
    compile(project(":jvm-core"))
    testCompile(project(":educational-core", "testOutput"))
    testCompile(project(":jvm-core", "testOutput"))
  }
}

project(":Edu-Python") {
  intellij {
    // FIXME we should compile python module with CLion too
    if (!isJvmCenteredIDE) {
      localPath = null
      version = ideaVersion
    }
    setPlugins("PythonCore:${prop("pythonPluginVersion")}")
  }

  dependencies {
    compile(project(":educational-core"))
    testCompile(project(":educational-core", "testOutput"))
  }
}

project(":Edu-JavaScript") {
  intellij {
    localPath = null
    version = ideaVersion
    setPlugins("NodeJS:${prop("nodeJsPluginVersion")}", "JavaScriptLanguage", "CSS", "JavaScriptDebugger")
  }
  dependencies {
    compile(project(":educational-core"))
    testCompile(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Rust") {
  intellij {
    setPlugins("org.rust.lang:${prop("rustPluginVersion")}")
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
  }

  dependencies {
    compile(project(":educational-core"))
    testCompile(project(":educational-core", "testOutput"))
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
    if (!inJetBrainsNetwork && org.gradle.internal.os.OperatingSystem.current().isWindows()) {
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
