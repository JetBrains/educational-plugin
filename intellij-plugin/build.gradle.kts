import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.*
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformTestingExtension
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import org.jetbrains.intellij.platform.gradle.utils.extensionProvider

plugins {
  id("intellij-plugin-common-conventions")
  id("org.jetbrains.intellij.platform")
}

repositories {
  intellijPlatform {
    defaultRepositories()
    jetbrainsRuntime()
  }
}

val buildNumber = System.getenv("BUILD_NUMBER") ?: "SNAPSHOT"

if (hasProp("setTCBuildNumber")) {
  // Specify build number at building plugin running configuration on TC
  // with heading plugin version: e.g. `3.8.BUILD_NUMBER` instead of `BUILD_NUMBER`
  println("##teamcity[buildNumber '$pluginVersion.$buildNumber']")
}

version = "$pluginVersion-$platformVersion-$buildNumber"

intellijPlatform {
  projectName = "JetBrainsAcademy"
  pluginConfiguration {
    id = "com.jetbrains.edu"
    name = "JetBrains Academy"
    version = "$pluginVersion-$platformVersion-$buildNumber"
    changeNotes = provider { file("changes.html").readText() }
    description = provider { file("description.html").readText() }

    ideaVersion {
      sinceBuild = prop("customSinceBuild")
      untilBuild = prop("customUntilBuild")
    }

    vendor {
      name = "JetBrains"
    }
  }
  caching {
    ides {
      enabled = true
    }
  }
  instrumentCode = false
  buildSearchableOptions = prop("enableBuildSearchableOptions").toBoolean()
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)

    pluginComposedModule(implementation(project("educational-core")))
    pluginComposedModule(implementation(project("jvm-core")))
    pluginComposedModule(implementation(project("AI")))
    pluginComposedModule(implementation(project("Edu-Java")))
    pluginComposedModule(implementation(project("Edu-Kotlin")))
    pluginComposedModule(implementation(project("Edu-Python")))
    pluginComposedModule(implementation(project("Edu-Scala")))
    pluginComposedModule(implementation(project("Edu-JavaScript")))
    pluginComposedModule(implementation(project("Edu-Rust")))
    pluginComposedModule(implementation(project("Edu-Cpp")))
    pluginComposedModule(implementation(project("Edu-Cpp:CLion-Classic")))
    pluginComposedModule(implementation(project("Edu-Cpp:CLion-Nova")))
    pluginComposedModule(implementation(project("Edu-Go")))
    pluginComposedModule(implementation(project("Edu-Php")))
    pluginComposedModule(implementation(project("Edu-Shell")))
    pluginComposedModule(implementation(project("Edu-CSharp")))
    pluginComposedModule(implementation(project("sql")))
    pluginComposedModule(implementation(project("sql:sql-jvm")))
    pluginComposedModule(implementation(project("features:code-insight-core")))
    pluginComposedModule(implementation(project("features:code-insight-html")))
    pluginComposedModule(implementation(project("features:code-insight-markdown")))
    pluginComposedModule(implementation(project("features:code-insight-yaml")))
    pluginComposedModule(implementation(project("features:command-line")))
    pluginComposedModule(implementation(project("features:github")))
    pluginComposedModule(implementation(project("features:ai-error-explanation")))
    pluginComposedModule(implementation(project("features:ai-hints-core")))
    pluginComposedModule(implementation(project("features:ai-hints-kotlin")))
    pluginComposedModule(implementation(project("features:ai-hints-python")))
    pluginComposedModule(implementation(project("features:ai-test-generation")))
    pluginComposedModule(implementation(project("features:ide-onboarding")))
    if (!isAtLeast252) { // BACKCOMPAT: Temporarily exclude for 2025.2 as it doesn't compile
      pluginComposedModule(implementation(project("features:remote-env")))
    }
    pluginComposedModule(implementation(project("features:social-media")))
    pluginComposedModule(implementation(project("localization")))

    testFramework(TestFrameworkType.Bundled)
  }
}

// Since 2024.2 Python Community plugin is available in paid products (like IU) together with Python Pro as its base dependency.
// Actually, Python Community contains all necessary code that we need.
// Python Pro plugin is added here on 2024.2 just to have the most common setup from user POV (i.e. Python Community + Python Pro)
val ideaPlugins = listOfNotNull(
  scalaPlugin,
  rustPlugin,
  pythonProPlugin,
  pythonCommunityPlugin,
  goPlugin,
  phpPlugin
)

val ideToPlugins = mapOf(
  // BACKCOMPTA: 2025.2. Drop `IntellijIdeaUltimate` usage
  IntellijIdeaUltimate to ideaPlugins,
  IntellijIdea to ideaPlugins,
  CLion to listOf(rustPlugin),
  AndroidStudio to listOf(pythonCommunityPlugin),
  GoLand to listOf(pythonCommunityPlugin),
  RustRover to listOf(pythonCommunityPlugin)
)

tasks {
  val projectName = project.extensionProvider.flatMap { it.projectName }

  composedJar {
    archiveBaseName.convention(projectName)
  }

  withType<PrepareSandboxTask> {
    from("socialMedia") {
      into("${projectName.get()}/socialMedia")
      include("**/*.gif")
    }
    doLast {
      val kotlinJarRe = """kotlin-(stdlib|reflect|runtime).*\.jar""".toRegex()
      val libraryDir = destinationDir.resolve("${projectName.get()}/lib")
      val kotlinStdlibJars = libraryDir.listFiles().orEmpty().filter { kotlinJarRe.matches(it.name) }
      check(kotlinStdlibJars.isEmpty()) {
        "Plugin shouldn't contain kotlin stdlib jars. Found:\n" + kotlinStdlibJars.joinToString(separator = ",\n") { it.absolutePath }
      }
    }
  }
  withType<RunIdeTask> {
    // Disable auto plugin reloading. See `com.intellij.ide.plugins.DynamicPluginVfsListener`
    // To enable dynamic reloading, change value to `true` and disable `EduDynamicPluginListener`
    autoReload = false
    jvmArgs("-Xmx2g")
    jvmArgs("-Dide.experimental.ui=true")
    jvmArgs("-Didea.kotlin.plugin.use.k2=true")

    // These system properties are used by educational-ml-library
    // System properties can't be passed directly since Gradle runs the IDE process separately
    // They are not inherited by default, unlike environment variables, which should work by default
    System.getProperties()
      .filterKeys { (it as? String)?.startsWith("educational.ml.") == true }
      .map { (key, value) -> jvmArgs("-D$key=$value") }

    // Uncomment to show localized messages
    // jvmArgs("-Didea.l10n=true")

    // Uncomment to enable memory dump creation if plugin cannot be unloaded by the platform
    // jvmArgs("-Dide.plugins.snapshot.on.unload.fail=true")

    // Uncomment to enable FUS testing mode
    // jvmArgs("-Dfus.internal.test.mode=true")
  }
  buildPlugin {
    dependsOn(":edu-format:jar")
    dependsOn(":edu-format:sourcesJar")
    dependsOn(*subprojects.mapNotNull { it.tasks.findByName(VERIFY_CLASSES_TASK_NAME) }.toTypedArray())
    doLast {
      copyFormatJars()
    }
  }

  intellijPlatformTesting {
    // Generates event scheme for JetBrains Academy plugin FUS events to `build/eventScheme.json`
    runIde.register("buildEventsScheme") {
      useInstaller = false

      task {
        args("buildEventsScheme", "--outputFile=${buildDir()}/eventScheme.json", "--pluginId=com.jetbrains.edu")
        // Force headless mode to be able to run command on CI
        systemProperty("java.awt.headless", "true")
        // BACKCOMPAT: 2025.1. Update value to 252 and this comment
        // `IDEA_BUILD_NUMBER` variable is used by `buildEventsScheme` task to write `buildNumber` to output json.
        // It will be used by TeamCity automation to set minimal IDE version for new events
        environment("IDEA_BUILD_NUMBER", "251")
      }
    }

    runIde.register("runInSplitMode") {
      useInstaller = false
      splitMode = true

      // Specify custom sandbox directory to have a stable path to log file
      sandboxDirectory = intellijPlatform.sandboxContainer.dir("split-mode-sandbox-$environmentName")

      plugins {
        val type = baseVersion.toTypeWithVersion().type
        plugins(idePlugins(type))
      }
    }

    customRunIdeTask(IntellijIdeaUltimate, ideaVersion, baseTaskName = "Idea")
    customRunIdeTask(CLion, clionVersion) {
      setClionSystemProperties(withRadler = false)
    }
    customRunIdeTask(CLion, clionVersion, baseTaskName = "CLion-Nova") {
      setClionSystemProperties(withRadler = true)
    }
    customRunIdeTask(PyCharmCommunity, pycharmVersion, baseTaskName = "PyCharm")
    customRunIdeTask(WebStorm)
    customRunIdeTask(GoLand)
    customRunIdeTask(PhpStorm)
    customRunIdeTask(RustRover)
    customRunIdeTask(DataSpell)
    customRunIdeTask(Rider, riderVersion)
  }
}

fun idePlugins(type: IntelliJPlatformType): List<String> {
  return ideToPlugins[type].orEmpty() + psiViewerPlugin
}

/**
 * Creates `run$[baseTaskName]` Gradle task to run IDE of given [type]
 * via `runIde` task with plugins according to [ideToPlugins] map
 */
fun IntelliJPlatformTestingExtension.customRunIdeTask(
  type: IntelliJPlatformType,
  versionWithCode: String? = null,
  baseTaskName: String = type.name,
  configureRunIdeTask: RunIdeTask.() -> Unit = {},
) {
  runIde.register("run$baseTaskName") {
    useInstaller = false

    if (versionWithCode != null) {
      val version = versionWithCode.toTypeWithVersion().version

      this.type = type
      this.version = version
    }
    else {
      val pathProperty = baseTaskName.replaceFirstChar { it.lowercaseChar() } + "Path"
      // Avoid throwing exception during property calculation.
      // Some IDE tooling (for example, Package Search plugin) may try to calculate properties during `Sync` phase for all tasks.
      // In our case, some `run*` task may not have `pathProperty` in your `gradle.properties`,
      // and as a result, the `Sync` tool window will show you the error thrown by `prop` function.
      //
      // The following solution just moves throwing the corresponding error to task execution,
      // i.e., only when a task is actually invoked
      if (hasProp(pathProperty)) {
        localPath.convention(layout.dir(provider { file(prop(pathProperty)) }))
      }
      else {
        task {
          doFirst {
            throw GradleException("Property `$pathProperty` is not defined in gradle.properties")
          }
        }
      }
    }

    // Specify custom sandbox directory to have a stable path to log file
    sandboxDirectory = intellijPlatform.sandboxContainer.dir("${baseTaskName.lowercase()}-sandbox-$environmentName")

    task(configureRunIdeTask)

    plugins {
      plugins(idePlugins(type))
    }
  }
}

fun buildDir(): String {
  return project.layout.buildDirectory.get().asFile.absolutePath
}

fun copyFormatJars() {
  copy {
    from("../edu-format/build/libs/")
    into("build/distributions")
    include("*.jar")
  }
}
