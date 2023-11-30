import groovy.util.Node
import groovy.xml.XmlParser
import org.gradle.api.JavaVersion.VERSION_17
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.RunIdeBase
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

// For some reason, Gradle doesn't find `zstd` lib to unpack fleet archive downloaded by fleet gradle plugin.
// Let's add it manually for now
buildscript {
  dependencies {
    classpath("com.github.luben:zstd-jni:1.5.5-10")
  }
}

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

val ideaSandbox = "${buildDir()}/idea-sandbox"
val pycharmSandbox = "${buildDir()}/pycharm-sandbox"
val studioSandbox = "${buildDir()}/studio-sandbox"
val webStormSandbox = "${buildDir()}/webstorm-sandbox"
val clionSandbox = "${buildDir()}/clion-sandbox"
val goLandSandbox = "${buildDir()}/goland-sandbox"
val phpStormSandbox = "${buildDir()}/phpstorm-sandbox"
val remoteDevServerSandbox = "${buildDir()}/remote-dev-server-sandbox"

// BACKCOMPAT: 2023.1
val isAtLeast232 = environmentName.toInt() >= 232

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

val isTeamCity: Boolean get() = System.getenv("TEAMCITY_VERSION") != null

plugins {
  idea
  alias(libs.plugins.kotlinPlugin)
  alias(libs.plugins.gradleIntelliJPlugin)
  alias(libs.plugins.downloadPlugin)
  alias(libs.plugins.propertiesPlugin)
  alias(libs.plugins.testRetryPlugin)
  `maven-publish`
}

idea {
  project {
    jdkName = "17"
    languageLevel = IdeaLanguageLevel("11")
    vcs = "Git"
  }
  module {
    excludeDirs.add(file("dependencies"))
  }
}

allprojects {
  apply {
    plugin("java")
    plugin("kotlin")
    plugin("net.saliman.properties")
    plugin("org.gradle.test-retry")
  }

  repositories {
    mavenCentral()
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
  }

  configure<JavaPluginExtension> {
    sourceCompatibility = VERSION_17
    targetCompatibility = VERSION_17
  }
  sourceSets {
    main {
      if (project != rootProject) {
        java.srcDirs("src", "branches/$environmentName/src")
      }
      resources.srcDirs("resources", "branches/$environmentName/resources")
    }

    test {
      if (project != rootProject) {
        java.srcDirs("testSrc", "branches/$environmentName/testSrc")
        resources.srcDirs("testResources", "branches/$environmentName/testResources")
      }
    }
  }

  kotlin {
    sourceSets {
      if (project != rootProject) {
        main {
          kotlin.srcDirs("src", "branches/$environmentName/src")
        }
        test {
          kotlin.srcDirs("testSrc", "branches/$environmentName/testSrc")
        }
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
      withProp(secretProperties, "stepikTestClientSecret") { environment("STEPIK_TEST_CLIENT_SECRET", it) }
      withProp(secretProperties, "stepikTestClientId") { environment("STEPIK_TEST_CLIENT_ID", it) }
      withProp("excludeTests") { exclude(it) }

      systemProperty("java.awt.headless", "true")

      ignoreFailures = true
      filter {
        isFailOnNoMatchingTests = false
      }
      if (isTeamCity) {
        retry {
          maxRetries = 3
          maxFailures = 5
        }
      }
    }

    withType<JavaCompile> {
      options.encoding = "UTF-8"
      // Prevents unexpected incremental compilation errors after changing value of `environmentName` property
      inputs.property("environmentName", providers.gradleProperty("environmentName"))
    }
    withType<KotlinCompile> {
      compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        languageVersion = KotlinVersion.DEFAULT
        // see https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
        apiVersion = KotlinVersion.KOTLIN_1_8
        freeCompilerArgs = listOf("-Xjvm-default=all")
      }
      // Prevents unexpected incremental compilation errors after changing value of `environmentName` property
      inputs.property("environmentName", providers.gradleProperty("environmentName"))
    }

    jar {
      // Starting from gradle-intellij-plugin 1.6.0, test runs produces `classpath.index` file in `class` directory
      // But this file shouldn't be included into final module artifact at all, so exclude it manually for now
      exclude("**/classpath.index")
    }
  }
}

fun Iterable<Project>.pluginModules(): List<Project> {
  return filter { it.name != "edu-format" && it.name != "fleet-plugin" }
}

configure(allprojects.pluginModules()) {
  apply {
    plugin("org.jetbrains.intellij")
  }
  intellij {
    version = baseVersion
    instrumentCode = false
  }

  tasks {
    val verifyClasses = task("verifyClasses") {
      dependsOn(jar)
      doLast {
        verifyClasses(project)
      }
    }
    // Fail plugin build if there are errors in module packages
    rootProject.tasks.buildPlugin {
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
    implementationWithoutKotlin(rootProject.libs.retrofit)
    implementationWithoutKotlin(rootProject.libs.converter.jackson)
    implementationWithoutKotlin(rootProject.libs.logging.interceptor)
    implementationWithoutKotlin(rootProject.libs.kotlin.css.jvm)

    testImplementationWithoutKotlin(rootProject.libs.kotlin.test.junit)
    testImplementationWithoutKotlin(rootProject.libs.mockwebserver)
    testImplementationWithoutKotlin(rootProject.libs.mockk)
  }
}

configure(subprojects.pluginModules()) {
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
    implementation(project(":Edu-Php"))
    implementation(project(":Edu-Shell"))
    implementation(project(":sql"))
    implementation(project(":sql:sql-jvm"))
    implementation(project(":github"))
    implementation(project(":remote-env"))
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
      from("$rootDir/twitter") {
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
    // BACKCOMPAT: 2023.1. Update value to 232 and this comment
    // `IDEA_BUILD_NUMBER` variable is used by `buildEventsScheme` task to write `buildNumber` to output json.
    // It will be used by TeamCity automation to set minimal IDE version for new events
    environment("IDEA_BUILD_NUMBER", "231")
  }

  task<RunIdeTask>("runRemoteDevServer") {
    dependsOn(tasks.prepareSandbox)
    val remoteProjectPath = System.getenv("REMOTE_DEV_PROJECT") ?: project.layout.projectDirectory.dir("example-course-project").asFile.absolutePath
    args("cwmHostNoLobby", remoteProjectPath)
    systemProperty("ide.browser.jcef.enabled", "false")
  }

  task("configureIdea") {
    doLast {
      intellij.sandboxDir = ideaSandbox
      withProp("ideaPath") { path ->
        tasks.runIde {
          ideDir = file(path)
        }
      }
    }
  }

  task("configurePyCharm") {
    doLast {
      intellij.sandboxDir = pycharmSandbox
      withProp("pycharmPath") { path ->
        tasks.runIde {
          ideDir = file(path)
        }
      }
    }
  }

  task("configureWebStorm") {
    doLast {
      if (!hasProp("webStormPath")) {
        throw InvalidUserDataException("Path to WebStorm installed locally is needed\nDefine \"webStormPath\" property")
      }

      intellij.sandboxDir = webStormSandbox
      tasks.runIde {
        ideDir = file(prop("webStormPath"))
      }
    }
  }

  task("configureCLion") {
    doLast {
      intellij.sandboxDir = clionSandbox
      withProp("clionPath") { path ->
        tasks.runIde {
          ideDir = file(path)
        }
      }
    }
  }

  task("configureAndroidStudio") {
    doLast {
      intellij.sandboxDir = studioSandbox
      withProp("androidStudioPath") { path ->
        tasks.runIde {
          ideDir = file(path)
        }
      }
    }
  }

  task("configureGoLand") {
    doLast {
      if (!hasProp("goLandPath")) {
        throw InvalidUserDataException("Path to GoLand installed locally is needed\nDefine \"goLandPath\" property")
      }

      intellij.sandboxDir = goLandSandbox
      tasks.runIde {
        ideDir = file(prop("goLandPath"))
      }
    }
  }

  task("configurePhpStorm") {
    doLast {
      if (!hasProp("phpStormPath")) {
        throw InvalidUserDataException("Path to PhpStorm installed locally is needed\nDefine \"phpStormPath\" property")
      }

      intellij.sandboxDir = phpStormSandbox
      tasks.runIde {
        ideDir = file(prop("phpStormPath"))
      }
    }
  }

  task("configureRemoteDevServer") {
    doLast {
      intellij.sandboxDir = remoteDevServerSandbox
    }
  }
}

project(":edu-format") {
  java {
    withSourcesJar()
  }
  dependencies {
    compileOnly(rootProject.libs.kotlin.stdlib)
    compileOnly(rootProject.libs.annotations)
    implementationWithoutKotlin(rootProject.libs.jackson.module.kotlin)
    implementationWithoutKotlin(rootProject.libs.jackson.dataformat.yaml)
    implementationWithoutKotlin(rootProject.libs.jackson.datatype.jsr310)
    implementationWithoutKotlin(rootProject.libs.retrofit)
    implementationWithoutKotlin(rootProject.libs.converter.jackson)
    implementationWithoutKotlin(rootProject.libs.logging.interceptor)
  }

  // Workaround to help java to find `module-info.java` file.
  // Is there a better way?
  val moduleName = "com.jetbrains.edu.format"
  tasks {
    compileJava {
      inputs.property("moduleName", moduleName)
      options.compilerArgs = listOf("--patch-module", "$moduleName=${sourceSets.main.get().output.asPath}")
    }
  }
}

project(":educational-core") {
  dependencies {
    api(project(":edu-format"))
  }
}

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
    implementation(project(":educational-core"))
    implementation(project(":code-insight"))

    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":code-insight", "testOutput"))
  }
}

project(":code-insight:yaml") {
  intellij {
    plugins = listOf(yamlPlugin)
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
      version = ideaVersion
    }
    plugins = jvmPlugins
  }

  dependencies {
    implementation(project(":educational-core"))

    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":remote-env") {
  intellij {
    if (isStudioIDE) {
      version = ideaVersion
    }
    plugins = listOf(codeWithMePlugin)
  }

  dependencies {
    implementation(project(":educational-core"))
  }
}

project(":Edu-Java") {
  intellij {
    version = ideaVersion
    plugins = jvmPlugins
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
      version = ideaVersion
    }
    plugins = kotlinPlugins
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
    version = ideaVersion
    val pluginsList = jvmPlugins + scalaPlugin
    plugins = pluginsList
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
    version = studioVersion
    val pluginsList = jvmPlugins + androidPlugin
    plugins = pluginsList
  }

  dependencies {
    implementation(project(":educational-core"))
    implementation(project(":jvm-core"))

    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":jvm-core", "testOutput"))
  }

  // BACKCOMPAT: enable when 233 studio is available
  tasks.withType<Test> {
    enabled = environmentName.toInt() < 233
  }
}

project(":Edu-Python") {
  intellij {
    val pluginList = pythonPlugins + listOfNotNull(
      if (isJvmCenteredIDE) javaPlugin else null,
      // needed only for tests, actually
      platformImagesPlugin
    )
    plugins = pluginList
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
    implementation(project(":educational-core"))
    compileOnly(project(":Edu-Python"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Python:PyCharm") {
  intellij {
    if (isStudioIDE) {
      version = ideaVersion
    }
    plugins = pythonPlugins
  }

  dependencies {
    implementation(project(":educational-core"))
    compileOnly(project(":Edu-Python"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-JavaScript") {
  intellij {
    version = ideaVersion
    plugins = javaScriptPlugins
  }
  dependencies {
    implementation(project(":educational-core"))

    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Rust") {
  intellij {
    if (isAtLeast232 && !isIdeaIDE && !isClionIDE) {
      version = ideaVersion
    }
    plugins = rustPlugins
  }

  dependencies {
    implementation(project(":educational-core"))

    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Cpp") {
  intellij {
    version = clionVersion
    plugins = cppPlugins
  }

  dependencies {
    implementation(project(":educational-core"))

    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Go") {
  intellij {
    version = ideaVersion
    plugins = listOf(goPlugin, intelliLangPlugin)
  }

  dependencies {
    implementation(project(":educational-core"))

    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Php") {
  intellij {
    version = ideaVersion
    plugins = listOf(phpPlugin)
  }

  dependencies {
    implementation(project(":educational-core"))

    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":Edu-Shell") {
  intellij {
    plugins = listOf(shellScriptPlugin)
  }

  dependencies {
    implementation(project(":educational-core"))

    testImplementation(project(":educational-core", "testOutput"))
  }
}

project(":sql") {
  intellij {
    if (isStudioIDE || isPycharmIDE) {
      version = ideaVersion
    }
    plugins = listOf(sqlPlugin)
  }

  dependencies {
    api(project(":educational-core"))
    testImplementation(project(":educational-core", "testOutput"))
  }
}

project("sql:sql-jvm") {
  intellij {
    version = ideaVersion
    plugins = listOf(sqlPlugin) + jvmPlugins
  }

  dependencies {
    api(project(":sql"))
    api(project(":jvm-core"))
    testImplementation(project(":educational-core", "testOutput"))
    testImplementation(project(":sql", "testOutput"))
    testImplementation(project(":jvm-core", "testOutput"))
  }
}

project(":github") {
  intellij {
    plugins = listOf(githubPlugin)
  }

  dependencies {
    implementation(project(":educational-core"))

    testImplementation(project(":educational-core", "testOutput"))
  }
}

// For some reason, `version = "$pluginVersion.0"` inside `fleet-plugin/build.gradle.kts` is not enough.
// It seems fleet gradle plugin reads project version too early when it's not set yet.
// This code executed before `fleet-plugin/build.gradle.kts` is evaluated,
// so at the moment of reading version is already set.
//
// `.0` is needed because fleet plugin should have only `major.minor.patch` version structure
if (prop("fleetIntegration").toBoolean()) {
  project(":fleet-plugin") {
    version = "$pluginVersion.0"
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
    // TODO: make it more precise
    it.extension == "xml" && it.readText().trimStart().startsWith("<idea-plugin")
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
      listOfNotNull(resourcesDir.resolve("$name.xml").takeIf { it.exists() })
    }.firstOrNull() ?: error("Failed to find manifest file for ${project.name} module")
}

fun findModulePackage(project: Project): String {
  // Some gradle projects are not modules from IDEA plugin point of view
  // because we use `include` for them inside manifests, i.e. they just a part of another module.
  // That's why we delegate manifest search to other projects in some cases
  val moduleManifest = when (project.path) {
    ":", ":educational-core", ":edu-format", ":code-insight" -> manifestFile(rootProject, "META-INF/plugin.xml")
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

publishing {
  publications {
    create<MavenPublication>("edu-format") {
      groupId = "com.jetbrains.edu"
      artifactId = "edu-format"
      version = prop("publishingVersion")

      artifact(prop("eduFormatArtifactPath"))
      artifact(prop("eduFormatSourcesArtifactPath")) {
        classifier = "sources"
      }
    }
  }
  repositories {
    maven {
      url = uri("https://packages.jetbrains.team/maven/p/edu/maven")
      credentials {
        username = prop("publishingUsername")
        password = prop("publishingPassword")
      }
    }
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
    from("edu-format/build/libs/")
    into("build/distributions")
    include("*.jar")
  }
}