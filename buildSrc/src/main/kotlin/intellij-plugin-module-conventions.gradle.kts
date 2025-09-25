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
  test {
    failOnNoDiscoveredTests = false
    // Needed for both `:intellij-plugin:features:ai-hints-kotlin` and `:intellij-plugin:Edu-Kotlin`
    // Does nothing otherwise because Kotlin does not exist in the classpath
    jvmArgumentProviders += CommandLineArgumentProvider {
      listOf("-Didea.kotlin.plugin.use.k2=true")
    }
  }
}

dependencies {
  val testOutput = configurations.create("testOutput")
  testOutput(sourceSets.test.get().output.classesDirs)

  intellijPlatform {
    testIntellijPlugins(commonTestPlugins)
    testFramework(TestFrameworkType.Bundled)
  }
}
