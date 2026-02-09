import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(jvmPlugins)

    if (isAtLeast261) {
      testFramework(TestFrameworkType.Plugin.Java)
    }
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":intellij-plugin:jvm-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
}
