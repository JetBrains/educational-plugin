plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    // TODO: use `baseVersion` when https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1790 is resolved
    intellijIde(ideaVersion)
    if (isAtLeast262) {
      intellijPlugins(testRunnerPlugin)
    }
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":validation-results"))
  implementation(libs.clikt.core)

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(libs.kotlinx.coroutine.test)
}
