import groovy.util.Node
import groovy.xml.XmlParser
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("common-conventions")
  id("net.saliman.properties")
}

java {
  sourceSets {
    main {
      java.srcDirs("branches/$environmentName/src")
      resources.srcDirs("branches/$environmentName/resources")
    }

    test {
      java.srcDirs("branches/$environmentName/testSrc")
      resources.srcDirs("branches/$environmentName/testResources")
    }
  }
}

kotlin {
  sourceSets {
    main {
      kotlin.srcDirs("branches/$environmentName/src")
    }
    test {
      kotlin.srcDirs("branches/$environmentName/testSrc")
    }
  }
}

// It's not possible to use version catalogs in convenrion plugin as usual,
// so we have to get the catalog itself and libraries manually
// See https://docs.gradle.org/current/userguide/version_catalogs.html#sec:buildsrc-version-catalog
val libs = the<VersionCatalogsExtension>().named("libs")

// TODO: move dependencies into particular module `build.gradle.kts`.
//  Most modules don't need all (or even any) these dependencies
dependencies {
  implementationWithoutKotlin(libs.findLibrary("twitter4j.core").get())
  implementationWithoutKotlin(libs.findLibrary("twitter4j.v2").get())
  implementationWithoutKotlin(libs.findLibrary("jsoup").get())
  implementationWithoutKotlin(libs.findLibrary("jackson.dataformat.yaml").get())
  implementationWithoutKotlin(libs.findLibrary("jackson.datatype.jsr310").get())
  implementationWithoutKotlin(libs.findLibrary("jackson.module.kotlin").get())
  implementationWithoutKotlin(libs.findLibrary("okhttp").get())
  implementationWithoutKotlin(libs.findLibrary("logging.interceptor").get())
  implementationWithoutKotlin(libs.findLibrary("retrofit").get())
  implementationWithoutKotlin(libs.findLibrary("converter.jackson").get())
  implementationWithoutKotlin(libs.findLibrary("kotlin.css.jvm").get())
  
  testImplementation(libs.findLibrary("junit").get())
  testImplementation(libs.findLibrary("openTest4J").get())
  testImplementation(libs.findLibrary("classgraph").get())
  testImplementationWithoutKotlin(libs.findLibrary("kotlin.test.junit").get())
  testImplementationWithoutKotlin(libs.findLibrary("mockwebserver").get())
  testImplementationWithoutKotlin(libs.findLibrary("mockk").get())
}

tasks {
  withType<JavaCompile> {
    // Prevents unexpected incremental compilation errors after changing value of `environmentName` property
    inputs.property("environmentName", providers.gradleProperty("environmentName"))
  }
  withType<KotlinCompile> {
    // Prevents unexpected incremental compilation errors after changing value of `environmentName` property
    inputs.property("environmentName", providers.gradleProperty("environmentName"))
  }

  register(VERIFY_CLASSES_TASK_NAME) {
    dependsOn(jar)
    // `verifyClasses` relies on resources from the current and `intellij-plugin` modules.
    // So, we need to be sure that all necessary recourses are already located in expected placed
    dependsOn(project(":intellij-plugin").tasks.jar)
    doLast {
      verifyClasses(project)
    }
  }
}

private fun parseManifest(file: File): Node {
  val node = XmlParser().parse(file)
  check(node.name() == "idea-plugin") {
    "Manifest file `$file` doesn't contain top-level `idea-plugin` attribute"
  }
  return node
}

private fun manifestFile(project: Project): File? {
  var filePath: String? = null
  // Some gradle projects are not modules from IDEA plugin point of view
  // because we use `include` for them inside manifests, i.e. they just a part of another module.
  // That's why we delegate manifest search to other projects in some cases
  when (project.path) {
    ":intellij-plugin" -> {
      filePath = "META-INF/plugin.xml"
    }
    ":intellij-plugin:educational-core",
    ":intellij-plugin:Edu-Python:Idea", ":intellij-plugin:Edu-Python:PyCharm" -> return manifestFile(project.parent!!)
    // Localization module is not supposed to have a plugin manifest.
    // Since it also is not supposed to have any code, only resources, no need to verify anything for it
    ":intellij-plugin:localization" -> return null
  }

  val mainOutput = project.sourceSets.main.get().output
  val resourcesDir = mainOutput.resourcesDir ?: error("Failed to find resources dir for ${project.name}")

  if (filePath != null) {
    return resourcesDir.resolve(filePath).takeIf { it.exists() } ?: error("Failed to find manifest file for ${project.name} module")
  }
  val rootManifestFile = manifestFile(project(":intellij-plugin")) ?: error("Failed to find manifest file for :intellij-plugin module")
  val rootManifest = parseManifest(rootManifestFile)
  val children = ((rootManifest["content"] as? List<*>)?.single() as? Node)?.children()
                 ?: error("Failed to find module declarations in root manifest")
  return children.filterIsInstance<Node>()
           .flatMap { node ->
             if (node.name() != "module") return@flatMap emptyList()
             val name = node.attribute("name") as? String ?: return@flatMap emptyList()
             listOfNotNull(resourcesDir.resolve("$name.xml").takeIf { it.exists() })
           }.firstOrNull() ?: error("Failed to find manifest file for ${project.name} module")
}

private fun findModulePackage(project: Project): String? {
  val moduleManifest = manifestFile(project) ?: return null
  val node = parseManifest(moduleManifest)
  return node.attribute("package") as? String ?: error("Failed to find package for ${project.name}")
}

private fun verifyClasses(project: Project) {
  val pkg = findModulePackage(project) ?: return
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
