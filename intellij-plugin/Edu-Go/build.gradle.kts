plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(goPlugin)

    if (isAtLeast262) {
      intellijPlugins(testRunnerPlugin)
    }
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
