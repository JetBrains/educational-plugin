plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    val ideVersion = if (!isIdeaIDE && !isClionIDE) ideaVersion else baseVersion
    intellijIde(ideVersion)

    intellijPlugins(rustPlugins)

    if (isAtLeast262) {
      intellijPlugins(testRunnerPlugin)
    }
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
