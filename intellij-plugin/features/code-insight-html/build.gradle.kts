import org.gradle.kotlin.dsl.withType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":intellij-plugin:features:code-insight-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(project(":intellij-plugin:features:code-insight-core", "testOutput"))
}

tasks {
  withType<PrepareSandboxTask> {
    // It seems the JavaScript plugin adds references that have conflicts with in-course references.
    // And it leads to failed tests.
    // Ideally, it should work together as well, but let's simply disable the JavaScript plugin here for now
    disabledPlugins.add(javaScriptPlugin)
  }
}