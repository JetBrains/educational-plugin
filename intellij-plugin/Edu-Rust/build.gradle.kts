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
      // BACKCOMPAT 2026.1: Drop not-null assertion (!!)
      testIntellijPlugins(nativeDebugPlugin!!)
    }
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
