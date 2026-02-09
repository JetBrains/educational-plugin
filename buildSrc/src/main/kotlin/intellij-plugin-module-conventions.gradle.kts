import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
  id("intellij-plugin-common-conventions")
  id("org.jetbrains.intellij.platform.module")
}

repositories {
  intellijPlatform {
    defaultRepositories()
    jetbrainsRuntime()
  }
}

intellijPlatform {
  instrumentCode = false
}

tasks {
  prepareSandbox { enabled = false }
  composedJar {
    archiveVersion = ""
    // default name is ${project.rootProject.name}.${project.name}
    // but jar file name should be the same as module name defined in plugin.xml
    // Currently, we use the name of gradle subproject for plugin module
    archiveBaseName = project.name
  }
  test {
    failOnNoDiscoveredTests = false
    // Needed for both `:intellij-plugin:features:ai-hints-kotlin` and `:intellij-plugin:Edu-Kotlin`
    // Does nothing otherwise because Kotlin does not exist in the classpath
    jvmArgumentProviders += CommandLineArgumentProvider {
      listOf("-Didea.kotlin.plugin.use.k2=true")
    }

    // TODO: Drop Hyperskill-related code. See EDU-8582
    exclude("com/jetbrains/edu/learning/stepik/**")
    exclude("**/hyperskill/**")
  }
}

dependencies {
  val testOutput = configurations.create("testOutput")
  testOutput(sourceSets.test.get().output.classesDirs)

  intellijPlatform {
    testIntellijPlugins(commonTestPlugins)
    testFramework(intellijTestFrameworkType)
  }
}
