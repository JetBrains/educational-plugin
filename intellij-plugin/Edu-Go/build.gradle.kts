plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(goPlugin)

    intellijPlugins(testRunnerPlugin)
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
