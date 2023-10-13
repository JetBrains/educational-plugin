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
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
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

// Probably, these versions should be extracted to version catalog
// See https://docs.gradle.org/current/userguide/platforms.html#sub:conventional-dependencies-toml
val jacksonVersion = "2.14.3"
val okhttpVersion = "4.10.0"
val retrofitVersion = "2.9.0"

val ideaSandbox = "${project.buildDir.absolutePath}/idea-sandbox"
val pycharmSandbox = "${project.buildDir.absolutePath}/pycharm-sandbox"
val studioSandbox = "${project.buildDir.absolutePath}/studio-sandbox"
val webStormSandbox = "${project.buildDir.absolutePath}/webstorm-sandbox"
val clionSandbox = "${project.buildDir.absolutePath}/clion-sandbox"
val goLandSandbox = "${project.buildDir.absolutePath}/goland-sandbox"
val phpStormSandbox = "${project.buildDir.absolutePath}/phpstorm-sandbox"

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
  kotlin("jvm") version "1.9.0"
  id("org.jetbrains.intellij") version "1.16.0"
  id("de.undercouch.download") version "5.3.0"
  id("net.saliman.properties") version "1.5.2"
  id("org.gradle.test-retry") version "1.5.1"
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

    withType<JavaCompile> { options.encoding = "UTF-8" }
    withType<KotlinCompile> {
      compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        languageVersion = KotlinVersion.DEFAULT
        // see https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
        apiVersion = KotlinVersion.KOTLIN_1_8
        freeCompilerArgs = listOf("-Xjvm-default=all")
      }
    }

    jar {
      // Starting from gradle-intellij-plugin 1.6.0, test runs produces `classpath.index` file in `class` directory
      // But this file shouldn't be included into final module artifact at all, so exclude it manually for now
      exclude("**/classpath.index")
    }

    val verifyClasses = task("verifyClasses") {
      dependsOn(jar)
      doLast {
        verifyClasses(project)
      }
    }
    // Fail plugin build if there are errors in module packages
    rootProject.tasks.buildPlugin {
      dependsOn(verifyClasses)
      doLast {
        copyFormatJar()
      }
    }
  }
}

fun Iterable<Project>.pluginModules(): List<Project> {
  return filter { it.name != "edu-format" }
}

configure(allprojects.pluginModules()) {
  apply {
    plugin("org.jetbrains.intellij")
  }
  intellij {
    version = baseVersion
    instrumentCode = false
  }
  dependencies {
    implementationWithoutKotlin(group = "org.twitter4j", name = "twitter4j-core", version = "4.0.1")
    implementationWithoutKotlin("org.jsoup:jsoup:1.15.3")
    implementationWithoutKotlin(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = jacksonVersion)
    implementationWithoutKotlin(group = "com.fasterxml.jackson.datatype", name = "jackson-datatype-jsr310", version = jacksonVersion)
    implementationWithoutKotlin(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jacksonVersion)
    implementationWithoutKotlin("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementationWithoutKotlin("com.squareup.retrofit2:converter-jackson:$retrofitVersion")
    implementationWithoutKotlin("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementationWithoutKotlin("org.jetbrains:kotlin-css-jvm:1.0.0-pre.58-kotlin-1.3.0")

    // The same as `testImplementation(kotlin("test"))` but with excluding kotlin stdlib dependencies
    testImplementationWithoutKotlin("org.jetbrains.kotlin:kotlin-test-junit:${getKotlinPluginVersion()}")
    testImplementationWithoutKotlin("com.squareup.okhttp3:mockwebserver:$okhttpVersion")
    testImplementationWithoutKotlin("io.mockk:mockk:1.12.0")
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
  }

  // Generates event scheme for JetBrains Academy plugin FUS events to `build/eventScheme.json`
  task<RunIdeTask>("buildEventsScheme") {
    dependsOn(tasks.prepareSandbox)
    args("buildEventsScheme", "--outputFile=${buildDir.resolve("eventScheme.json").absolutePath}", "--pluginId=com.jetbrains.edu")
    // Force headless mode to be able to run command on CI
    systemProperty("java.awt.headless", "true")
    // BACKCOMPAT: 2023.1. Update value to 232 and this comment
    // `IDEA_BUILD_NUMBER` variable is used by `buildEventsScheme` task to write `buildNumber` to output json.
    // It will be used by TeamCity automation to set minimal IDE version for new events
    environment("IDEA_BUILD_NUMBER", "231")
  }

  task<RunIdeTask>("validateCourse") {
    dependsOn(tasks.prepareSandbox)
    args("validateCourse", "/Users/Arseniy.Pendryak/IdeaProjects/PlainTextValidationCourse13", "--marketplace", "22836")
    // Force headless mode to be able to run command on CI
    systemProperty("java.awt.headless", "true")
//    systemProperty("idea.disposer.debug", "off")
    systemProperty("project.jdk", "/Users/Arseniy.Pendryak/Library/Java/JavaVirtualMachines/corretto-17.0.7/Contents/Home")
    systemProperty("project.jdk.name", "TestJdk")
    systemProperty("project.python.interpreter", "/opt/homebrew/bin/python3")
    systemProperty("project.rust.toolchain", "/Users/Arseniy.Pendryak/.cargo/bin")
    systemProperty("project.go.sdk", "/Users/Arseniy.Pendryak/sdk/go1.20.4")
    systemProperty("project.nodejs.interpreter", "/opt/homebrew/bin/node")
  }

  task<RunIdeTask>("createCourse") {
    dependsOn(tasks.prepareSandbox)
    args("createCourse", "/Users/Arseniy.Pendryak/IdeaProjects/CmdPythonCourse10001", "--marketplace", "https://plugins.jetbrains.com/plugin/16630-introduction-to-python")
    // Force headless mode to be able to run command on CI
    systemProperty("java.awt.headless", "true")
//    systemProperty("idea.disposer.debug", "off")
    systemProperty("project.jdk", "/Users/Arseniy.Pendryak/Library/Java/JavaVirtualMachines/corretto-17.0.7/Contents/Home")
    systemProperty("project.jdk.name", "TestJdk")
    systemProperty("project.python.interpreter", "/opt/homebrew/bin/python3")
    systemProperty("project.rust.toolchain", "/Users/Arseniy.Pendryak/.cargo/bin")
    systemProperty("project.go.sdk", "/Users/Arseniy.Pendryak/sdk/go1.20.4")
    systemProperty("project.nodejs.interpreter", "/opt/homebrew/bin/node")
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
}

project(":edu-format") {
  dependencies {
    compileOnly(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8")
    compileOnly(group = "org.jetbrains", name = "annotations", version = "23.0.0")
    implementationWithoutKotlin(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jacksonVersion)
    implementationWithoutKotlin(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = jacksonVersion)
    implementationWithoutKotlin(group = "com.fasterxml.jackson.datatype", name = "jackson-datatype-jsr310", version = jacksonVersion)
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

  // BACKCOMPAT: enable when 232 studio is available
  tasks.withType<Test> {
    enabled = environmentName.toInt() < 232
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

fun DependencyHandler.implementationWithoutKotlin(dependencyNotation: String): ExternalModuleDependency {
  return implementation(dependencyNotation) {
    excludeKotlinDeps()
  }
}

fun DependencyHandler.implementationWithoutKotlin(group: String, name: String, version: String? = null): ExternalModuleDependency {
  return implementation(group, name, version) {
    excludeKotlinDeps()
  }
}

fun DependencyHandler.testImplementationWithoutKotlin(dependencyNotation: String): ExternalModuleDependency {
  return testImplementation(dependencyNotation) {
    excludeKotlinDeps()
  }
}

fun copyFormatJar() {
  copy {
    from("edu-format/build/libs/")
    into("build/distributions")
    include("edu-format.jar")
  }
}