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
    // Needed for both `:intellij-plugin:features:ai-hints-kotlin` and `:intellij-plugin:Edu-Kotlin`
    // Does nothing otherwise because Kotlin does not exist in the classpath
    jvmArgumentProviders += CommandLineArgumentProvider {
      listOf(
        if (isAtLeast251) {
          "-Didea.kotlin.plugin.use.k2=true"
        }
        else {
          "-Didea.kotlin.plugin.use.k2=false"
        }
      )
    }
  }
}

dependencies {
  val testOutput = configurations.create("testOutput")
  testOutput(sourceSets.test.get().output.classesDirs)

  intellijPlatform {
    // FIXME: Drop when new version (`clionVersion`) of EAP supported
    // Reason: Neither `com.jetbrains.modules.json`, nor `com.jetbrains.json` is resolved
    if (isAtLeast252 && project.path.contains("Edu-Cpp")) {
      testIntellijPlugins(imagesPlugin, yamlPlugin)
    } else {
      testIntellijPlugins(commonTestPlugins)
    }
    testFramework(TestFrameworkType.Bundled)
  }
}
